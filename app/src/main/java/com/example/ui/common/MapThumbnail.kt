package com.example.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GraphiteCharcoal
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianBlack
import org.json.JSONArray
import kotlin.math.max
import kotlin.math.min

data class LatLng(val latitude: Double, val longitude: Double)

fun parsePolylineJson(json: String?): List<LatLng> {
    if (json.isNullOrBlank()) return emptyList()
    val list = mutableListOf<LatLng>()
    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val lat = if (obj.has("lat")) obj.getDouble("lat") else obj.optDouble("latitude", 0.0)
            val lng = if (obj.has("lng")) obj.getDouble("lng") else obj.optDouble("longitude", 0.0)
            list.add(LatLng(lat, lng))
        }
    } catch (e: Exception) {
        // Fallback or empty
    }
    return list
}

@Composable
fun PolylineMapThumbnail(
    polylineJson: String,
    modifier: Modifier = Modifier,
    lineColor: Color = ElectricCyan
) {
    val points = remember(polylineJson) { parsePolylineJson(polylineJson) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF14171F))
            .border(1.dp, Color(0xFF2E3340), RoundedCornerShape(16.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val padding = 36f

            // Draw dark tactical grid lines
            val gridStep = 45f
            var x = 0f
            while (x < width) {
                drawLine(
                    color = Color(0x14EAEAF0),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += gridStep
            }
            var y = 0f
            while (y < height) {
                drawLine(
                    color = Color(0x14EAEAF0),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                y += gridStep
            }

            if (points.size >= 2) {
                var minLat = Double.MAX_VALUE
                var maxLat = -Double.MAX_VALUE
                var minLng = Double.MAX_VALUE
                var maxLng = -Double.MAX_VALUE

                points.forEach {
                    minLat = min(minLat, it.latitude)
                    maxLat = max(maxLat, it.latitude)
                    minLng = min(minLng, it.longitude)
                    maxLng = max(maxLng, it.longitude)
                }

                val latRange = max(maxLat - minLat, 0.0005)
                val lngRange = max(maxLng - minLng, 0.0005)

                val drawWidth = width - (padding * 2)
                val drawHeight = height - (padding * 2)

                val path = Path()
                points.forEachIndexed { index, pt ->
                    // Normalize to canvas coordinates (latitude is inverted for Y axis)
                    val normX = ((pt.longitude - minLng) / lngRange).toFloat()
                    val normY = (1.0f - ((pt.latitude - minLat) / latRange).toFloat())

                    val canvasX = padding + (normX * drawWidth)
                    val canvasY = padding + (normY * drawHeight)

                    if (index == 0) {
                        path.moveTo(canvasX, canvasY)
                    } else {
                        path.lineTo(canvasX, canvasY)
                    }
                }

                // Cyan glow under the polyline
                drawPath(
                    path = path,
                    color = Color(0x3300E5C7),
                    style = Stroke(
                        width = 14f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Main route polyline
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = 6.5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Start Marker (Mint green dot)
                val startPt = points.first()
                val startX = padding + (((startPt.longitude - minLng) / lngRange).toFloat() * drawWidth)
                val startY = padding + ((1.0f - ((startPt.latitude - minLat) / latRange).toFloat()) * drawHeight)
                drawCircle(color = ObsidianBlack, radius = 8f, center = Offset(startX, startY))
                drawCircle(color = MintGreen, radius = 5.5f, center = Offset(startX, startY))

                // End Marker (Electric Cyan dot)
                val endPt = points.last()
                val endX = padding + (((endPt.longitude - minLng) / lngRange).toFloat() * drawWidth)
                val endY = padding + ((1.0f - ((endPt.latitude - minLat) / latRange).toFloat()) * drawHeight)
                drawCircle(color = ObsidianBlack, radius = 9f, center = Offset(endX, endY))
                drawCircle(color = lineColor, radius = 6f, center = Offset(endX, endY))
                drawCircle(color = ObsidianBlack, radius = 2.5f, center = Offset(endX, endY))
            }
        }
    }
}
