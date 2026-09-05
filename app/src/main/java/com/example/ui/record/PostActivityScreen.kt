package com.example.ui.record

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.StrideRepository
import com.example.service.TrackingService
import com.example.ui.common.PolylineMapThumbnail
import com.example.ui.theme.CoolGrey
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GraphiteCharcoal
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.OffWhite
import com.example.ui.theme.SoftCoral
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun PostActivityScreen(
    activityType: String,
    distanceMeters: Double,
    durationSeconds: Long,
    avgPaceSecPerKm: Double,
    calories: Int,
    polylineJson: String,
    strideRepository: StrideRepository,
    authRepository: AuthRepository,
    onSavedOrDiscarded: () -> Unit
) {
    val currentUser by authRepository.currentUser.collectAsState()

    val defaultTitle = remember(activityType) {
        val typeName = activityType.lowercase().replaceFirstChar { it.uppercase() }
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val timeOfDay = when {
            hour < 12 -> "Morning"
            hour < 17 -> "Afternoon"
            else -> "Evening"
        }
        "$timeOfDay $typeName"
    }

    var title by remember { mutableStateOf(defaultTitle) }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ObsidianBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Save Workout",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = OffWhite
                )
                TextButton(
                    onClick = { showDiscardDialog = true },
                    modifier = Modifier.testTag("button_discard_workout")
                ) {
                    Text("Discard", color = SoftCoral, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Map thumbnail of completed route
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color(0x3300E5C7))
            ) {
                PolylineMapThumbnail(
                    polylineJson = polylineJson,
                    modifier = Modifier.fillMaxSize(),
                    lineColor = ElectricCyan
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metrics Summary Card in Graphite Charcoal
            Card(
                modifier = Modifier
                    .fillMaxWidth()
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
                        Text("DISTANCE", fontSize = 10.sp, color = CoolGrey, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Text(
                            text = TrackingService.formatDistance(distanceMeters),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = ElectricCyan
                        )
                    }
                    Column {
                        Text("DURATION", fontSize = 10.sp, color = CoolGrey, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Text(
                            text = TrackingService.formatTime(durationSeconds),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = OffWhite
                        )
                    }
                    Column {
                        Text("PACE", fontSize = 10.sp, color = CoolGrey, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Text(
                            text = TrackingService.formatPace(avgPaceSecPerKm),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = OffWhite
                        )
                    }
                    Column {
                        Text("CALORIES", fontSize = 10.sp, color = CoolGrey, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Text(
                            text = "$calories",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = OffWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Workout Title", color = CoolGrey) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_activity_title"),
                shape = RoundedCornerShape(16.dp),
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

            Spacer(modifier = Modifier.height(14.dp))

            // Description / notes input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes / How did it feel?", color = CoolGrey) },
                placeholder = { Text("e.g. Great weather, pushed pace on the final sprint.", color = CoolGrey) },
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_activity_description"),
                shape = RoundedCornerShape(16.dp),
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

            Spacer(modifier = Modifier.height(20.dp))

            // Privacy Toggle
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
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPrivate) Icons.Default.Lock else Icons.Default.Public,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isPrivate) "Private Activity" else "Public to Feed",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = OffWhite
                            )
                            Text(
                                text = if (isPrivate) "Visible only to you" else "Visible to friends in the Stryde feed",
                                fontSize = 12.sp,
                                color = CoolGrey
                            )
                        }
                    }

                    Switch(
                        checked = !isPrivate,
                        onCheckedChange = { isPrivate = !it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBlack,
                            checkedTrackColor = ElectricCyan,
                            uncheckedThumbColor = CoolGrey,
                            uncheckedTrackColor = Color(0xFF252932)
                        ),
                        modifier = Modifier.testTag("switch_privacy")
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save Activity Button
            Button(
                onClick = {
                    isSaving = true
                    scope.launch {
                        val activity = ActivityEntity(
                            id = UUID.randomUUID().toString(),
                            userId = currentUser?.uid ?: "local_runner",
                            userName = currentUser?.displayName?.ifBlank { "Athlete" } ?: "Athlete",
                            userPhotoUrl = currentUser?.photoUrl,
                            activityType = activityType,
                            title = title.ifBlank { defaultTitle },
                            description = description,
                            distanceMeters = distanceMeters,
                            durationSeconds = durationSeconds,
                            avgPaceSecPerKm = avgPaceSecPerKm,
                            avgSpeedKmh = if (durationSeconds > 0) (distanceMeters / durationSeconds) * 3.6 else 0.0,
                            calories = calories,
                            polylineJson = polylineJson,
                            timestamp = System.currentTimeMillis(),
                            kudosCount = 0,
                            commentsCount = 0,
                            isKudoedByMe = false,
                            isPrivate = isPrivate
                        )
                        strideRepository.saveActivity(activity)
                        isSaving = false
                        onSavedOrDiscarded()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = ElectricCyan)
                    .testTag("button_save_activity"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = ObsidianBlack
                ),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = ObsidianBlack, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, tint = ObsidianBlack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Save Activity",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Discard Confirmation Dialog
        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                containerColor = GraphiteCharcoal,
                title = { Text("Discard Workout?", fontWeight = FontWeight.Bold, color = OffWhite) },
                text = { Text("Are you sure you want to discard this recorded workout? This cannot be undone.", color = CoolGrey) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDiscardDialog = false
                            onSavedOrDiscarded()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftCoral,
                            contentColor = ObsidianBlack
                        ),
                        modifier = Modifier.testTag("button_confirm_discard")
                    ) {
                        Text("Discard", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text("Keep Editing", color = CoolGrey)
                    }
                }
            )
        }
    }
}
