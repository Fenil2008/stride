package com.example.data.repository

import android.net.Uri
import android.util.Log
import com.example.data.local.StrideDao
import com.example.data.model.ActivityEntity
import com.example.data.model.CommentEntity
import com.example.data.model.UserProfileEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class StrideRepository(
    private val strideDao: StrideDao,
    private val scope: CoroutineScope,
    private val firestoreManager: FirestoreManager? = null
) {
    val allActivities: Flow<List<ActivityEntity>> = strideDao.getAllActivities()

    private var activitiesListener: ListenerRegistration? = null
    private val commentsListeners = mutableMapOf<String, ListenerRegistration>()

    private fun getFirestore(): FirebaseFirestore =
        firestoreManager?.getFirestore() ?: FirebaseFirestore.getInstance()

    init {
        // Automatically purge any stale mock sample data from local storage
        scope.launch(Dispatchers.IO) {
            purgeMockData()
        }
    }

    fun getActivityById(id: String): Flow<ActivityEntity?> = strideDao.getActivityById(id)

    fun getUserActivities(userId: String): Flow<List<ActivityEntity>> = strideDao.getUserActivities(userId)

    fun getComments(activityId: String): Flow<List<CommentEntity>> = strideDao.getCommentsForActivity(activityId)

    fun getUserProfile(uid: String): Flow<UserProfileEntity?> = strideDao.getUserProfile(uid)

    suspend fun saveActivity(activity: ActivityEntity) {
        strideDao.insertActivity(activity)
        syncActivityToFirestore(activity)
    }

    suspend fun toggleKudos(
        activity: ActivityEntity,
        currentUserId: String = "runner",
        currentUserName: String = "Athlete"
    ) {
        val newIsKudoed = !activity.isKudoedByMe
        val newCount = if (newIsKudoed) activity.kudosCount + 1 else maxOf(0, activity.kudosCount - 1)
        strideDao.updateKudos(activity.id, newCount, newIsKudoed)

        scope.launch(Dispatchers.IO) {
            try {
                val firestore = getFirestore()
                val kudoDoc = firestore.collection("activities").document(activity.id)
                    .collection("kudos").document(currentUserId)
                val actDoc = firestore.collection("activities").document(activity.id)

                if (newIsKudoed) {
                    val kudoData = mapOf(
                        "uid" to currentUserId,
                        "userName" to currentUserName,
                        "timestamp" to System.currentTimeMillis()
                    )
                    kudoDoc.set(kudoData).await()
                    actDoc.update("kudosCount", FieldValue.increment(1)).await()
                } else {
                    kudoDoc.delete().await()
                    actDoc.update("kudosCount", FieldValue.increment(-1)).await()
                }
            } catch (e: Exception) {
                Log.w("StrideRepo", "Firestore kudos sync: ${e.message}")
            }
        }
    }

    suspend fun addComment(activityId: String, userId: String, userName: String, userPhotoUrl: String?, text: String) {
        val comment = CommentEntity(
            id = UUID.randomUUID().toString(),
            activityId = activityId,
            userId = userId,
            userName = userName,
            userPhotoUrl = userPhotoUrl,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        strideDao.insertComment(comment)
        strideDao.incrementCommentsCount(activityId)

        scope.launch(Dispatchers.IO) {
            try {
                val firestore = getFirestore()
                val commentMap = hashMapOf(
                    "id" to comment.id,
                    "activityId" to comment.activityId,
                    "userId" to comment.userId,
                    "userName" to comment.userName,
                    "userPhotoUrl" to comment.userPhotoUrl,
                    "text" to comment.text,
                    "timestamp" to comment.timestamp
                )
                firestore.collection("activities").document(activityId)
                    .collection("comments").document(comment.id)
                    .set(commentMap)
                    .await()
                firestore.collection("activities").document(activityId)
                    .update("commentsCount", FieldValue.increment(1))
                    .await()
            } catch (e: Exception) {
                Log.w("StrideRepo", "Firestore comment sync: ${e.message}")
            }
        }
    }

    suspend fun saveUserProfile(profile: UserProfileEntity) {
        strideDao.insertUserProfile(profile)
        scope.launch(Dispatchers.IO) {
            try {
                val firestore = getFirestore()
                val profileMap = hashMapOf(
                    "uid" to profile.uid,
                    "displayName" to profile.displayName,
                    "email" to profile.email,
                    "photoUri" to profile.photoUri,
                    "bio" to profile.bio,
                    "unitPreference" to profile.unitPreference,
                    "totalActivities" to profile.totalActivities,
                    "totalDistanceMeters" to profile.totalDistanceMeters,
                    "totalDurationSeconds" to profile.totalDurationSeconds
                )
                firestore.collection("users").document(profile.uid)
                    .set(profileMap)
                    .await()
            } catch (e: Exception) {
                Log.w("StrideRepo", "Firestore profile sync: ${e.message}")
            }
        }
    }

    suspend fun uploadProfilePhoto(uri: Uri, uid: String): String? = withContext(Dispatchers.IO) {
        try {
            val storage = FirebaseStorage.getInstance()
            val ref = storage.reference.child("users/$uid/profile_${System.currentTimeMillis()}.jpg")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.w("StrideRepo", "Storage photo upload: ${e.message}")
            null
        }
    }

    suspend fun followUser(currentUid: String, targetUid: String, targetName: String) = withContext(Dispatchers.IO) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val followData = mapOf(
                "followedUid" to targetUid,
                "followedName" to targetName,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("users").document(currentUid)
                .collection("following").document(targetUid)
                .set(followData)
                .await()
        } catch (e: Exception) {
            Log.w("StrideRepo", "Follow user error: ${e.message}")
        }
    }

    suspend fun unfollowUser(currentUid: String, targetUid: String) = withContext(Dispatchers.IO) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("users").document(currentUid)
                .collection("following").document(targetUid)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.w("StrideRepo", "Unfollow user error: ${e.message}")
        }
    }

    private fun syncActivityToFirestore(activity: ActivityEntity) {
        scope.launch(Dispatchers.IO) {
            try {
                val firestore = getFirestore()
                val map = hashMapOf(
                    "id" to activity.id,
                    "userId" to activity.userId,
                    "userName" to activity.userName,
                    "userPhotoUrl" to activity.userPhotoUrl,
                    "activityType" to activity.activityType,
                    "title" to activity.title,
                    "description" to activity.description,
                    "distanceMeters" to activity.distanceMeters,
                    "durationSeconds" to activity.durationSeconds,
                    "avgPaceSecPerKm" to activity.avgPaceSecPerKm,
                    "avgSpeedKmh" to activity.avgSpeedKmh,
                    "elevationGainMeters" to activity.elevationGainMeters,
                    "calories" to activity.calories,
                    "polylineJson" to activity.polylineJson,
                    "timestamp" to activity.timestamp,
                    "kudosCount" to activity.kudosCount,
                    "commentsCount" to activity.commentsCount,
                    "isPrivate" to activity.isPrivate
                )
                firestore.collection("activities").document(activity.id)
                    .set(map)
                    .await()
                Log.d("StrideRepo", "Activity written to Firestore activities/${activity.id}")

                // Update career totals and weekly progress in user's Firestore profile
                try {
                    val userRef = firestore.collection("users").document(activity.userId)
                    userRef.update(
                        "totalActivities", FieldValue.increment(1),
                        "totalDistanceMeters", FieldValue.increment(activity.distanceMeters),
                        "totalDurationSeconds", FieldValue.increment(activity.durationSeconds),
                        "lastActiveTimestamp", activity.timestamp
                    ).await()
                } catch (eu: Exception) {
                    // If user doc doesn't have fields yet, set merge
                    val userSummary = mapOf(
                        "uid" to activity.userId,
                        "lastActiveTimestamp" to activity.timestamp
                    )
                    firestore.collection("users").document(activity.userId)
                        .set(userSummary, com.google.firebase.firestore.SetOptions.merge())
                }
            } catch (e: Exception) {
                Log.w("StrideRepo", "Firestore activity sync: ${e.message}")
            }
        }
    }

    /**
     * Deletes an activity/post along with all associated comments and likes/kudos
     * from both the local Room database and remote Cloud Firestore.
     */
    suspend fun deleteActivity(activityId: String, userId: String? = null) {
        // 1. Delete locally from Room
        strideDao.deleteActivity(activityId)
        strideDao.deleteCommentsForActivity(activityId)

        // 2. Delete from Cloud Firestore (Document + Subcollections comments & kudos)
        scope.launch(Dispatchers.IO) {
            try {
                val firestore = getFirestore()
                val actRef = firestore.collection("activities").document(activityId)

                // Fetch and delete all comments in subcollection
                try {
                    val commentsSnapshot = actRef.collection("comments").get().await()
                    for (doc in commentsSnapshot.documents) {
                        doc.reference.delete().await()
                    }
                } catch (e: Exception) {
                    Log.w("StrideRepo", "Delete comments subcollection: ${e.message}")
                }

                // Fetch and delete all kudos in subcollection
                try {
                    val kudosSnapshot = actRef.collection("kudos").get().await()
                    for (doc in kudosSnapshot.documents) {
                        doc.reference.delete().await()
                    }
                } catch (e: Exception) {
                    Log.w("StrideRepo", "Delete kudos subcollection: ${e.message}")
                }

                // Delete the main activity document
                actRef.delete().await()
                Log.d("StrideRepo", "Successfully deleted activity $activityId from Firestore.")

                // Recalculate and update user's career totals in Firestore if userId is known
                if (!userId.isNullOrBlank()) {
                    try {
                        val remainingActivities = strideDao.getUserActivities(userId)
                        // Update user document
                        firestore.collection("users").document(userId).update(
                            "totalActivities", FieldValue.increment(-1)
                        ).await()
                    } catch (e: Exception) {
                        Log.w("StrideRepo", "Update user stats after activity deletion: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("StrideRepo", "Failed to delete activity from Firestore", e)
            }
        }
    }

    /**
     * Deletes a specific comment from Room and Cloud Firestore.
     */
    suspend fun deleteComment(commentId: String, activityId: String) {
        strideDao.deleteComment(commentId)
        strideDao.decrementCommentsCount(activityId)
        scope.launch(Dispatchers.IO) {
            try {
                val firestore = getFirestore()
                firestore.collection("activities").document(activityId)
                    .collection("comments").document(commentId)
                    .delete().await()
                firestore.collection("activities").document(activityId)
                    .update("commentsCount", FieldValue.increment(-1)).await()
            } catch (e: Exception) {
                Log.w("StrideRepo", "Firestore delete comment: ${e.message}")
            }
        }
    }

    /**
     * Shares activity via Android System Intent and updates share counter in Firestore.
     */
    fun shareActivity(context: android.content.Context, activity: ActivityEntity) {
        try {
            val distStr = com.example.service.TrackingService.formatDistance(activity.distanceMeters)
            val timeStr = com.example.service.TrackingService.formatTime(activity.durationSeconds)
            val paceStr = com.example.service.TrackingService.formatPace(activity.avgPaceSecPerKm)

            val shareText = """
🏃 Check out my ${activity.activityType.lowercase().replaceFirstChar { it.uppercase() }} on Stride!
Title: ${activity.title}
Distance: $distStr
Duration: $timeStr
Pace: $paceStr
Burned: ${activity.calories} kcal

Track your workouts and compete on community leaderboards with Stride!
            """.trimIndent()

            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Stride Workout: ${activity.title}")
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share workout via").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            })

            // Increment share count in Firestore
            scope.launch(Dispatchers.IO) {
                try {
                    getFirestore().collection("activities").document(activity.id)
                        .update("sharesCount", FieldValue.increment(1)).await()
                } catch (e: Exception) {
                    Log.w("StrideRepo", "Increment shares: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("StrideRepo", "Error sharing activity", e)
        }
    }

    /**
     * Updates user profile in Room and Cloud Firestore, and propagates updated
     * athlete name/photo to all of the user's past activities in Firestore.
     */
    suspend fun updateUserProfileAndActivities(
        uid: String,
        displayName: String,
        bio: String,
        unitPreference: String,
        photoUri: String?
    ) {
        val profile = UserProfileEntity(
            uid = uid,
            displayName = displayName,
            email = "",
            photoUri = photoUri,
            bio = bio,
            unitPreference = unitPreference
        )
        strideDao.insertUserProfile(profile)

        scope.launch(Dispatchers.IO) {
            try {
                val firestore = getFirestore()
                val profileMap = hashMapOf<String, Any?>(
                    "uid" to uid,
                    "displayName" to displayName,
                    "photoUri" to photoUri,
                    "bio" to bio,
                    "unitPreference" to unitPreference
                )
                firestore.collection("users").document(uid)
                    .set(profileMap, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                // Propagate updated name and photo across all user's published activities
                val userActivitiesQuery = firestore.collection("activities")
                    .whereEqualTo("userId", uid)
                    .get().await()

                for (doc in userActivitiesQuery.documents) {
                    doc.reference.update(
                        "userName", displayName,
                        "userPhotoUrl", photoUri
                    ).await()
                }
                Log.d("StrideRepo", "Updated profile and synced across user's activities in Firestore.")
            } catch (e: Exception) {
                Log.w("StrideRepo", "Update profile & activities in Firestore: ${e.message}")
            }
        }
    }

    fun startRealtimeActivitiesSync(currentUserId: String?) {
        try {
            activitiesListener?.remove()
            val firestore = getFirestore()
            activitiesListener = firestore.collection("activities")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener { snapshots, e ->
                    if (e != null || snapshots == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        // Handle deletions in real-time
                        for (change in snapshots.documentChanges) {
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                val removedId = change.document.id
                                strideDao.deleteActivity(removedId)
                                strideDao.deleteCommentsForActivity(removedId)
                            }
                        }

                        val list = snapshots.documents.mapNotNull { doc ->
                            doc.toActivityEntity()
                        }
                        if (list.isNotEmpty()) {
                            strideDao.insertActivities(list)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("StrideRepo", "Start realtime activities sync: ${e.message}")
        }
    }

    fun startRealtimeCommentsSync(activityId: String) {
        if (commentsListeners.containsKey(activityId)) return
        try {
            val firestore = getFirestore()
            val listener = firestore.collection("activities").document(activityId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshots, e ->
                    if (e != null || snapshots == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        val comments = snapshots.documents.mapNotNull { doc ->
                            val id = doc.getString("id") ?: doc.id
                            val uid = doc.getString("userId") ?: return@mapNotNull null
                            val name = doc.getString("userName") ?: "Athlete"
                            val photo = doc.getString("userPhotoUrl")
                            val text = doc.getString("text") ?: ""
                            val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            CommentEntity(id, activityId, uid, name, photo, text, ts)
                        }
                        if (comments.isNotEmpty()) {
                            comments.forEach { strideDao.insertComment(it) }
                        }
                    }
                }
            commentsListeners[activityId] = listener
        } catch (e: Exception) {
            Log.w("StrideRepo", "Comments listener error: ${e.message}")
        }
    }

    suspend fun purgeMockData() {
        try {
            strideDao.deleteMockActivities()
            strideDao.deleteMockComments()
            Log.d("StrideRepo", "Purged any legacy mock sample activities and comments.")
        } catch (e: Exception) {
            Log.w("StrideRepo", "Purge mock data: ${e.message}")
        }
    }
}

private fun DocumentSnapshot.toActivityEntity(): ActivityEntity? {
    val id = getString("id") ?: id
    val userId = getString("userId") ?: return null
    val userName = getString("userName") ?: "Athlete"
    val userPhotoUrl = getString("userPhotoUrl")
    val activityType = getString("activityType") ?: "RUN"
    val title = getString("title") ?: "Workout"
    val description = getString("description") ?: ""
    val distanceMeters = getDouble("distanceMeters") ?: 0.0
    val durationSeconds = getLong("durationSeconds") ?: 0L
    val avgPaceSecPerKm = getDouble("avgPaceSecPerKm") ?: 0.0
    val avgSpeedKmh = getDouble("avgSpeedKmh") ?: 0.0
    val elevationGainMeters = getDouble("elevationGainMeters") ?: 0.0
    val calories = getLong("calories")?.toInt() ?: 0
    val polylineJson = getString("polylineJson") ?: "[]"
    val timestamp = getLong("timestamp") ?: System.currentTimeMillis()
    val kudosCount = getLong("kudosCount")?.toInt() ?: 0
    val commentsCount = getLong("commentsCount")?.toInt() ?: 0
    val isPrivate = getBoolean("isPrivate") ?: false

    return ActivityEntity(
        id = id,
        userId = userId,
        userName = userName,
        userPhotoUrl = userPhotoUrl,
        activityType = activityType,
        title = title,
        description = description,
        distanceMeters = distanceMeters,
        durationSeconds = durationSeconds,
        avgPaceSecPerKm = avgPaceSecPerKm,
        avgSpeedKmh = avgSpeedKmh,
        elevationGainMeters = elevationGainMeters,
        calories = calories,
        polylineJson = polylineJson,
        timestamp = timestamp,
        kudosCount = kudosCount,
        commentsCount = commentsCount,
        isKudoedByMe = false,
        isPrivate = isPrivate,
        syncedToCloud = true
    )
}
