package com.example.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AutomatedEmailEntity
import com.example.service.EmailAutomationService
import com.example.ui.theme.CoolGrey
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
fun AutomatedEmailsBottomSheet(
    emailService: EmailAutomationService,
    onDismiss: () -> Unit,
    targetRecipientEmail: String? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val emails by emailService.allEmails.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedFilter by remember { mutableStateOf("ALL") }
    var viewingEmail by remember { mutableStateOf<AutomatedEmailEntity?>(null) }

    val filteredEmails = remember(emails, selectedFilter, targetRecipientEmail) {
        val baseList = if (targetRecipientEmail != null) {
            emails.filter { it.recipientEmail.equals(targetRecipientEmail, ignoreCase = true) }
        } else {
            emails
        }
        when (selectedFilter) {
            "WELCOME" -> baseList.filter { it.emailType == "WELCOME" }
            "SECURITY" -> baseList.filter { it.emailType in listOf("PASSWORD_RESET", "PASSWORD_CHANGED") }
            "LIVE_UPDATES" -> baseList.filter { it.emailType == "LIVE_UPDATE" }
            "WORKOUTS" -> baseList.filter { it.emailType == "WORKOUT_POSTED" }
            else -> baseList
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GraphiteCharcoal,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            if (viewingEmail != null) {
                EmailDetailView(
                    email = viewingEmail!!,
                    onBack = { viewingEmail = null },
                    onOpenInClient = {
                        emailService.launchEmailApp(
                            recipientEmail = viewingEmail!!.recipientEmail,
                            subject = viewingEmail!!.subject,
                            body = viewingEmail!!.bodyContent
                        )
                    },
                    onDelete = {
                        scope.launch {
                            emailService.deleteEmail(viewingEmail!!.id)
                            viewingEmail = null
                        }
                    }
                )
            } else {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Automated Email Inbox",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = OffWhite
                            )
                            Text(
                                text = "${emails.size} total automated dispatches",
                                fontSize = 12.sp,
                                color = CoolGrey
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_email_inbox")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = CoolGrey
                        )
                    }
                }

                // Filter Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "All Dispatches",
                        "WELCOME" to "Welcome",
                        "SECURITY" to "Password & Security",
                        "LIVE_UPDATES" to "Live Updates",
                        "WORKOUTS" to "Workouts"
                    )
                    items(filters) { (key, label) ->
                        val isSelected = selectedFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan,
                                selectedLabelColor = ObsidianBlack,
                                containerColor = Color(0xFF252932),
                                labelColor = CoolGrey
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) ElectricCyan else Color(0xFF2E3340),
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }
                }

                // Action Bar: Send Live Test Update
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF252932)),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2E3340)))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Live Platform Updates",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OffWhite
                            )
                            Text(
                                text = "Trigger real-time community milestone email",
                                fontSize = 11.sp,
                                color = CoolGrey
                            )
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val recipient = targetRecipientEmail ?: "athlete@stryde.app"
                                    emailService.sendLiveUpdateEmail(
                                        recipientEmail = recipient,
                                        recipientName = "Stryde Runner",
                                        updateTitle = "New Segments & AI Leaderboard Unlocked",
                                        headline = "3 new athletic segments have been mapped near your GPS coordinates!",
                                        details = "• Waterfront Sprint (1.2 km, Flat)\n• Pinecrest Incline (840 m, +8% Grade)\n• Sunset Loop (5.4 km, Rolling Hills)\n\nClimb the leaderboard and claim KOM/QOM honors this week!"
                                    )
                                    Toast.makeText(context, "Live update email sent!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = ObsidianBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("send_test_update_email")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = ObsidianBlack
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Live Update", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ObsidianBlack)
                        }
                    }
                }

                // Email List
                if (filteredEmails.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = CoolGrey,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No automated emails in this view",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = OffWhite
                            )
                            Text(
                                text = "Automated emails are sent on signup, password resets, and live workout saves.",
                                fontSize = 12.sp,
                                color = CoolGrey,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredEmails, key = { it.id }) { email ->
                            EmailItemCard(
                                email = email,
                                onClick = {
                                    scope.launch {
                                        emailService.markAsRead(email.id)
                                    }
                                    viewingEmail = email
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun EmailItemCard(
    email: AutomatedEmailEntity,
    onClick: () -> Unit
) {
    val icon = when (email.emailType) {
        "WELCOME" -> Icons.Default.VerifiedUser
        "PASSWORD_RESET" -> Icons.Default.Lock
        "PASSWORD_CHANGED" -> Icons.Default.Check
        "WORKOUT_POSTED" -> Icons.Default.FitnessCenter
        else -> Icons.Default.NotificationsActive
    }

    val iconColor = when (email.emailType) {
        "WELCOME" -> MintGreen
        "PASSWORD_RESET" -> ElectricCyan
        "PASSWORD_CHANGED" -> IndigoViolet
        "WORKOUT_POSTED" -> ElectricCyan
        else -> IndigoViolet
    }

    val dateFormatted = remember(email.sentTimestamp) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(email.sentTimestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("email_item_${email.id}"),
        colors = CardDefaults.cardColors(containerColor = if (email.isRead) Color(0xFF222630) else Color(0xFF282D38)),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (email.isRead) Color(0xFF2E3340) else ElectricCyan.copy(alpha = 0.4f))
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = email.senderName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OffWhite
                        )
                        Text(
                            text = "To: ${email.recipientEmail}",
                            fontSize = 11.sp,
                            color = CoolGrey
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (email.verificationCode != null) {
                        Surface(
                            color = ElectricCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "OTP: ${email.verificationCode}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = dateFormatted,
                        fontSize = 11.sp,
                        color = CoolGrey
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = email.subject,
                fontSize = 13.sp,
                fontWeight = if (!email.isRead) FontWeight.Bold else FontWeight.Medium,
                color = OffWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = email.previewText,
                fontSize = 12.sp,
                color = CoolGrey,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmailDetailView(
    email: AutomatedEmailEntity,
    onBack: () -> Unit,
    onOpenInClient: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val dateFormatted = remember(email.sentTimestamp) {
        SimpleDateFormat("EEE, MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(email.sentTimestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Top Back & Actions Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("email_back_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OffWhite
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onOpenInClient,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("open_in_external_email")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = ElectricCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Email App", fontSize = 12.sp, color = ElectricCyan)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_email_button")) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = SoftCoral
                    )
                }
            }
        }

        // Letterhead Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF252932)),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2E3340)))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = email.subject,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = OffWhite
                )
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFF2E3340))
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(
                            text = "From: ${email.senderName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OffWhite
                        )
                        Text(
                            text = "<${email.senderAddress}>",
                            fontSize = 11.sp,
                            color = CoolGrey
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "To: ${email.recipientName} <${email.recipientEmail}>",
                            fontSize = 12.sp,
                            color = CoolGrey
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            color = MintGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "● ${email.status}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MintGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dateFormatted,
                            fontSize = 10.sp,
                            color = CoolGrey
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Prominent OTP Code Card if this is a Password Reset Email
        if (email.verificationCode != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = ElectricCyan.copy(alpha = 0.10f)),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ElectricCyan.copy(alpha = 0.35f)))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PASSWORD RESET VERIFICATION CODE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = email.verificationCode,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OffWhite,
                        letterSpacing = 4.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            clipboardManager.setPrimaryClip(
                                ClipData.newPlainText("Stryde Reset Code", email.verificationCode)
                            )
                            Toast.makeText(context, "Code ${email.verificationCode} copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = ObsidianBlack
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("copy_reset_code_button")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = ObsidianBlack)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Code", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ObsidianBlack)
                    }
                }
            }
        }

        // Email Body Content
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF252932)),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2E3340)))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = email.bodyContent,
                    fontSize = 13.sp,
                    color = OffWhite,
                    lineHeight = 20.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
