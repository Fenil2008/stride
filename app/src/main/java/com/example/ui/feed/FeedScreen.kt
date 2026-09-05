package com.example.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.StrideRepository
import com.example.service.TrackingService
import com.example.ui.common.PolylineMapThumbnail
import com.example.ui.common.StrideMarkIcon
import com.example.ui.theme.CoolGrey
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GraphiteCharcoal
import com.example.ui.theme.IndigoViolet
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.OffWhite
import com.example.ui.theme.SoftCoral
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    strideRepository: StrideRepository,
    authRepository: AuthRepository,
    onActivityClick: (String) -> Unit
) {
    val activities by strideRepository.allActivities.collectAsState(initial = emptyList())
    val currentUser by authRepository.currentUser.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }

    // Comments bottom sheet state
    var selectedActivityForComments by remember { mutableStateOf<ActivityEntity?>(null) }
    var activityToDelete by remember { mutableStateOf<ActivityEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val filteredActivities = remember(activities, selectedFilter) {
        if (selectedFilter == "ALL") activities
        else activities.filter { it.activityType.equals(selectedFilter, ignoreCase = true) }
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
            // Header Bar: Apple-inspired dark liquid glass
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GraphiteCharcoal)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StrideMarkIcon(
                        size = 38.dp,
                        tint = ElectricCyan
                    )
                    Column {
                        Text(
                            text = "STRIDE",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            letterSpacing = 2.sp,
                            color = OffWhite
                        )
                        Text(
                            text = "Welcome back, ${currentUser?.displayName?.ifBlank { "Athlete" } ?: "Athlete"}",
                            fontSize = 12.sp,
                            color = CoolGrey,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar badge in dark surface
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF252932))
                            .border(1.5.dp, ElectricCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (currentUser?.displayName?.take(1)?.uppercase() ?: "A"),
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Divider(color = Color(0xFF2E3340), thickness = 1.dp)

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                data class FilterItem(val type: String, val label: String, val icon: ImageVector?)
                val filters = listOf(
                    FilterItem("ALL", "All Feed", null),
                    FilterItem("RUN", "Runs", Icons.Default.DirectionsRun),
                    FilterItem("RIDE", "Rides", Icons.Default.DirectionsBike),
                    FilterItem("WALK", "Walks", Icons.Default.DirectionsWalk),
                    FilterItem("HIKE", "Hikes", Icons.Default.Hiking)
                )
                items(filters) { item ->
                    val isSelected = selectedFilter == item.type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = item.type },
                        leadingIcon = if (item.icon != null) {
                            {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) ObsidianBlack else ElectricCyan
                                )
                            }
                        } else null,
                        label = {
                            Text(
                                text = item.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan,
                            selectedLabelColor = ObsidianBlack,
                            containerColor = GraphiteCharcoal,
                            labelColor = CoolGrey
                        ),
                        border = if (isSelected) null else FilterChipDefaults.filterChipBorder(
                            borderColor = Color(0xFF2E3340),
                            borderWidth = 1.dp,
                            enabled = true,
                            selected = false
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("filter_${item.type}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Activity Cards Feed
            if (filteredActivities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = Color(0xFF2E3340),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No activities recorded yet",
                            fontWeight = FontWeight.Bold,
                            color = OffWhite,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Record your first workout using the center Record tab!",
                            color = CoolGrey,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredActivities, key = { it.id }) { activity ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400), initialOffsetY = { 40 }),
                            exit = fadeOut(animationSpec = tween(200))
                        ) {
                            ActivityFeedCard(
                                activity = activity,
                                onActivityClick = { onActivityClick(activity.id) },
                                onKudosClick = {
                                    scope.launch { strideRepository.toggleKudos(activity) }
                                },
                                onCommentClick = {
                                    selectedActivityForComments = activity
                                },
                                onShareClick = {
                                    strideRepository.shareActivity(context, activity)
                                },
                                onDeleteClick = if (activity.userId == currentUser?.uid || activity.userName == currentUser?.displayName) {
                                    { activityToDelete = activity }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // Comments Bottom Sheet
        if (selectedActivityForComments != null) {
            val act = selectedActivityForComments!!
            val commentsFlow = strideRepository.getComments(act.id)
            val comments by commentsFlow.collectAsState(initial = emptyList())
            var newCommentText by remember { mutableStateOf("") }

            ModalBottomSheet(
                onDismissRequest = { selectedActivityForComments = null },
                sheetState = sheetState,
                containerColor = GraphiteCharcoal,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Comments (${comments.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = OffWhite
                    )
                    Text(
                        text = act.title,
                        fontSize = 13.sp,
                        color = CoolGrey
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (comments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No comments yet. Be the first to cheer!",
                                color = CoolGrey,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(comments, key = { it.id }) { c ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x2600E5C7)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = c.userName.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricCyan,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = c.userName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = OffWhite
                                        )
                                        Text(
                                            text = c.text,
                                            fontSize = 14.sp,
                                            color = CoolGrey,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    if (c.userId == currentUser?.uid || act.userId == currentUser?.uid) {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    strideRepository.deleteComment(c.id, act.id)
                                                }
                                            },
                                            modifier = Modifier.size(28.dp).testTag("button_delete_comment_${c.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Comment",
                                                tint = CoolGrey.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Row for new comment
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { newCommentText = it },
                            placeholder = { Text("Write a comment...", color = CoolGrey) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_comment_text"),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = Color(0xFF2E3340),
                                focusedContainerColor = ObsidianBlack,
                                unfocusedContainerColor = ObsidianBlack,
                                focusedTextColor = OffWhite,
                                unfocusedTextColor = OffWhite,
                                cursorColor = ElectricCyan
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newCommentText.isNotBlank()) {
                                    val textToSend = newCommentText
                                    newCommentText = ""
                                    scope.launch {
                                        strideRepository.addComment(
                                            activityId = act.id,
                                            userId = currentUser?.uid ?: "athlete",
                                            userName = currentUser?.displayName?.ifBlank { "Athlete" } ?: "Athlete",
                                            userPhotoUrl = currentUser?.photoUrl,
                                            text = textToSend
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(ElectricCyan, CircleShape)
                                .testTag("button_submit_comment")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send Comment",
                                tint = ObsidianBlack,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (activityToDelete != null) {
            AlertDialog(
                onDismissRequest = { activityToDelete = null },
                title = { Text("Delete Workout Post?", color = OffWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "This will permanently delete this workout post, along with all associated comments and kudos, from your device and Cloud Firestore.",
                        color = CoolGrey,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val target = activityToDelete!!
                            activityToDelete = null
                            scope.launch {
                                strideRepository.deleteActivity(target.id, target.userId)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed, contentColor = OffWhite),
                        modifier = Modifier.testTag("button_confirm_delete_feed_post")
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activityToDelete = null }) {
                        Text("Cancel", color = CoolGrey)
                    }
                }
            )
        }
    }
}

@Composable
fun ActivityFeedCard(
    activity: ActivityEntity,
    onActivityClick: () -> Unit,
    onKudosClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit = {},
    onDeleteClick: (() -> Unit)? = null
) {
    val kudosScale by animateFloatAsState(
        targetValue = if (activity.isKudoedByMe) 1.25f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "kudos_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color(0x3300E5C7))
            .clickable(onClick = onActivityClick)
            .testTag("activity_card_${activity.id}"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF2E3340)),
        colors = CardDefaults.cardColors(containerColor = GraphiteCharcoal)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Athlete & Type Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gradient avatar matching Stryde theme: Cyan to Indigo
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ElectricCyan, IndigoViolet)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activity.userName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activity.userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = OffWhite
                    )
                    Text(
                        text = formatRelativeTime(activity.timestamp),
                        fontSize = 12.sp,
                        color = CoolGrey
                    )
                }

                // Activity Type Icon Badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF252932))
                        .border(1.dp, Color(0xFF2E3340), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getActivityIcon(activity.activityType),
                        contentDescription = activity.activityType,
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title & Description
            Text(
                text = activity.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = OffWhite
            )

            if (activity.description.isNotBlank()) {
                Text(
                    text = activity.description,
                    fontSize = 14.sp,
                    color = CoolGrey,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Map Polyline Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF2E3340), RoundedCornerShape(16.dp))
            ) {
                PolylineMapThumbnail(
                    polylineJson = activity.polylineJson,
                    modifier = Modifier.fillMaxSize(),
                    lineColor = ElectricCyan
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Column Stats Row in dark graphite card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF14171F), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x332E3340), RoundedCornerShape(16.dp))
                    .padding(vertical = 12.dp, horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Distance
                Column {
                    Text(
                        "DISTANCE",
                        fontSize = 10.sp,
                        color = CoolGrey,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = TrackingService.formatDistance(activity.distanceMeters),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = OffWhite
                    )
                }

                // Pace
                Column {
                    Text(
                        "PACE",
                        fontSize = 10.sp,
                        color = CoolGrey,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = TrackingService.formatPace(activity.avgPaceSecPerKm),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = OffWhite
                    )
                }

                // Time
                Column {
                    Text(
                        "TIME",
                        fontSize = 10.sp,
                        color = CoolGrey,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = TrackingService.formatTime(activity.durationSeconds),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = OffWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFF2E3340), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Social Action Buttons: Kudos (Mint Green) & Comments
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kudos Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onKudosClick)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("kudos_button_${activity.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (activity.isKudoedByMe) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Give Kudos",
                        tint = if (activity.isKudoedByMe) MintGreen else CoolGrey,
                        modifier = Modifier
                            .size(19.dp)
                            .scale(kudosScale)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${activity.kudosCount} Kudos",
                        fontSize = 13.sp,
                        fontWeight = if (activity.isKudoedByMe) FontWeight.Bold else FontWeight.Medium,
                        color = if (activity.isKudoedByMe) MintGreen else CoolGrey
                    )
                }

                // Comments Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onCommentClick)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("comment_button_${activity.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = CoolGrey,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${activity.commentsCount} Comments",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = CoolGrey
                    )
                }

                // Share Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onShareClick)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("share_button_${activity.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = CoolGrey,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = CoolGrey
                    )
                }

                if (onDeleteClick != null) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp).testTag("delete_button_${activity.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Workout",
                            tint = SoftCoral.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

fun getActivityIcon(type: String): ImageVector {
    return when (type.uppercase()) {
        "RUN" -> Icons.Default.DirectionsRun
        "RIDE" -> Icons.Default.DirectionsBike
        "WALK" -> Icons.Default.DirectionsWalk
        "HIKE" -> Icons.Default.Hiking
        else -> Icons.Default.DirectionsRun
    }
}

fun formatRelativeTime(timestamp: Long): String {
    val diffMs = System.currentTimeMillis() - timestamp
    val hours = diffMs / 3600000
    val minutes = diffMs / 60000
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        hours < 48 -> "Yesterday"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
