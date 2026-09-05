package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "automated_emails")
data class AutomatedEmailEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val recipientEmail: String,
    val recipientName: String = "Athlete",
    val senderAddress: String = "notifications@stride-app.com",
    val senderName: String = "Stride Athlete Support",
    val emailType: String, // "WELCOME", "PASSWORD_RESET", "PASSWORD_CHANGED", "LIVE_UPDATE", "WORKOUT_POSTED"
    val subject: String,
    val previewText: String,
    val bodyContent: String,
    val verificationCode: String? = null,
    val sentTimestamp: Long = System.currentTimeMillis(),
    val status: String = "DELIVERED", // "DELIVERED", "VERIFIED", "READ"
    val isRead: Boolean = false
)
