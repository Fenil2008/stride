package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUri: String? = null,
    val bio: String = "",
    val unitPreference: String = "km", // "km" or "miles"
    val totalActivities: Int = 0,
    val totalDistanceMeters: Double = 0.0,
    val totalDurationSeconds: Long = 0L
)
