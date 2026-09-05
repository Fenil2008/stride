package com.example.ui.explore

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.model.ActivityEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.StrideRepository
import com.example.service.TrackingService
import com.example.ui.common.parsePolylineJson
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Creates high-visibility GPS location marker with Electric Cyan aura.
 */
private fun createHighPrecisionLocationMarker(context: Context): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val size = (44 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val cx = size / 2f
    val cy = size / 2f

    // Outer translucent Electric Cyan pulse ring
    paint.color = AndroidColor.argb(50, 0, 229, 199)
    canvas.drawCircle(cx, cy, size / 2f, paint)

    // Inner bright cyan halo
    paint.color = AndroidColor.argb(100, 0, 229, 199)
    canvas.drawCircle(cx, cy, size * 0.36f, paint)

    // Solid dark ring border
    paint.color = AndroidColor.parseColor("#0B0D12")
    canvas.drawCircle(cx, cy, size * 0.28f, paint)

    // Core vibrant Electric Cyan dot
    paint.color = AndroidColor.parseColor("#00E5C7")
    canvas.drawCircle(cx, cy, size * 0.20f, paint)

    // Center white reflection point
    paint.color = AndroidColor.WHITE
    canvas.drawCircle(cx, cy, size * 0.07f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Creates a pulsating leading indicator marker for live GPS recording.
 */
private fun createLivePositionMarker(context: Context): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val size = (48 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val cx = size / 2f
    val cy = size / 2f

    // Outer live pulse aura
    paint.color = AndroidColor.argb(70, 0, 229, 199)
    canvas.drawCircle(cx, cy, size / 2f, paint)

    // Intense vibrant cyan core
    paint.color = AndroidColor.parseColor("#00E5C7")
    canvas.drawCircle(cx, cy, size * 0.32f, paint)

    // Inner white point
    paint.color = AndroidColor.WHITE
    canvas.drawCircle(cx, cy, size * 0.15f, paint)

    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Creates custom Start (S) or Finish (F) pin markers in Stride palette.
 */
private fun createRoutePin(context: Context, label: String, hexColor: String): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val size = (30 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val cx = size / 2f
    val cy = size / 2f

    // Shadow
    paint.color = AndroidColor.argb(90, 0, 0, 0)
    canvas.drawCircle(cx, cy + 2 * density, size * 0.44f, paint)

    // Obsidian outline
    paint.color = AndroidColor.parseColor("#0B0D12")
    canvas.drawCircle(cx, cy, size * 0.44f, paint)

    // Colored body
    paint.color = AndroidColor.parseColor(hexColor)
    canvas.drawCircle(cx, cy, size * 0.36f, paint)

    // Label text
    paint.color = AndroidColor.parseColor("#0B0D12")
    paint.textSize = size * 0.36f
    paint.textAlign = Paint.Align.CENTER
    paint.isFakeBoldText = true
    canvas.drawText(label, cx, cy + (paint.textSize / 3), paint)

    return BitmapDrawable(context.resources, bitmap)
}

// Distinct athletic palette for personal routes
private val routeColors = listOf(
    "#00E5C7", // Electric Cyan
    "#6C5CE7", // Indigo Violet
    "#3DDC97", // Mint Green
    "#FFB703", // Amber Gold
    "#FF6B6B", // Soft Coral
    "#38BDF8", // Sky Blue
    "#F72585", // Neon Pink
    "#4CC9F0"  // Vivid Cyan
)

@Composable
fun ExploreScreen(
    strideRepository: StrideRepository,
    authRepository: AuthRepository,
    onActivityClick: (String) -> Unit = {},
    onNavigateToRecord: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val currentUser by authRepository.currentUser.collectAsState()
    val currentUid = currentUser?.uid ?: ""
    val allActivities by strideRepository.allActivities.collectAsState(initial = emptyList())

    // Filter for the logged-in user's own explored activities
    val userActivities = remember(allActivities, currentUid) {
        if (currentUid.isBlank()) {
            allActivities
        } else {
            allActivities.filter { it.userId == currentUid || it.userId.startsWith(currentUid) }
        }
    }

    // Active recording tracking state from TrackingService
    val trackingState by TrackingService.trackingState.collectAsState()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    // GPS & Locality state
    var currentLatitude by remember { mutableStateOf(37.7749) }
    var currentLongitude by remember { mutableStateOf(-122.4194) }
    var localityName by remember { mutableStateOf("My Area") }
    var isLocating by remember { mutableStateOf(false) }

    // Selection
    var selectedActivity by remember { mutableStateOf<ActivityEntity?>(null) }

    // Map & Overlays reference
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var userLocationMarker by remember { mutableStateOf<Marker?>(null) }
    var livePolyline by remember { mutableStateOf<Polyline?>(null) }
    var liveMarker by remember { mutableStateOf<Marker?>(null) }

    // List of active route overlays to manage clearing
    val pastRouteOverlays = remember { mutableListOf<org.osmdroid.views.overlay.Overlay>() }

    // Refresh explored route overlays on the map
    fun redrawUserRoutes(mv: MapView) {
        // Remove old past route overlays
        pastRouteOverlays.forEach { mv.overlays.remove(it) }
        pastRouteOverlays.clear()

        userActivities.forEachIndexed { index, act ->
            val points = parsePolylineJson(act.polylineJson)
            if (points.isNotEmpty()) {
                val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }
                val colorHex = routeColors[index % routeColors.size]
                val isSelected = selectedActivity?.id == act.id

                val poly = Polyline(mv).apply {
                    outlinePaint.color = AndroidColor.parseColor(colorHex)
                    outlinePaint.strokeWidth = if (isSelected) 18f else 11f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                    setPoints(geoPoints)
                    setOnClickListener { _, _, _ ->
                        selectedActivity = act
                        true
                    }
                }
                mv.overlays.add(poly)
                pastRouteOverlays.add(poly)

                // If this route is selected or if there are few routes, add Start/Finish markers
                if (isSelected) {
                    val startMarker = Marker(mv).apply {
                        position = geoPoints.first()
                        icon = createRoutePin(context, "S", "#3DDC97")
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "${act.title} - Start"
                    }
                    val finishMarker = Marker(mv).apply {
                        position = geoPoints.last()
                        icon = createRoutePin(context, "F", colorHex)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "${act.title} - Finish"
                    }
                    mv.overlays.add(startMarker)
                    mv.overlays.add(finishMarker)
                    pastRouteOverlays.add(startMarker)
                    pastRouteOverlays.add(finishMarker)
                }
            }
        }
        mv.invalidate()
    }

    // Function to fetch current location and recenter map
    fun refreshCurrentLocation(animateMap: Boolean = true) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            isLocating = true
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc: Location? ->
                    isLocating = false
                    if (loc != null) {
                        currentLatitude = loc.latitude
                        currentLongitude = loc.longitude

                        scope.launch {
                            val resolvedName = withContext(Dispatchers.IO) {
                                try {
                                    val geocoder = Geocoder(context, Locale.getDefault())
                                    val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                                    val addr = addresses?.firstOrNull()
                                    addr?.subLocality ?: addr?.locality ?: addr?.subAdminArea ?: "My Area"
                                } catch (e: Exception) {
                                    "My Area"
                                }
                            }
                            localityName = resolvedName

                            mapViewRef?.let { mv ->
                                val userGeo = GeoPoint(loc.latitude, loc.longitude)
                                if (userLocationMarker == null) {
                                    userLocationMarker = Marker(mv).apply {
                                        position = userGeo
                                        icon = createHighPrecisionLocationMarker(context)
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                        title = "You are here"
                                        mv.overlays.add(this)
                                    }
                                } else {
                                    userLocationMarker?.position = userGeo
                                }

                                if (animateMap) {
                                    mv.controller.animateTo(userGeo)
                                    mv.controller.setZoom(15.5)
                                }
                                mv.invalidate()
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    isLocating = false
                }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fineGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            hasLocationPermission = true
            refreshCurrentLocation(animateMap = true)
        }
    }

    // Request permissions and initial auto-center on load
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            refreshCurrentLocation(animateMap = true)
        }
    }

    // Redraw user's explored routes when activities change or when a route is selected
    LaunchedEffect(userActivities, selectedActivity, mapViewRef) {
        mapViewRef?.let { mv ->
            redrawUserRoutes(mv)
        }
    }

    // Live Tracking Route Progressive Drawing
    LaunchedEffect(trackingState.routePoints, trackingState.isTracking, mapViewRef) {
        val mv = mapViewRef ?: return@LaunchedEffect
        if (trackingState.isTracking && trackingState.routePoints.isNotEmpty()) {
            val liveGeoPoints = trackingState.routePoints.map { GeoPoint(it.latitude, it.longitude) }

            // Update or create live tracking polyline
            if (livePolyline == null) {
                livePolyline = Polyline(mv).apply {
                    outlinePaint.color = AndroidColor.parseColor("#00E5C7")
                    outlinePaint.strokeWidth = 16f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                    setPoints(liveGeoPoints)
                }
                mv.overlays.add(livePolyline)
            } else {
                livePolyline?.setPoints(liveGeoPoints)
            }

            // Update or create live moving position marker at the leading end
            val lastPoint = liveGeoPoints.last()
            if (liveMarker == null) {
                liveMarker = Marker(mv).apply {
                    position = lastPoint
                    icon = createLivePositionMarker(context)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "Live Position"
                }
                mv.overlays.add(liveMarker)
            } else {
                liveMarker?.position = lastPoint
            }

            mv.invalidate()
        } else {
            // Remove live tracking overlays if not actively tracking
            if (livePolyline != null) {
                mv.overlays.remove(livePolyline)
                livePolyline = null
            }
            if (liveMarker != null) {
                mv.overlays.remove(liveMarker)
                liveMarker = null
            }
            mv.invalidate()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ObsidianBlack
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // High-contrast Dark Tactical OSM Map View
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("explore_map_view"),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)
                        controller.setCenter(GeoPoint(currentLatitude, currentLongitude))

                        // High contrast night-mode athletic palette filter
                        val matrix = ColorMatrix(
                            floatArrayOf(
                                -0.80f, 0f, 0f, 0f, 210f,
                                0f, -0.80f, 0f, 0f, 215f,
                                0f, 0f, -0.75f, 0f, 225f,
                                0f, 0f, 0f, 1f, 0f
                            )
                        )
                        val filter = ColorMatrixColorFilter(matrix)
                        overlayManager.tilesOverlay.setColorFilter(filter)

                        mapViewRef = this
                        refreshCurrentLocation(animateMap = true)
                    }
                },
                update = { mv ->
                    mapViewRef = mv
                }
            )

            // Top Header: Explored Path History Status
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GraphiteCharcoal.copy(alpha = 0.94f)),
                    border = BorderStroke(1.dp, Color(0xFF2E3340)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color.Black)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Explored Map",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = OffWhite,
                                    letterSpacing = (-0.3).sp
                                )
                                if (trackingState.isTracking) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0x2600E5C7))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.FiberManualRecord,
                                                contentDescription = null,
                                                tint = ElectricCyan,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "RECORDING LIVE",
                                                color = ElectricCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = if (userActivities.isEmpty()) {
                                    "No explored paths yet • $localityName"
                                } else {
                                    "${userActivities.size} personal route${if (userActivities.size > 1) "s" else ""} • $localityName"
                                },
                                fontSize = 12.sp,
                                color = CoolGrey
                            )
                        }

                        // Recenter button
                        IconButton(
                            onClick = { refreshCurrentLocation(animateMap = true) },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFF252932), CircleShape)
                                .border(1.dp, Color(0xFF353B4A), CircleShape)
                                .testTag("button_recenter_location")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Recenter to My Location",
                                tint = if (isLocating) ElectricCyan else OffWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Section: Explored Route Carousel, Selected Route Detail Card, or Empty State
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp)
            ) {
                // Live workout banner if recording
                if (trackingState.isTracking) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .shadow(8.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = GraphiteCharcoal.copy(alpha = 0.95f)),
                        border = BorderStroke(1.dp, ElectricCyan)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Live Activity in Progress",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )
                                Text(
                                    text = "${TrackingService.formatDistance(trackingState.distanceMeters)} • ${TrackingService.formatTime(trackingState.elapsedSeconds)}",
                                    fontSize = 13.sp,
                                    color = OffWhite,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Button(
                                onClick = onNavigateToRecord,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("button_view_live_record")
                            ) {
                                Text("View Live", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Selected Route Detail Card
                if (selectedActivity != null) {
                    val act = selectedActivity!!
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .shadow(12.dp, RoundedCornerShape(22.dp)),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = GraphiteCharcoal.copy(alpha = 0.96f)),
                        border = BorderStroke(1.5.dp, ElectricCyan)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val icon = when (act.activityType.uppercase()) {
                                        "RIDE" -> Icons.Default.DirectionsBike
                                        "WALK" -> Icons.Default.DirectionsWalk
                                        "HIKE" -> Icons.Default.Hiking
                                        else -> Icons.Default.DirectionsRun
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = act.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OffWhite
                                    )
                                }
                                IconButton(
                                    onClick = { selectedActivity = null },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = CoolGrey,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Distance", fontSize = 11.sp, color = CoolGrey)
                                    Text(
                                        text = TrackingService.formatDistance(act.distanceMeters),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = OffWhite
                                    )
                                }
                                Column {
                                    Text("Time", fontSize = 11.sp, color = CoolGrey)
                                    Text(
                                        text = TrackingService.formatTime(act.durationSeconds),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = OffWhite
                                    )
                                }
                                Column {
                                    Text("Pace", fontSize = 11.sp, color = CoolGrey)
                                    Text(
                                        text = TrackingService.formatPace(act.avgPaceSecPerKm),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = OffWhite
                                    )
                                }
                                Column {
                                    Text("Kudos", fontSize = 11.sp, color = CoolGrey)
                                    Text(
                                        text = "${act.kudosCount}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MintGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { onActivityClick(act.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("button_open_activity_detail"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = ObsidianBlack
                                )
                            ) {
                                Text("Open Activity Details", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else if (userActivities.isNotEmpty()) {
                    // Explored routes quick carousel
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(userActivities, key = { it.id }) { act ->
                            val isSelected = selectedActivity?.id == act.id
                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .clickable {
                                        selectedActivity = act
                                        val points = parsePolylineJson(act.polylineJson)
                                        if (points.isNotEmpty()) {
                                            mapViewRef?.controller?.animateTo(GeoPoint(points.first().latitude, points.first().longitude))
                                            mapViewRef?.controller?.setZoom(15.5)
                                        }
                                    }
                                    .testTag("route_card_${act.id}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF1E2430) else GraphiteCharcoal.copy(alpha = 0.92f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) ElectricCyan else Color(0xFF2E3340)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = act.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OffWhite,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = TrackingService.formatDistance(act.distanceMeters),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ElectricCyan
                                        )
                                        Text(
                                            text = TrackingService.formatTime(act.durationSeconds),
                                            fontSize = 12.sp,
                                            color = CoolGrey
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (!trackingState.isTracking) {
                    // Empty state when user has no activities recorded yet
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .shadow(8.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = GraphiteCharcoal.copy(alpha = 0.94f)),
                        border = BorderStroke(1.dp, Color(0xFF2E3340))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Route,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Your Explored Map",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = OffWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your explored routes will appear here once you complete your first activity.",
                                fontSize = 13.sp,
                                color = CoolGrey,
                                modifier = Modifier.padding(horizontal = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onNavigateToRecord,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = ObsidianBlack
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("button_explore_record_first")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsRun,
                                    contentDescription = null,
                                    tint = ObsidianBlack,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Record First Activity",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
