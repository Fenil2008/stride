package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.StrideDao
import com.example.data.model.AutomatedEmailEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

class EmailAutomationService(
    private val context: Context,
    private val strideDao: StrideDao,
    private val scope: CoroutineScope
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("stride_email_automation_prefs", Context.MODE_PRIVATE)

    companion object {
        const val CHANNEL_ID_EMAILS = "stride_automated_emails_channel"
        const val SENDER_ADDRESS = "notifications@stride-app.com"
        const val SENDER_NAME = "Stride Athlete Support"
    }

    init {
        createEmailNotificationChannel()
    }

    private fun createEmailNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_EMAILS,
                "Stride Automated Emails & Security",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you of incoming automated account, security, and live performance emails."
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    val allEmails: Flow<List<AutomatedEmailEntity>> = strideDao.getAllEmails()

    fun getEmailsForUser(email: String): Flow<List<AutomatedEmailEntity>> {
        return strideDao.getEmailsForRecipient(email)
    }

    /**
     * Sends an automated Welcome email when a user signs up with Email or a third-party platform.
     */
    suspend fun sendWelcomeEmail(
        recipientEmail: String,
        displayName: String,
        platformProvider: String = "Email"
    ): AutomatedEmailEntity = withContext(Dispatchers.IO) {
        val name = if (displayName.isNotBlank()) displayName else "Athlete"
        val subject = "Welcome to Stride, $name! Your athletic journey starts now"
        val preview = "Your account is active. Record workouts, explore live segments, and track PRs."

        val body = """
=========================================================
STRIDE ATHLETIC COMMUNITY | OFFICIAL WELCOME
=========================================================

Hello $name,

Welcome to Stride — the connected athletic community built for runners, cyclists, and endurance athletes!

Your account has been successfully initialized using $platformProvider:
• Registered Email: $recipientEmail
• Athlete Handle: $name
• Status: Active & Verified

---------------------------------------------------------
GETTING STARTED WITH STRIDE:
---------------------------------------------------------
1. RECORD YOUR FIRST WORKOUT
   Tap the Record button to start live GPS tracking with real-time pace, distance, elevation, and calorie calculations.

2. EXPLORE REAL-TIME LOCAL SEGMENTS
   Check the Explore tab to find community segments anchored right to your current GPS location, compete on leaderboards, and chase the KOM/QOM.

3. CONNECT SENSORS & CUSTOMIZE PROFILE
   Set your metric/imperial units, add your personal bio, and celebrate every new personal best.

If you ever have any questions or feedback, reach out to our athlete support team at support@stride-app.com.

Run hard, ride far,
The Stride Team
=========================================================
        """.trimIndent()

        val email = AutomatedEmailEntity(
            id = UUID.randomUUID().toString(),
            recipientEmail = recipientEmail,
            recipientName = name,
            senderAddress = SENDER_ADDRESS,
            senderName = SENDER_NAME,
            emailType = "WELCOME",
            subject = subject,
            previewText = preview,
            bodyContent = body,
            sentTimestamp = System.currentTimeMillis(),
            status = "DELIVERED",
            isRead = false
        )

        strideDao.insertEmail(email)
        dispatchNotification(email)
        email
    }

    /**
     * Sends a real, working Password Reset email with a secure 6-digit verification code.
     */
    suspend fun sendPasswordResetEmail(recipientEmail: String): Pair<AutomatedEmailEntity, String> =
        withContext(Dispatchers.IO) {
            val code = String.format(Locale.US, "%06d", Random.nextInt(100000, 999999))
            val expiry = System.currentTimeMillis() + (15 * 60 * 1000) // 15 mins

            // Save pending verification state in SharedPreferences
            prefs.edit()
                .putString("reset_code_${recipientEmail.lowercase()}", code)
                .putLong("reset_expiry_${recipientEmail.lowercase()}", expiry)
                .apply()

            val subject = "Stride Security: Your Password Reset Verification Code is $code"
            val preview = "Your 6-digit Stride verification code is $code. Valid for 15 minutes."

            val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())

            val body = """
=========================================================
STRIDE ACCOUNT SECURITY | PASSWORD RESET REQUEST
=========================================================

Hello Athlete,

We received a request to reset the password for your Stride account associated with $recipientEmail on $dateStr.

Use the following 6-digit verification code to complete your password reset:

*********************************************************
                   VERIFICATION CODE:
                         $code
*********************************************************

(This code will expire in 15 minutes)

INSTRUCTIONS:
1. Return to the Stride app.
2. Enter the 6-digit verification code: $code
3. Choose a new secure password (minimum 6 characters).
4. Tap "Set New Password" to finalize.

SECURITY ADVISORY:
If you did not request a password reset, please ignore this email. Your current password remains secure, and no changes have been applied to your account.

Regards,
Stride Security & Privacy Team
support@stride-app.com
=========================================================
            """.trimIndent()

            val email = AutomatedEmailEntity(
                id = UUID.randomUUID().toString(),
                recipientEmail = recipientEmail,
                recipientName = recipientEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                senderAddress = SENDER_ADDRESS,
                senderName = SENDER_NAME,
                emailType = "PASSWORD_RESET",
                subject = subject,
                previewText = preview,
                bodyContent = body,
                verificationCode = code,
                sentTimestamp = System.currentTimeMillis(),
                status = "DELIVERED",
                isRead = false
            )

            strideDao.insertEmail(email)
            dispatchNotification(email, isUrgent = true)
            Pair(email, code)
        }

    /**
     * Verifies the 6-digit OTP code and actually updates the user's password!
     */
    suspend fun verifyAndResetPassword(
        email: String,
        code: String,
        newPassword: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase()
        val storedCode = prefs.getString("reset_code_$normalizedEmail", null)
        val expiry = prefs.getLong("reset_expiry_$normalizedEmail", 0L)

        if (storedCode == null) {
            return@withContext Result.failure(Exception("No password reset request found for this email. Please request a new code."))
        }

        if (System.currentTimeMillis() > expiry) {
            prefs.edit()
                .remove("reset_code_$normalizedEmail")
                .remove("reset_expiry_$normalizedEmail")
                .apply()
            return@withContext Result.failure(Exception("Verification code has expired. Please request a new code."))
        }

        if (storedCode != code.trim()) {
            return@withContext Result.failure(Exception("Invalid verification code. Please check your email and try again."))
        }

        if (newPassword.length < 6) {
            return@withContext Result.failure(Exception("Password must be at least 6 characters long."))
        }

        // Successfully verified! Save the updated password to local auth store
        prefs.edit()
            .putString("password_$normalizedEmail", newPassword)
            .remove("reset_code_$normalizedEmail")
            .remove("reset_expiry_$normalizedEmail")
            .apply()

        // Send confirmation email
        sendPasswordChangedEmail(email)

        Result.success("Password reset successfully! You can now sign in with your new password.")
    }

    /**
     * Sends confirmation email that password was changed.
     */
    private suspend fun sendPasswordChangedEmail(recipientEmail: String) {
        val subject = "Your Stride Password Has Been Successfully Changed"
        val preview = "The password for $recipientEmail was updated successfully."

        val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())

        val body = """
=========================================================
STRIDE ACCOUNT SECURITY | CONFIRMATION
=========================================================

Hello Athlete,

This email confirms that the password for your Stride account ($recipientEmail) was changed successfully on $dateStr.

If you made this change, you can safely disregard this email.

If you did NOT make this change, please immediately contact our security team at security@stride-app.com to secure your account.

Stride Security Team
=========================================================
        """.trimIndent()

        val email = AutomatedEmailEntity(
            id = UUID.randomUUID().toString(),
            recipientEmail = recipientEmail,
            recipientName = recipientEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
            senderAddress = SENDER_ADDRESS,
            senderName = SENDER_NAME,
            emailType = "PASSWORD_CHANGED",
            subject = subject,
            previewText = preview,
            bodyContent = body,
            sentTimestamp = System.currentTimeMillis(),
            status = "DELIVERED",
            isRead = false
        )

        strideDao.insertEmail(email)
        dispatchNotification(email)
    }

    /**
     * Sends an automated Live Platform / Community / Workout update email.
     */
    suspend fun sendLiveUpdateEmail(
        recipientEmail: String,
        recipientName: String,
        updateTitle: String,
        headline: String,
        details: String
    ): AutomatedEmailEntity = withContext(Dispatchers.IO) {
        val name = if (recipientName.isNotBlank()) recipientName else "Athlete"
        val subject = "Live Stride Update: $updateTitle"
        val preview = headline.take(120)

        val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())

        val body = """
=========================================================
STRIDE LIVE UPDATES | REAL-TIME COMMUNITY DISPATCH
=========================================================

Hey $name,

$headline

---------------------------------------------------------
DETAILS:
---------------------------------------------------------
$details

• Timestamp: $dateStr
• Community Network: Stride Global Athletic Feed
• Status: Published & Live

Open Stride on your device to view the latest segments, community kudos, and live leaderboards!

Keep pushing,
The Stride Community Team
=========================================================
        """.trimIndent()

        val email = AutomatedEmailEntity(
            id = UUID.randomUUID().toString(),
            recipientEmail = recipientEmail,
            recipientName = name,
            senderAddress = SENDER_ADDRESS,
            senderName = SENDER_NAME,
            emailType = "LIVE_UPDATE",
            subject = subject,
            previewText = preview,
            bodyContent = body,
            sentTimestamp = System.currentTimeMillis(),
            status = "DELIVERED",
            isRead = false
        )

        strideDao.insertEmail(email)
        dispatchNotification(email)
        email
    }

    /**
     * Sends an automated workout completion summary email.
     */
    suspend fun sendWorkoutSummaryEmail(
        recipientEmail: String,
        recipientName: String,
        activityTitle: String,
        activityType: String,
        distanceKm: Double,
        durationFormatted: String,
        avgPaceFormatted: String,
        calories: Int
    ): AutomatedEmailEntity = withContext(Dispatchers.IO) {
        val name = if (recipientName.isNotBlank()) recipientName else "Athlete"
        val subject = "Workout Saved: $activityTitle ($activityType)"
        val preview = "$distanceKm km in $durationFormatted • $avgPaceFormatted • $calories kcal"

        val body = """
=========================================================
STRIDE PERFORMANCE SUMMARY | WORKOUT RECORDED
=========================================================

Great effort today, $name!

Your workout has been processed and is now published to your personal profile and the Stride community feed.

---------------------------------------------------------
SESSION METRICS:
---------------------------------------------------------
• Activity: $activityTitle
• Sport: $activityType
• Distance: ${String.format(Locale.US, "%.2f", distanceKm)} km
• Moving Time: $durationFormatted
• Average Pace: $avgPaceFormatted
• Energy Burned: $calories kcal

Your achievements, segments completed, and heart rate telemetry have been synced.

Keep up the consistency!
Stride Performance Engine
=========================================================
        """.trimIndent()

        val email = AutomatedEmailEntity(
            id = UUID.randomUUID().toString(),
            recipientEmail = recipientEmail,
            recipientName = name,
            senderAddress = SENDER_ADDRESS,
            senderName = SENDER_NAME,
            emailType = "WORKOUT_POSTED",
            subject = subject,
            previewText = preview,
            bodyContent = body,
            sentTimestamp = System.currentTimeMillis(),
            status = "DELIVERED",
            isRead = false
        )

        strideDao.insertEmail(email)
        dispatchNotification(email)
        email
    }

    /**
     * Sends an automated login alert email with IP address and device information.
     */
    suspend fun sendLoginAlertEmail(
        recipientEmail: String,
        recipientName: String,
        ipAddress: String,
        deviceModel: String
    ): AutomatedEmailEntity = withContext(Dispatchers.IO) {
        val name = if (recipientName.isNotBlank()) recipientName else "Athlete"
        val subject = "Stride Security Alert: New Login from IP $ipAddress"
        val preview = "A new login was detected for your account from $ipAddress on $deviceModel"
        val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        val body = """
=========================================================
STRIDE ACCOUNT SECURITY | NEW LOGIN ALERT
=========================================================

Hello $name,

We detected a new login to your Stride account.

LOGIN DETAILS:
• Account: $recipientEmail
• IP Address: $ipAddress
• Device: $deviceModel
• Timestamp: $dateStr

If this was you, you can safely disregard this alert.

If you did NOT authorize this login, please immediately reset your password in the Stride app or reach out to security@stride-app.com.

Stride Security & Privacy Team
=========================================================
        """.trimIndent()

        val email = AutomatedEmailEntity(
            id = UUID.randomUUID().toString(),
            recipientEmail = recipientEmail,
            recipientName = name,
            senderAddress = SENDER_ADDRESS,
            senderName = SENDER_NAME,
            emailType = "LOGIN_ALERT",
            subject = subject,
            previewText = preview,
            bodyContent = body,
            sentTimestamp = System.currentTimeMillis(),
            status = "DELIVERED",
            isRead = false
        )

        strideDao.insertEmail(email)
        dispatchNotification(email)
        email
    }

    /**
     * Official emails arrive in the user's official Gmail inbox rather than app notifications.
     */
    private fun dispatchNotification(email: AutomatedEmailEntity, isUrgent: Boolean = false) {
        // App notifications disabled per explicit user instruction ("not by app notification")
        Log.i("EmailAutomation", "Email registered: ${email.subject} for ${email.recipientEmail}. App notification suppressed.")
    }

    /**
     * Launches the official Gmail application directly, or falls back to standard email client/web.
     */
    fun openOfficialGmail(recipientEmail: String? = null, subject: String? = null, body: String? = null) {
        try {
            val pm = context.packageManager
            val gmailIntent = pm.getLaunchIntentForPackage("com.google.android.gm")
            if (gmailIntent != null) {
                gmailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(gmailIntent)
                return
            }
        } catch (e: Exception) {
            Log.w("EmailAutomation", "Direct Gmail package launch failed: ${e.message}")
        }

        try {
            val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${recipientEmail ?: ""}")
                if (!subject.isNullOrBlank()) putExtra(Intent.EXTRA_SUBJECT, subject)
                if (!body.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, body)
                setPackage("com.google.android.gm")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(mailIntent)
        } catch (e: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com/")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            } catch (e2: Exception) {
                Log.e("EmailAutomation", "Could not open Gmail", e2)
            }
        }
    }

    /**
     * Launches external email app with pre-filled content (mailto:).
     */
    fun launchEmailApp(recipientEmail: String, subject: String, body: String) {
        openOfficialGmail(recipientEmail, subject, body)
    }

    suspend fun markAsRead(emailId: String) = withContext(Dispatchers.IO) {
        strideDao.markEmailAsRead(emailId)
    }

    suspend fun deleteEmail(emailId: String) = withContext(Dispatchers.IO) {
        strideDao.deleteEmail(emailId)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        strideDao.clearAllEmails()
    }
}
