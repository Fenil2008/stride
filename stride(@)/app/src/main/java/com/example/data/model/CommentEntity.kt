package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey
    val id: String,
    val activityId: String,
    val userId: String,
    val userName: String,
    val userPhotoUrl: String? = null,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
