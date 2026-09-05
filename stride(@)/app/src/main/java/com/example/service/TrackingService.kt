package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.StrideApp
import com.example.data.model.RoutePoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TrackingState(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val isMoving: Boolean = false,
    val activityType: String = "RUN",
    val elapsedSeconds: Long = 0L,
    val distanceMeters: Double = 0.0,
    val currentPaceSecPerKm: Double = 0.0,
    val avgPaceSecPerKm: Double = 0.0,
    val calories: Int = 0,
    val elevationGainMeters: Double = 0.0,
    val currentInclinePercent: Double = 0.0,
    val steps: Int = 0,
    val routePoints: List<RoutePoint> = emptyList(),
    val currentLocation: RoutePoint? = null
)

class TrackingService : Service(), SensorEventListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var timerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Hardware Step & Motion Sensors
    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var initialStepCount = -1
    private var sessionSteps = 0

    // Accelerometer physical movement detection
    private var lastAccelMagnitude: Float = 9.81f
    private var accelVarianceSum: Float = 0f
    private var accelSampleCount: Int = 0
    private var isPhysicalMotionDetected: Boolean = false
    private var lastMotionTimestamp: Long = 0L

    // Active movement tracking
    private var anchorLocation: Location? = null
    private var activeMovingSeconds: Long = 0L

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        // Prefer STEP_COUNTER (hardware cumulative), fallback to STEP_DETECTOR
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Stride::TrackingWakeLock"
        )?.apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val type = intent.getStringExtra(EXTRA_ACTIVITY_TYPE) ?: "RUN"
                startTracking(type)
            }
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startTracking(type: String) {
        if (_trackingState.value.isTracking && !_trackingState.value.isPaused) return

        initialStepCount = -1
        sessionSteps = 0
        anchorLocation = null
        activeMovingSeconds = 0L
        isPhysicalMotionDetected = false
        lastMotionTimestamp = 0L

        _trackingState.value = TrackingState(
            isTracking = true,
            isPaused = false,
            isMoving = false,
            activityType = type,
            elapsedSeconds = 0L,
            distanceMeters = 0.0,
            currentInclinePercent = 0.0,
            steps = 0,
            routePoints = emptyList()
        )

        try {
            wakeLock?.acquire(8 * 60 * 60 * 1000L) // 8 hour safe maximum
        } catch (e: Exception) {
            Log.e("TrackingService", "Error acquiring WakeLock", e)
        }

        registerSensors()
        startForeground(NOTIFICATION_ID, buildNotification("Recording $type..."))
        startTimer()
        startLocationUpdates()
    }

    private fun pauseTracking() {
        _trackingState.value = _trackingState.value.copy(isPaused = true, isMoving = false)
        timerJob?.cancel()
        removeLocationUpdates()
        unregisterSensors()
        updateNotification("Paused - ${formatDistance(_trackingState.value.distanceMeters)}")
    }

    @SuppressLint("MissingPermission")
    private fun resumeTracking() {
        _trackingState.value = _trackingState.value.copy(isPaused = false)
        registerSensors()
        startTimer()
        startLocationUpdates()
        updateNotification("Recording ${_trackingState.value.activityType}...")
    }

    private fun stopTracking() {
        timerJob?.cancel()
        removeLocationUpdates()
        unregisterSensors()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e("TrackingService", "Error releasing WakeLock", e)
        }
        _trackingState.value = _trackingState.value.copy(isTracking = false, isPaused = false, isMoving = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun registerSensors() {
        stepSensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        accelerometerSensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun unregisterSensors() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !_trackingState.value.isTracking || _trackingState.value.isPaused) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalSensorSteps = event.values[0].toInt()
                if (initialStepCount < 0) {
                    initialStepCount = totalSensorSteps
                }
                val calculatedSteps = (totalSensorSteps - initialStepCount).coerceAtLeast(0)
                if (calculatedSteps > sessionSteps) {
                    sessionSteps = calculatedSteps
                    _trackingState.value = _trackingState.value.copy(steps = sessionSteps)
                    lastMotionTimestamp = System.currentTimeMillis()
                    isPhysicalMotionDetected = true
                }
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                if (event.values[0] == 1.0f) {
                    sessionSteps += 1
                    _trackingState.value = _trackingState.value.copy(steps = sessionSteps)
                    lastMotionTimestamp = System.currentTimeMillis()
                    isPhysicalMotionDetected = true
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val mag = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val delta = kotlin.math.abs(mag - lastAccelMagnitude)
                lastAccelMagnitude = mag

                accelVarianceSum += delta
                accelSampleCount++

                // Every 10 samples (~0.5 second)
                if (accelSampleCount >= 10) {
                    val avgDelta = accelVarianceSum / accelSampleCount
                    // If avg delta is > 0.35 m/s², the phone is experiencing physical athletic motion
                    // If avg delta < 0.15 m/s², the phone is sitting on a desk or at rest
                    if (avgDelta > 0.35f) {
                        lastMotionTimestamp = System.currentTimeMillis()
                        isPhysicalMotionDetected = true
                    } else if (System.currentTimeMillis() - lastMotionTimestamp > 3000L) {
                        isPhysicalMotionDetected = false
                    }
                    accelVarianceSum = 0f
                    accelSampleCount = 0
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && _trackingState.value.isTracking && !_trackingState.value.isPaused) {
                delay(1000L)
                val newSeconds = _trackingState.value.elapsedSeconds + 1
                val isMoving = _trackingState.value.isMoving
                if (isMoving) {
                    activeMovingSeconds++
                }

                val distance = _trackingState.value.distanceMeters

                // Avg Pace: ONLY show when real distance has been covered (>= 25 meters)
                // When phone is at rest or has not moved, avg pace remains 0.0 (displays "--:--")
                val avgPace = if (distance >= 25.0 && activeMovingSeconds > 0) {
                    (activeMovingSeconds / (distance / 1000.0))
                } else {
                    0.0
                }

                // Calories: ONLY burn when distance >= 15 meters and athlete is actively moving
                // When phone is at rest, calories remains strictly 0 kcal!
                val cal = if (distance >= 15.0 && activeMovingSeconds > 0) {
                    calculateCalories(_trackingState.value.activityType, distance, activeMovingSeconds)
                } else {
                    0
                }

                _trackingState.value = _trackingState.value.copy(
                    elapsedSeconds = newSeconds,
                    avgPaceSecPerKm = avgPace,
                    calories = cal
                )

                if (newSeconds % 3 == 0L) {
                    updateNotification(
                        "${formatTime(newSeconds)} • ${formatDistance(distance)} • ${formatPace(avgPace)}"
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(2f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    processNewLocation(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback as LocationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("TrackingService", "Permission missing for location updates", e)
        }
    }

    private fun removeLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    private fun processNewLocation(location: Location) {
        // Discard low-accuracy GPS fixes (> 25m uncertainty)
        if (location.hasAccuracy() && location.accuracy > 25.0f) {
            return
        }

        val current = _trackingState.value
        val newPoint = RoutePoint(location.latitude, location.longitude, location.altitude, location.time)

        // Initialize anchor on first fix
        if (current.routePoints.isEmpty() || anchorLocation == null) {
            anchorLocation = location
            _trackingState.value = current.copy(
                routePoints = listOf(newPoint),
                currentLocation = newPoint
            )
            return
        }

        val anchor = anchorLocation ?: location
        val distArray = FloatArray(1)
        Location.distanceBetween(
            anchor.latitude, anchor.longitude,
            location.latitude, location.longitude,
            distArray
        )
        val incrementalDist = distArray[0].toDouble()

        // Speed from GPS Doppler if present, or distance/time delta
        val speedMps = if (location.hasSpeed()) location.speed.toDouble() else {
            val dtSec = ((location.time - anchor.time) / 1000.0).coerceAtLeast(0.5)
            incrementalDist / dtSec
        }

        // REAL STATIONARY / REST DETECTION:
        // 1. If Doppler speed is below 0.65 m/s (~2.3 km/h, human walk threshold)
        // 2. Or incremental distance is below the GPS jitter noise floor (4.5m)
        // 3. Or incremental distance is within the location accuracy radius
        // 4. And no physical accelerometer motion or steps detected
        val isSpeedStationary = speedMps < 0.65
        val isWithinGpsNoise = incrementalDist < 4.5 || (location.hasAccuracy() && incrementalDist < location.accuracy * 0.65)
        val isHardwareStill = !isPhysicalMotionDetected

        if ((isSpeedStationary && isWithinGpsNoise) || (isWithinGpsNoise && isHardwareStill)) {
            // PHONE IS AT REST: Do NOT accumulate distance, do NOT accumulate calories, do NOT change pace
            _trackingState.value = current.copy(
                isMoving = false,
                currentPaceSecPerKm = 0.0,
                currentLocation = newPoint
            )
            return
        }

        // Filter out GPS teleport anomalies (> 35 m/s ~ 126 km/h)
        if (speedMps > 35.0 || incrementalDist > 100.0) {
            anchorLocation = location
            _trackingState.value = current.copy(currentLocation = newPoint)
            return
        }

        // ATHLETE IS ACTIVELY MOVING!
        var elevationDiff = 0.0
        val lastPoint = current.routePoints.last()
        if (newPoint.altitude > 0 && lastPoint.altitude > 0 && newPoint.altitude > lastPoint.altitude) {
            val diff = newPoint.altitude - lastPoint.altitude
            if (diff < 15.0) {
                elevationDiff = diff
            }
        }

        val newTotalDist = current.distanceMeters + incrementalDist
        val speedKmh = speedMps * 3.6
        val currentPace = if (speedKmh > 0.8) (3600.0 / speedKmh) else 0.0

        val inclinePercent = if (incrementalDist > 3.0 && elevationDiff > 0) {
            ((elevationDiff / incrementalDist) * 100.0).coerceIn(-30.0, 45.0)
        } else {
            current.currentInclinePercent
        }

        val updatedPoints = current.routePoints.toMutableList().apply { add(newPoint) }
        anchorLocation = location

        _trackingState.value = current.copy(
            isMoving = true,
            distanceMeters = newTotalDist,
            currentPaceSecPerKm = currentPace,
            elevationGainMeters = current.elevationGainMeters + elevationDiff,
            currentInclinePercent = inclinePercent,
            routePoints = updatedPoints,
            currentLocation = newPoint
        )
    }

    private fun calculateCalories(type: String, distanceMeters: Double, activeMovingSecs: Long): Int {
        if (distanceMeters < 15.0 || activeMovingSecs <= 0L) return 0

        val km = distanceMeters / 1000.0
        // Real metabolic cost per km (for ~70kg athlete):
        val calPerKm = when (type) {
            "RUN" -> 68.0   // ~1 kcal/kg/km
            "RIDE" -> 32.0  // ~0.45 kcal/kg/km
            "HIKE" -> 58.0  // ~0.85 kcal/kg/km
            else -> 45.0    // WALK: ~0.65 kcal/kg/km
        }
        val elevationBonus = _trackingState.value.elevationGainMeters * 0.05
        return ((km * calPerKm) + elevationBonus).toInt()
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("open_record_tab", true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, StrideApp.CHANNEL_ID_TRACKING)
            .setContentTitle("Stride Workout Active")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        removeLocationUpdates()
    }

    companion object {
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "com.example.action.START"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_RESUME = "com.example.action.RESUME"
        const val ACTION_STOP = "com.example.action.STOP"
        const val EXTRA_ACTIVITY_TYPE = "extra_activity_type"

        private val _trackingState = MutableStateFlow(TrackingState())
        val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

        fun resetState() {
            _trackingState.value = TrackingState()
        }

        fun formatTime(totalSeconds: Long): String {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

        fun formatDistance(meters: Double, isImperial: Boolean = false): String {
            return if (isImperial) {
                val miles = meters * 0.000621371
                String.format("%.2f mi", miles)
            } else {
                val km = meters / 1000.0
                String.format("%.2f km", km)
            }
        }

        fun formatPace(paceSecPerKm: Double, isImperial: Boolean = false): String {
            if (paceSecPerKm <= 0.0 || paceSecPerKm > 3600.0) return if (isImperial) "--:-- /mi" else "--:-- /km"
            val targetPace = if (isImperial) (paceSecPerKm * 1.60934) else paceSecPerKm
            val mins = (targetPace / 60).toInt()
            val secs = (targetPace % 60).toInt()
            val unit = if (isImperial) "/mi" else "/km"
            return String.format("%d:%02d %s", mins, secs, unit)
        }
    }
}
