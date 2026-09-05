package com.example.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityEntity
import com.example.ui.theme.CoolGrey
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GraphiteCharcoal
import com.example.ui.theme.IndigoViolet
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.OffWhite
import java.util.Calendar
import kotlin.math.max

data class DayProgress(
    val dayLabel: String,
    val distanceKm: Double,
    val durationMinutes: Long,
    val isToday: Boolean
)

@Composable
fun WeeklyProgressChart(
    activities: List<ActivityEntity>,
    modifier: Modifier = Modifier
) {
    // Metric mode: 0 = Distance (km), 1 = Duration (min)
    var selectedMetric by remember { mutableIntStateOf(0) }

    // Aggregate the last 7 days from Monday to Sunday or trailing 7 days
    val weeklyData = remember(activities) {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val cal = Calendar.getInstance()
        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val todayIndexInWeek = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - 2

        val dayBuckets = MutableList(7) { i ->
            DayProgress(
                dayLabel = days[i],
                distanceKm = 0.0,
                durationMinutes = 0L,
                isToday = (i == todayIndexInWeek)
            )
        }

        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - 7L * 24 * 3600 * 1000

        activities.forEach { act ->
            if (act.timestamp >= sevenDaysAgo) {
                cal.timeInMillis = act.timestamp
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                val idx = if (dow == Calendar.SUNDAY) 6 else dow - 2
                if (idx in 0..6) {
                    val prev = dayBuckets[idx]
                    dayBuckets[idx] = prev.copy(
                        distanceKm = prev.distanceKm + (act.distanceMeters / 1000.0),
                        durationMinutes = prev.durationMinutes + (act.durationSeconds / 60)
                    )
                }
            }
        }
        dayBuckets
    }

    val totalDistanceKm = weeklyData.sumOf { it.distanceKm }
    val totalDurationMinutes = weeklyData.sumOf { it.durationMinutes }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(selectedMetric) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color(0x3300E5C7))
            .testTag("weekly_progress_chart_card"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF2E3340)),
        colors = CardDefaults.cardColors(containerColor = GraphiteCharcoal)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with toggle button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Weekly Progress",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OffWhite
                    )
                    Text(
                        text = if (selectedMetric == 0)
                            String.format("%.1f km total this week", totalDistanceKm)
                        else
                            "${totalDurationMinutes / 60}h ${totalDurationMinutes % 60}m total this week",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ElectricCyan
                    )
                }

                // Toggle tabs
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF252932))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedMetric == 0) ElectricCyan else Color.Transparent)
                            .clickable { selectedMetric = 0 }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Distance",
                            fontSize = 12.sp,
                            fontWeight = if (selectedMetric == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedMetric == 0) ObsidianBlack else CoolGrey
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedMetric == 1) ElectricCyan else Color.Transparent)
                            .clickable { selectedMetric = 1 }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Duration",
                            fontSize = 12.sp,
                            fontWeight = if (selectedMetric == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedMetric == 1) ObsidianBlack else CoolGrey
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bar chart canvas
            val maxValue = remember(weeklyData, selectedMetric) {
                val values = weeklyData.map { if (selectedMetric == 0) it.distanceKm else it.durationMinutes.toDouble() }
                max(values.maxOrNull() ?: 1.0, if (selectedMetric == 0) 10.0 else 60.0)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                    val width = size.width
                    val height = size.height
                    val barCount = weeklyData.size
                    val spacing = width / barCount
                    val barWidth = 24.dp.toPx()

                    // Horizontal background grid lines
                    val gridLines = 3
                    for (g in 0..gridLines) {
                        val y = height - (g.toFloat() / gridLines) * (height - 10.dp.toPx())
                        drawLine(
                            color = Color(0xFF252932),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    weeklyData.forEachIndexed { i, day ->
                        val value = if (selectedMetric == 0) day.distanceKm else day.durationMinutes.toDouble()
                        val fraction = (value / maxValue).toFloat().coerceIn(0f, 1f)
                        val barHeight = (fraction * (height - 12.dp.toPx()) * animProgress.value).coerceAtLeast(6.dp.toPx())
                        val centerX = spacing * i + spacing / 2f
                        val left = centerX - barWidth / 2f
                        val top = height - barHeight

                        // Background capsule track
                        drawRoundRect(
                            color = Color(0xFF252932),
                            topLeft = Offset(left, 0f),
                            size = Size(barWidth, height),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )

                        // Filled active bar: ElectricCyan if today, IndigoViolet if other day
                        val barColor = if (day.isToday) ElectricCyan else IndigoViolet
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }
                }
            }

            // X-axis Day labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeklyData.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(36.dp)
                    ) {
                        Text(
                            text = day.dayLabel,
                            fontSize = 11.sp,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (day.isToday) ElectricCyan else CoolGrey
                        )
                        if (day.isToday) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(ElectricCyan)
                            )
                        }
                    }
                }
            }
        }
    }
}
