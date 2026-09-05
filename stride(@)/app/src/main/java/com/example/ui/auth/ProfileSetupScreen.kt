package com.example.ui.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.repository.AuthRepository
import com.example.ui.theme.CoolGrey
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GraphiteCharcoal
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.OffWhite
import kotlinx.coroutines.launch

@Composable
fun ProfileSetupScreen(
    authRepository: AuthRepository,
    onSetupComplete: () -> Unit
) {
    val currentUser = authRepository.currentUser.value
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var bio by remember { mutableStateOf("") }
    var unitPreference by remember { mutableStateOf("km") } // "km" or "miles"
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Android zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ObsidianBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Complete Your Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OffWhite
            )

            Text(
                text = "Personalize your athlete identity on Stryde",
                fontSize = 14.sp,
                color = CoolGrey,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Profile Photo Upload Avatar
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(GraphiteCharcoal)
                    .border(2.dp, ElectricCyan, CircleShape)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("avatar_picker"),
                contentAlignment = Alignment.Center
            ) {
                if (selectedPhotoUri != null) {
                    AsyncImage(
                        model = selectedPhotoUri,
                        contentDescription = "Selected Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Upload Photo",
                            tint = ElectricCyan,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            "Add Photo",
                            fontSize = 11.sp,
                            color = ElectricCyan,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Display Name
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CoolGrey) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_setup_name"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = Color(0xFF2E3340),
                    focusedContainerColor = GraphiteCharcoal,
                    unfocusedContainerColor = GraphiteCharcoal,
                    focusedTextColor = OffWhite,
                    unfocusedTextColor = OffWhite,
                    focusedLabelColor = ElectricCyan,
                    unfocusedLabelColor = CoolGrey
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bio / Goal
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio or Fitness Goal") },
                placeholder = { Text("e.g., Training for my first half marathon", color = CoolGrey) },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_setup_bio"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = Color(0xFF2E3340),
                    focusedContainerColor = GraphiteCharcoal,
                    unfocusedContainerColor = GraphiteCharcoal,
                    focusedTextColor = OffWhite,
                    unfocusedTextColor = OffWhite,
                    focusedLabelColor = ElectricCyan,
                    unfocusedLabelColor = CoolGrey
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Unit Preference Selection
            Text(
                text = "Preferred Units",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = OffWhite,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Kilometers
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (unitPreference == "km") Color(0x2600E5C7) else GraphiteCharcoal)
                        .border(
                            width = if (unitPreference == "km") 2.dp else 1.dp,
                            color = if (unitPreference == "km") ElectricCyan else Color(0xFF2E3340),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { unitPreference = "km" }
                        .testTag("unit_km"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (unitPreference == "km") {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                        }
                        Text(
                            text = "Kilometers (km)",
                            fontWeight = if (unitPreference == "km") FontWeight.Bold else FontWeight.Medium,
                            color = if (unitPreference == "km") ElectricCyan else OffWhite
                        )
                    }
                }

                // Miles
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (unitPreference == "miles") Color(0x2600E5C7) else GraphiteCharcoal)
                        .border(
                            width = if (unitPreference == "miles") 2.dp else 1.dp,
                            color = if (unitPreference == "miles") ElectricCyan else Color(0xFF2E3340),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { unitPreference = "miles" }
                        .testTag("unit_miles"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (unitPreference == "miles") {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                        }
                        Text(
                            text = "Miles (mi)",
                            fontWeight = if (unitPreference == "miles") FontWeight.Bold else FontWeight.Medium,
                            color = if (unitPreference == "miles") ElectricCyan else OffWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Save & Continue Button
            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        authRepository.completeProfile(
                            displayName = displayName.ifBlank { "Athlete" },
                            bio = bio,
                            unitPreference = unitPreference,
                            photoUri = selectedPhotoUri?.toString()
                        )
                        isLoading = false
                        onSetupComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("button_save_profile"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = ObsidianBlack
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = ObsidianBlack, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                } else {
                    Text(
                        "Get Started",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack
                    )
                }
            }
        }
    }
}
