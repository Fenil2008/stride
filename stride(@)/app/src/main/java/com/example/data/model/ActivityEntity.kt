package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val userName: String,
    val userPhotoUrl: String? = null,
    val activityType: String, // RUN, RIDE, WALK, HIKE
    val title: String,
    val description: String = "",
    val distanceMeters: Double,
    val durationSeconds: Long,
    val avgPaceSecPerKm: Double, // in seconds per km (or converted for miles)
    val avgSpeedKmh: Double,
    val elevationGainMeters: Double = 0.0,
    val calories: Int = 0,
    val polylineJson: String = "[]",
    val timestamp: Long = System.currentTimeMillis(),
    val kudosCount: Int = 0,
    val commentsCount: Int = 0,
    val isKudoedByMe: Boolean = false,
    val isPrivate: Boolean = false,
    val syncedToCloud: Boolean = false
)
