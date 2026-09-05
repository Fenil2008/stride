package com.example.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.StrideApp
import com.example.data.repository.AuthRepository
import com.example.data.repository.FirestoreStatus
import com.example.data.repository.StrideRepository
import com.example.service.TrackingService
import com.example.ui.common.WeeklyProgressChart
import com.example.ui.history.HistoryActivityRow
import com.example.ui.theme.CoolGrey
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GraphiteCharcoal
import com.example.ui.theme.IndigoViolet
import com.example.ui.theme.MintGreen
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.OffWhite
import com.example.ui.theme.SoftCoral
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    strideRepository: StrideRepository,
    onActivityClick: (String) -> Unit,
    onLogout: () -> Unit
) {
    val currentUser by authRepository.currentUser.collectAsState()
    val uid = currentUser?.uid ?: ""
    val userProfileFlow = remember(uid) { strideRepository.getUserProfile(uid) }
    val userProfile by userProfileFlow.collectAsState(initial = null)

    val activities by strideRepository.allActivities.collectAsState(initial = emptyList())
    // User's own activities
    val myActivities = remember(activities, uid) {
        activities.filter { it.userId == uid || it.userName == currentUser?.displayName }
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val app = context.applicationContext as StrideApp
    val firestoreManager = app.firestoreManager
    val connectionStatus by firestoreManager.status.collectAsState()

    var showCloudSyncDialog by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var isSyncingAll by remember { mutableStateOf(false) }
    var customDbIdInput by remember { mutableStateOf(firestoreManager.getDatabaseId()) }

    // Edit Profile form fields
    var editName by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var editUnit by remember { mutableStateOf("km") }
    var editPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            editPhotoUri = uri
        }
    }

    val totalDistanceMeters = remember(myActivities) { myActivities.sumOf { it.distanceMeters } }
    val totalSeconds = remember(myActivities) { myActivities.sumOf { it.durationSeconds } }
    val totalCalories = remember(myActivities) { myActivities.sumOf { it.calories } }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ObsidianBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top Bar with Settings/Logout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Athlete Profile",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = OffWhite
                )

                IconButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.testTag("button_profile_logout")
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Log Out",
                        tint = SoftCoral
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile Header Card in Graphite Charcoal
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color(0x3300E5C7)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF2E3340)),
                colors = CardDefaults.cardColors(containerColor = GraphiteCharcoal)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(ElectricCyan, IndigoViolet)
                                    )
                                )
                                .border(2.dp, ElectricCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!currentUser?.photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = currentUser?.photoUrl,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = (currentUser?.displayName?.take(1)?.uppercase() ?: "A"),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ObsidianBlack
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser?.displayName?.ifBlank { "Athlete" } ?: "Athlete",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = OffWhite
                            )
                            Text(
                                text = currentUser?.email ?: "athlete@stryde.app",
                                fontSize = 13.sp,
                                color = CoolGrey
                            )
                            Text(
                                text = "Preference: ${userProfile?.unitPreference?.uppercase() ?: "KM"}",
                                fontSize = 12.sp,
                                color = ElectricCyan,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Bio
                    val bioText = userProfile?.bio ?: "Crushing fitness goals daily with Stride"
                    if (bioText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = bioText,
                            fontSize = 14.sp,
                            color = CoolGrey,
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Edit Profile Button
                    OutlinedButton(
                        onClick = {
                            editName = currentUser?.displayName ?: ""
                            editBio = userProfile?.bio ?: ""
                            editUnit = userProfile?.unitPreference ?: "km"
                            editPhotoUri = null
                            showEditProfileDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("button_edit_profile"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E3340))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Profile & Preferences", fontWeight = FontWeight.Bold, color = OffWhite)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cloud Firestore Sync & Diagnostics
                    OutlinedButton(
                        onClick = {
                            showCloudSyncDialog = true
                            scope.launch {
                                firestoreManager.testConnectionAndInitCollections()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("button_cloud_sync_diagnostics"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E3340))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = when (connectionStatus) {
                                is FirestoreStatus.Connected -> MintGreen
                                is FirestoreStatus.DatastoreModeError -> SoftCoral
                                else -> ElectricCyan
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (connectionStatus) {
                                is FirestoreStatus.Connected -> "Cloud Firestore (Online)"
                                is FirestoreStatus.DatastoreModeError -> "Cloud Sync (Datastore Mode Alert)"
                                else -> "Cloud Firestore & Sync Diagnostics"
                            },
                            fontWeight = FontWeight.Bold,
                            color = OffWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Weekly Distance & Duration Progress Chart
            WeeklyProgressChart(
                activities = myActivities.ifEmpty { activities },
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Career Stats Banner
            Text(
                text = "Career Performance",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = OffWhite
            )

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(24.dp), spotColor = Color(0x3300E5C7)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF2E3340)),
                colors = CardDefaults.cardColors(containerColor = GraphiteCharcoal)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = ElectricCyan
                        )
                    }
                    Column {
                        Text(
                            "TIME",
                            fontSize = 10.sp,
                            color = CoolGrey,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = TrackingService.formatTime(totalSeconds),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = OffWhite
                        )
                    }
                    Column {
                        Text(
                            "WORKOUTS",
                            fontSize = 10.sp,
                            color = CoolGrey,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${myActivities.size}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = OffWhite
                        )
                    }
                    Column {
                        Text(
                            "CALORIES",
                            fontSize = 10.sp,
                            color = CoolGrey,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "$totalCalories",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = OffWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // My Recorded Activities
            Text(
                text = "My Activities (${myActivities.size})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = OffWhite
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (myActivities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No personal workouts recorded yet.", color = CoolGrey)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    myActivities.forEach { act ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400), initialOffsetY = { 30 }),
                            exit = fadeOut(animationSpec = tween(200))
                        ) {
                            HistoryActivityRow(
                                activity = act,
                                onClick = { onActivityClick(act.id) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(96.dp))
        }

        // Edit Profile Dialog
        if (showEditProfileDialog) {
            AlertDialog(
                onDismissRequest = { showEditProfileDialog = false },
                containerColor = GraphiteCharcoal,
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold, color = OffWhite) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        // Avatar picker
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .align(Alignment.CenterHorizontally)
                                .clip(CircleShape)
                                .background(Color(0xFF252932))
                                .border(2.dp, ElectricCyan, CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (editPhotoUri != null) {
                                AsyncImage(
                                    model = editPhotoUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = ElectricCyan)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Name", color = CoolGrey) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_edit_name"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = Color(0xFF2E3340),
                                focusedContainerColor = Color(0xFF252932),
                                unfocusedContainerColor = Color(0xFF252932),
                                focusedTextColor = OffWhite,
                                unfocusedTextColor = OffWhite,
                                cursorColor = ElectricCyan
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = editBio,
                            onValueChange = { editBio = it },
                            label = { Text("Bio", color = CoolGrey) },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth().testTag("input_edit_bio"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = Color(0xFF2E3340),
                                focusedContainerColor = Color(0xFF252932),
                                unfocusedContainerColor = Color(0xFF252932),
                                focusedTextColor = OffWhite,
                                unfocusedTextColor = OffWhite,
                                cursorColor = ElectricCyan
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Unit Preference", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OffWhite)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { editUnit = "km" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (editUnit == "km") ElectricCyan else Color(0xFF252932),
                                    contentColor = if (editUnit == "km") ObsidianBlack else OffWhite
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Kilometers", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { editUnit = "miles" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (editUnit == "miles") ElectricCyan else Color(0xFF252932),
                                    contentColor = if (editUnit == "miles") ObsidianBlack else OffWhite
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Miles", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                val finalName = editName.ifBlank { "Athlete" }
                                val finalPhoto = editPhotoUri?.toString() ?: currentUser?.photoUrl
                                authRepository.completeProfile(
                                    displayName = finalName,
                                    bio = editBio,
                                    unitPreference = editUnit,
                                    photoUri = finalPhoto
                                )
                                strideRepository.updateUserProfileAndActivities(
                                    uid = uid,
                                    displayName = finalName,
                                    bio = editBio,
                                    unitPreference = editUnit,
                                    photoUri = finalPhoto
                                )
                                showEditProfileDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = ObsidianBlack
                        ),
                        modifier = Modifier.testTag("button_save_edit_profile")
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditProfileDialog = false }) {
                        Text("Cancel", color = CoolGrey)
                    }
                }
            )
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                containerColor = GraphiteCharcoal,
                title = { Text("Log Out of Stryde?", fontWeight = FontWeight.Bold, color = OffWhite) },
                text = { Text("You will be returned to the authentication screen. Your local workout history will remain securely saved.", color = CoolGrey) },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            authRepository.logout()
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftCoral,
                            contentColor = ObsidianBlack
                        ),
                        modifier = Modifier.testTag("button_confirm_logout")
                    ) {
                        Text("Log Out", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel", color = CoolGrey)
                    }
                }
            )
        }

        // Cloud Firestore Sync & Diagnostics Dialog
        if (showCloudSyncDialog) {
            AlertDialog(
                onDismissRequest = { showCloudSyncDialog = false },
                containerColor = GraphiteCharcoal,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = ElectricCyan)
                        Text("Cloud Firestore Sync", fontWeight = FontWeight.Bold, color = OffWhite)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Project & Database info
                        Text(
                            text = "Project: stride-new-app",
                            fontSize = 12.sp,
                            color = CoolGrey,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Active Database: ${firestoreManager.getDatabaseId()}",
                            fontSize = 12.sp,
                            color = CoolGrey,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Live Connection Status Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (connectionStatus) {
                                    is FirestoreStatus.Connected -> Color(0x1F3DDC97)
                                    is FirestoreStatus.DatastoreModeError -> Color(0x2EFF6B6B)
                                    is FirestoreStatus.PermissionError -> Color(0x2EFF6B6B)
                                    is FirestoreStatus.Checking -> Color(0x1A00E5C7)
                                    else -> Color(0xFF252932)
                                }
                            ),
                            border = BorderStroke(
                                1.dp,
                                when (connectionStatus) {
                                    is FirestoreStatus.Connected -> MintGreen.copy(alpha = 0.5f)
                                    is FirestoreStatus.DatastoreModeError -> SoftCoral.copy(alpha = 0.6f)
                                    is FirestoreStatus.PermissionError -> SoftCoral.copy(alpha = 0.6f)
                                    else -> Color(0xFF2E3340)
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    when (connectionStatus) {
                                        is FirestoreStatus.Checking -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = ElectricCyan
                                            )
                                            Text("Testing connection...", fontSize = 13.sp, color = ElectricCyan, fontWeight = FontWeight.SemiBold)
                                        }
                                        is FirestoreStatus.Connected -> {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MintGreen, modifier = Modifier.size(18.dp))
                                            Text("Connected (Native Mode)", fontSize = 13.sp, color = MintGreen, fontWeight = FontWeight.Bold)
                                        }
                                        is FirestoreStatus.DatastoreModeError -> {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = SoftCoral, modifier = Modifier.size(18.dp))
                                            Text("Datastore / Enterprise Mode", fontSize = 13.sp, color = SoftCoral, fontWeight = FontWeight.Bold)
                                        }
                                        is FirestoreStatus.PermissionError -> {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = SoftCoral, modifier = Modifier.size(18.dp))
                                            Text("Rules Permission Denied", fontSize = 13.sp, color = SoftCoral, fontWeight = FontWeight.Bold)
                                        }
                                        is FirestoreStatus.GeneralError -> {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = SoftCoral, modifier = Modifier.size(18.dp))
                                            Text("Connection Error", fontSize = 13.sp, color = SoftCoral, fontWeight = FontWeight.Bold)
                                        }
                                        else -> {
                                            Text("Not connected yet", fontSize = 13.sp, color = CoolGrey)
                                        }
                                    }
                                }

                                val statusDesc = when (val s = connectionStatus) {
                                    is FirestoreStatus.Connected ->
                                        "Firestore Native mode is active. Real-time collections ('users', 'activities', 'comments') are connected and working."
                                    is FirestoreStatus.DatastoreModeError ->
                                        "Your GCP database is set to Datastore mode, preventing mobile Firestore SDK access. On Firebase (free plan):\n1. Go to console.firebase.google.com -> stride-new-app\n2. Open 'Firestore Database' -> Create Database in 'Firestore Native Mode'\n3. If you set a custom Database ID, enter it below."
                                    is FirestoreStatus.PermissionError ->
                                        "Security rules blocked the write. In Firebase Console -> Firestore -> Rules, ensure authenticated or read/write access is enabled."
                                    is FirestoreStatus.GeneralError -> s.message
                                    else -> "Tap 'Test Connection' to verify Firestore Native mode."
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = statusDesc,
                                    fontSize = 11.sp,
                                    color = OffWhite,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Database ID
                        Text("Database ID (leave '(default)' unless custom)", fontSize = 12.sp, color = CoolGrey)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customDbIdInput,
                                onValueChange = { customDbIdInput = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricCyan,
                                    unfocusedBorderColor = Color(0xFF2E3340),
                                    focusedContainerColor = Color(0xFF252932),
                                    unfocusedContainerColor = Color(0xFF252932),
                                    focusedTextColor = OffWhite,
                                    unfocusedTextColor = OffWhite
                                )
                            )
                            Button(
                                onClick = {
                                    val cleaned = customDbIdInput.trim()
                                    if (cleaned == "(default)" || cleaned.isBlank()) {
                                        firestoreManager.setDatabaseId("(default)")
                                    } else {
                                        firestoreManager.setDatabaseId(cleaned)
                                    }
                                    scope.launch {
                                        firestoreManager.testConnectionAndInitCollections()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = ObsidianBlack
                                )
                            ) {
                                Text("Set", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (syncMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(syncMessage!!, color = MintGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action 1: Test Connection & Init Collections
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val res = firestoreManager.testConnectionAndInitCollections()
                                    syncMessage = when (res) {
                                        is FirestoreStatus.Connected -> "Success! Connected and pinged collections in Firestore."
                                        is FirestoreStatus.DatastoreModeError -> "Database is in Datastore mode. Create a Native mode database in Firebase Console."
                                        is FirestoreStatus.PermissionError -> "Blocked by Firestore Rules: ${res.message}"
                                        is FirestoreStatus.GeneralError -> "Error: ${res.message}"
                                        else -> null
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Connection & Ping Collections", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action 2: Sync All Local Workouts
                        OutlinedButton(
                            onClick = {
                                isSyncingAll = true
                                scope.launch {
                                    val res = firestoreManager.syncAllLocalActivities(app.database.strideDao())
                                    isSyncingAll = false
                                    syncMessage = if (res.isSuccess) {
                                        "Uploaded ${res.getOrNull()} activities to Firestore 'activities' collection!"
                                    } else {
                                        "Sync failed: ${res.exceptionOrNull()?.message}"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFF2E3340))
                        ) {
                            if (isSyncingAll) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = OffWhite)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = OffWhite, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync All Local Workouts to Cloud", color = OffWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showCloudSyncDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = ObsidianBlack
                        )
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
