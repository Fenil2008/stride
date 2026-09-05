package com.example.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityEntity
import com.example.data.repository.StrideRepository
import com.example.service.TrackingService
import com.example.ui.common.PolylineMapThumbnail
import com.example.ui.feed.formatRelativeTime
import com.example.ui.feed.getActivityIcon
import com.example.ui.theme.CoolGrey
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GraphiteCharcoal
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.OffWhite

@Composable
fun HistoryScreen(
    strideRepository: StrideRepository,
    onActivityClick: (String) -> Unit
) {
    val allActivities by strideRepository.allActivities.collectAsState(initial = emptyList())
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredActivities = remember(allActivities, selectedFilter) {
        if (selectedFilter == "ALL") allActivities
        else allActivities.filter { it.activityType.equals(selectedFilter, ignoreCase = true) }
    }

    val totalDistanceMeters = remember(filteredActivities) {
        filteredActivities.sumOf { it.distanceMeters }
    }
    val totalSeconds = remember(filteredActivities) {
        filteredActivities.sumOf { it.durationSeconds }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ObsidianBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Activity Log",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = OffWhite
                )
                Text(
                    text = "Your complete training archive and milestones",
                    fontSize = 13.sp,
                    color = CoolGrey,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Summary Stats Banner in Graphite Charcoal
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color(0x3300E5C7)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF2E3340)),
                colors = CardDefaults.cardColors(containerColor = GraphiteCharcoal)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "ACTIVITIES",
                            fontSize = 10.sp,
                            color = CoolGrey,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${filteredActivities.size}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = ElectricCyan
                        )
                    }
                    Column {
                        Text(
                            "DISTANCE",
                            fontSize = 10.sp,
                            color = CoolGrey,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = TrackingService.formatDistance(totalDistanceMeters),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = OffWhite
                        )
                    }
                    Column {
                        Text(
                            "TIME ACTIVE",
                            fontSize = 10.sp,
                            color = CoolGrey,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = TrackingService.formatTime(totalSeconds),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = OffWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "ALL" to "All",
                    "RUN" to "Runs",
                    "RIDE" to "Rides",
                    "WALK" to "Walks",
                    "HIKE" to "Hikes"
                )
                items(filters) { (type, label) ->
                    val isSelected = selectedFilter == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = type },
                        label = {
                            Text(
                                label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan,
                            selectedLabelColor = ObsidianBlack,
                            containerColor = Color(0xFF252932),
                            labelColor = CoolGrey
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) ElectricCyan else Color(0xFF2E3340),
                            borderWidth = 1.dp,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("history_filter_$type")
                    )
                }
            }

            // List of Workouts
            if (filteredActivities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No activities found for this filter.", color = CoolGrey, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredActivities, key = { it.id }) { activity ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400), initialOffsetY = { 30 }),
                            exit = fadeOut(animationSpec = tween(200))
                        ) {
                            HistoryActivityRow(
                                activity = activity,
                                onClick = { onActivityClick(activity.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryActivityRow(
    activity: ActivityEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x3300E5C7))
            .testTag("history_row_${activity.id}"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF2E3340)),
        colors = CardDefaults.cardColors(containerColor = GraphiteCharcoal)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Map mini thumbnail with dark container
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF252932))
            ) {
                PolylineMapThumbnail(
                    polylineJson = activity.polylineJson,
                    modifier = Modifier.fillMaxSize(),
                    lineColor = ElectricCyan
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getActivityIcon(activity.activityType),
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = activity.activityType.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = ElectricCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${formatRelativeTime(activity.timestamp)}",
                        fontSize = 11.sp,
                        color = CoolGrey
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = activity.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OffWhite,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = TrackingService.formatDistance(activity.distanceMeters),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                    Text(
                        text = TrackingService.formatTime(activity.durationSeconds),
                        fontSize = 13.sp,
                        color = CoolGrey
                    )
                    Text(
                        text = TrackingService.formatPace(activity.avgPaceSecPerKm),
                        fontSize = 13.sp,
                        color = CoolGrey
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = CoolGrey,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
