package com.example.ui.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthRepository
import com.example.data.repository.StrideRepository
import com.example.service.TrackingService
import com.example.ui.common.PolylineMapThumbnail
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

@Composable
fun ActivityDetailScreen(
    activityId: String,
    strideRepository: StrideRepository,
    authRepository: AuthRepository,
    onBack: () -> Unit
) {
    val activityFlow = remember(activityId) { strideRepository.getActivityById(activityId) }
    val activity by activityFlow.collectAsState(initial = null)
    val commentsFlow = remember(activityId) { strideRepository.getComments(activityId) }
    val comments by commentsFlow.collectAsState(initial = emptyList())
    val currentUser by authRepository.currentUser.collectAsState()

    androidx.compose.runtime.LaunchedEffect(activityId) {
        strideRepository.startRealtimeCommentsSync(activityId)
    }

    var commentText by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ObsidianBlack
    ) {
        if (activity == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Activity not found", color = CoolGrey)
            }
            return@Surface
        }

        val act = activity!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("button_back")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = OffWhite
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${act.activityType.lowercase().replaceFirstChar { it.uppercase() }} Detail",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OffWhite
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { strideRepository.shareActivity(context, act) },
                    modifier = Modifier.testTag("button_share_activity")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Workout",
                        tint = ElectricCyan
                    )
                }
                if (act.userId == currentUser?.uid || act.userName == currentUser?.displayName) {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.testTag("button_delete_activity")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Workout",
                            tint = SoftCoral
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Athlete Profile Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(ElectricCyan, IndigoViolet)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = act.userName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = ObsidianBlack,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = act.userName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = OffWhite
                        )
                        Text(
                            text = formatRelativeTime(act.timestamp),
                            fontSize = 12.sp,
                            color = CoolGrey
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title & Description
                Text(
                    text = act.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = OffWhite
                )

                if (act.description.isNotBlank()) {
                    Text(
                        text = act.description,
                        fontSize = 14.sp,
                        color = CoolGrey,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Large Map Route View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color(0x3300E5C7))
                ) {
                    PolylineMapThumbnail(
                        polylineJson = act.polylineJson,
                        modifier = Modifier.fillMaxSize(),
                        lineColor = ElectricCyan
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Expanded Detailed Stats Grid (4 Cards)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Distance",
                        value = TrackingService.formatDistance(act.distanceMeters),
                        icon = Icons.Default.Terrain
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Time",
                        value = TrackingService.formatTime(act.durationSeconds),
                        icon = Icons.Default.Timer
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Avg Pace",
                        value = TrackingService.formatPace(act.avgPaceSecPerKm),
                        icon = Icons.Default.Speed
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Calories",
                        value = "${act.calories} kcal",
                        icon = Icons.Default.LocalFireDepartment
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Kudos Section in Graphite Charcoal
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color(0x3300E5C7)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF2E3340)),
                    colors = CardDefaults.cardColors(containerColor = GraphiteCharcoal)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (act.isKudoedByMe) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = null,
                                tint = if (act.isKudoedByMe) MintGreen else CoolGrey,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${act.kudosCount} Kudos",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = OffWhite
                                )
                                Text(
                                    text = if (act.isKudoedByMe) "You cheered this workout!" else "Cheer this workout",
                                    fontSize = 12.sp,
                                    color = CoolGrey
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        strideRepository.toggleKudos(
                                            act,
                                            currentUser?.uid ?: "user",
                                            currentUser?.displayName ?: "Athlete"
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (act.isKudoedByMe) Color(0x263DDC97) else ElectricCyan,
                                    contentColor = if (act.isKudoedByMe) MintGreen else ObsidianBlack
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("button_kudos_toggle")
                            ) {
                                Text(
                                    text = if (act.isKudoedByMe) "Liked" else "Give Kudos",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { strideRepository.shareActivity(context, act) },
                                modifier = Modifier.testTag("button_share_detail")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = ElectricCyan
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Comments Section
                Text(
                    text = "Comments (${comments.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = OffWhite
                )

                Spacer(modifier = Modifier.height(10.dp))

                comments.forEach { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(ElectricCyan, IndigoViolet)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = c.userName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = ObsidianBlack,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = c.userName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = OffWhite
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = formatRelativeTime(c.timestamp),
                                    fontSize = 11.sp,
                                    color = CoolGrey
                                )
                            }
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
                    Divider(color = Color(0xFF2E3340), thickness = 1.dp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Add Comment Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Add your comment...", color = CoolGrey) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_detail_comment"),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Color(0xFF2E3340),
                            focusedContainerColor = GraphiteCharcoal,
                            unfocusedContainerColor = GraphiteCharcoal,
                            focusedTextColor = OffWhite,
                            unfocusedTextColor = OffWhite,
                            cursorColor = ElectricCyan
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                val text = commentText
                                commentText = ""
                                scope.launch {
                                    strideRepository.addComment(
                                        activityId = act.id,
                                        userId = currentUser?.uid ?: "athlete",
                                        userName = currentUser?.displayName?.ifBlank { "Athlete" } ?: "Athlete",
                                        userPhotoUrl = currentUser?.photoUrl,
                                        text = text
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(ElectricCyan, CircleShape)
                            .testTag("button_detail_send_comment")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = {
                    Text("Delete Workout Post?", color = OffWhite, fontWeight = FontWeight.Bold)
                },
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
                            showDeleteDialog = false
                            scope.launch {
                                strideRepository.deleteActivity(act.id, act.userId)
                                onBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralRed,
                            contentColor = OffWhite
                        ),
                        modifier = Modifier.testTag("button_confirm_delete_post")
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = CoolGrey)
                    }
                }
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x2600E5C7)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF2E3340)),
        colors = CardDefaults.cardColors(containerColor = GraphiteCharcoal)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label.uppercase(),
                    fontSize = 10.sp,
                    color = CoolGrey,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = OffWhite
            )
        }
    }
}
