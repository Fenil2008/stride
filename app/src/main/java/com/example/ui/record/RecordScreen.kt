package com.example.ui.record

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.service.TrackingService
import com.example.ui.theme.CoolGrey
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GraphiteCharcoal
import com.example.ui.theme.IndigoViolet
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.OffWhite
import com.example.ui.theme.SoftCoral
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private fun createHighPrecisionLocationMarker(context: Context): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val size = (44 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val cx = size / 2f
    val cy = size / 2f

    paint.color = AndroidColor.argb(50, 0, 229, 199)
    canvas.drawCircle(cx, cy, size / 2f, paint)

    paint.color = AndroidColor.argb(100, 0, 229, 199)
    canvas.drawCircle(cx, cy, size * 0.36f, paint)

    paint.color = AndroidColor.parseColor("#0B0D12")
    canvas.drawCircle(cx, cy, size * 0.28f, paint)

    paint.color = AndroidColor.parseColor("#00E5C7")
    canvas.drawCircle(cx, cy, size * 0.20f, paint)

    paint.color = AndroidColor.WHITE
    canvas.drawCircle(cx, cy, size * 0.07f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

@Composable
fun RecordScreen(
    onFinishWorkout: (
        activityType: String,
        distanceMeters: Double,
        durationSeconds: Long,
        avgPaceSecPerKm: Double,
        calories: Int,
        polylineJson: String
    ) -> Unit
) {
    val context = LocalContext.current
    val trackingState by TrackingService.trackingState.collectAsState()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val scope = rememberCoroutineScope()

    var selectedActivityType by remember { mutableStateOf("RUN") }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            val perms = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                perms.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            permissionLauncher.launch(perms.toTypedArray())
        }
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var routePolylineOverlay by remember { mutableStateOf<Polyline?>(null) }
    var userLocationMarker by remember { mutableStateOf<Marker?>(null) }

    // Pulsing animation for active record button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Center map on user's real GPS position
    fun recenterToUserLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null && mapViewRef != null) {
                        val geo = GeoPoint(loc.latitude, loc.longitude)
                        mapViewRef?.controller?.animateTo(geo)
                        mapViewRef?.controller?.setZoom(17.0)

                        if (userLocationMarker == null) {
                            userLocationMarker = Marker(mapViewRef).apply {
                                position = geo
                                icon = createHighPrecisionLocationMarker(context)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                title = "Your Location"
                                mapViewRef?.overlays?.add(this)
                            }
                        } else {
                            userLocationMarker?.position = geo
                        }
                        mapViewRef?.invalidate()
                    }
                }
        }
    }

    // Update map polyline when route points change
    LaunchedEffect(trackingState.routePoints.size) {
        val mv = mapViewRef ?: return@LaunchedEffect
        val points = trackingState.routePoints
        if (points.isNotEmpty()) {
            val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }

            if (routePolylineOverlay == null) {
                routePolylineOverlay = Polyline(mv).apply {
                    // Electric Cyan route polyline
                    outlinePaint.color = AndroidColor.parseColor("#00E5C7")
                    outlinePaint.strokeWidth = 14f
                    outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
                    outlinePaint.strokeJoin = android.graphics.Paint.Join.ROUND
                    setPoints(geoPoints)
                    mv.overlays.add(this)
                }
            } else {
                routePolylineOverlay?.setPoints(geoPoints)
            }

            val latest = geoPoints.last()
            mv.controller.animateTo(latest)

            if (userLocationMarker == null) {
                userLocationMarker = Marker(mv).apply {
                    position = latest
                    icon = createHighPrecisionLocationMarker(context)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    mv.overlays.add(this)
                }
            } else {
                userLocationMarker?.position = latest
            }
            mv.invalidate()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = ObsidianBlack) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Full-screen osmdroid interactive Map with Dark Mode filter
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("record_map_view"),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                        controller.setZoom(16.5)
                        controller.setCenter(GeoPoint(37.7749, -122.4194))

                        // High-contrast Dark Mode tiles filter
                        val darkMatrix = ColorMatrix(
                            floatArrayOf(
                                -0.85f, 0f, 0f, 0f, 215f,
                                0f, -0.85f, 0f, 0f, 215f,
                                0f, 0f, -0.85f, 0f, 215f,
                                0f, 0f, 0f, 1.0f, 0f
                            )
                        )
                        overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(darkMatrix))

                        mapViewRef = this
                        recenterToUserLocation()
                    }
                },
                update = {
                    mapViewRef = it
                }
            )

            // Top Status & Activity Selector Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (!trackingState.isTracking) {
                    // Activity Type Selector Chips (Graphite Charcoal Card)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color(0x3300E5C7)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E3340)),
                        colors = CardDefaults.cardColors(containerColor = GraphiteCharcoal),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            val types = listOf(
                                "RUN" to ("Run" to Icons.Default.DirectionsRun),
                                "RIDE" to ("Ride" to Icons.Default.DirectionsBike),
                                "WALK" to ("Walk" to Icons.Default.DirectionsWalk),
                                "HIKE" to ("Hike" to Icons.Default.Hiking)
                            )
                            types.forEach { (typeKey, pair) ->
                                val (label, icon) = pair
                                val isSelected = selectedActivityType == typeKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedActivityType = typeKey },
                                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                    leadingIcon = {
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = Color(0xFF252932),
                                        labelColor = CoolGrey,
                                        iconColor = CoolGrey,
                                        selectedContainerColor = ElectricCyan,
                                        selectedLabelColor = ObsidianBlack,
                                        selectedLeadingIconColor = ObsidianBlack
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) ElectricCyan else Color(0xFF2E3340)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.testTag("select_activity_$typeKey")
                                )
                            }
                        }
                    }
                } else {
                    // Active Workout Header Badge in Indigo Violet
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (trackingState.isPaused) Color(0xFFEAB308) else IndigoViolet
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (trackingState.isPaused) ObsidianBlack else MintGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (trackingState.isPaused) "WORKOUT PAUSED" else "RECORDING ${trackingState.activityType}",
                                    color = if (trackingState.isPaused) ObsidianBlack else OffWhite,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = TrackingService.formatTime(trackingState.elapsedSeconds),
                                color = if (trackingState.isPaused) ObsidianBlack else OffWhite,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }

            // Bottom Live Metrics Overlay & Control Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 84.dp)
            ) {
                // Floating Recenter to My Location Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { recenterToUserLocation() },
                        modifier = Modifier
                            .size(50.dp)
                            .shadow(8.dp, CircleShape, spotColor = Color(0x3300E5C7))
                            .background(GraphiteCharcoal, CircleShape)
                            .border(1.dp, ElectricCyan, CircleShape)
                            .testTag("button_recenter_location")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Recenter to My Location",
                            tint = ElectricCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Live Stats HUD Card in Graphite Charcoal
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Color(0x3300E5C7)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFF2E3340)),
                    colors = CardDefaults.cardColors(containerColor = GraphiteCharcoal),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        // Motion Status Indicator (Shows when phone is at rest vs in active motion)
                        if (trackingState.isTracking) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = when {
                                                trackingState.isPaused -> Color(0xFFFFB300)
                                                trackingState.isMoving -> ElectricCyan
                                                else -> CoolGrey
                                            },
                                            shape = CircleShape
                                        )
                                )
                                Text(
                                    text = when {
                                        trackingState.isPaused -> "PAUSED"
                                        trackingState.isMoving -> "ACTIVE MOTION"
                                        else -> "STATIONARY (AT REST)"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = when {
                                        trackingState.isPaused -> Color(0xFFFFB300)
                                        trackingState.isMoving -> ElectricCyan
                                        else -> CoolGrey
                                    }
                                )
                            }
                        }

                        // Main Stats 3-Col
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("TIME", fontSize = 10.sp, color = CoolGrey, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Text(
                                    text = TrackingService.formatTime(trackingState.elapsedSeconds),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OffWhite
                                )
                            }

                            Column {
                                Text("DISTANCE", fontSize = 10.sp, color = CoolGrey, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Text(
                                    text = TrackingService.formatDistance(trackingState.distanceMeters),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ElectricCyan
                                )
                            }

                            Column {
                                Text("AVG PACE", fontSize = 10.sp, color = CoolGrey, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Text(
                                    text = TrackingService.formatPace(trackingState.avgPaceSecPerKm),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OffWhite
                                )
                            }
                        }

                        // Secondary Real-time Metrics: Steps & Incline / Elevation & Calories
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    text = "Steps: ${trackingState.steps}",
                                    fontSize = 12.sp,
                                    color = OffWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Incline: ${String.format("%.1f", trackingState.currentInclinePercent)}%",
                                    fontSize = 12.sp,
                                    color = OffWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "Elev: +${trackingState.elevationGainMeters.toInt()}m",
                                    fontSize = 12.sp,
                                    color = CoolGrey,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${trackingState.calories} kcal",
                                    fontSize = 12.sp,
                                    color = CoolGrey,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Control Buttons (Start / Pause / Resume / Finish)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!trackingState.isTracking) {
                        // Large Circular Start Button in Cyan-Indigo gradient
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .scale(pulseScale)
                                .shadow(14.dp, CircleShape, spotColor = ElectricCyan)
                                .background(
                                    Brush.linearGradient(listOf(ElectricCyan, IndigoViolet)),
                                    CircleShape
                                )
                                .clickable {
                                    val intent = Intent(context, TrackingService::class.java).apply {
                                        action = TrackingService.ACTION_START
                                        putExtra(TrackingService.EXTRA_ACTIVITY_TYPE, selectedActivityType)
                                    }
                                    ContextCompat.startForegroundService(context, intent)
                                }
                                .testTag("button_start_workout"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start Workout",
                                    tint = ObsidianBlack,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    "START",
                                    color = ObsidianBlack,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    } else {
                        // Pause / Resume Button
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .shadow(8.dp, CircleShape, spotColor = IndigoViolet)
                                .background(if (trackingState.isPaused) ElectricCyan else IndigoViolet, CircleShape)
                                .clickable {
                                    val intent = Intent(context, TrackingService::class.java).apply {
                                        action = if (trackingState.isPaused) TrackingService.ACTION_RESUME else TrackingService.ACTION_PAUSE
                                    }
                                    context.startService(intent)
                                }
                                .testTag("button_pause_resume_workout"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (trackingState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (trackingState.isPaused) "Resume" else "Pause",
                                tint = if (trackingState.isPaused) ObsidianBlack else OffWhite,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(32.dp))

                        // Finish / Stop Workout Button in Soft Coral
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .shadow(8.dp, CircleShape, spotColor = SoftCoral)
                                .background(SoftCoral, CircleShape)
                                .clickable {
                                    val finalDist = trackingState.distanceMeters
                                    val finalTime = trackingState.elapsedSeconds
                                    val finalPace = trackingState.avgPaceSecPerKm
                                    val finalCal = trackingState.calories
                                    val finalType = trackingState.activityType
                                    val points = trackingState.routePoints

                                    // Build polyline JSON
                                    val jsonBuilder = StringBuilder("[")
                                    points.forEachIndexed { idx, pt ->
                                        jsonBuilder.append("""{"lat":${pt.latitude},"lng":${pt.longitude}}""")
                                        if (idx < points.size - 1) jsonBuilder.append(",")
                                    }
                                    jsonBuilder.append("]")

                                    // Stop background service
                                    val stopIntent = Intent(context, TrackingService::class.java).apply {
                                        action = TrackingService.ACTION_STOP
                                    }
                                    context.startService(stopIntent)
                                    TrackingService.resetState()

                                    // Navigate to summary screen
                                    onFinishWorkout(
                                        finalType,
                                        finalDist,
                                        finalTime,
                                        finalPace,
                                        finalCal,
                                        jsonBuilder.toString()
                                    )
                                }
                                .testTag("button_finish_workout"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Finish Workout",
                                tint = ObsidianBlack,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
