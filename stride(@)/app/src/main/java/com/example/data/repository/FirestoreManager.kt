package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.local.StrideDao
import com.example.data.model.ActivityEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class FirestoreStatus {
    object Idle : FirestoreStatus()
    object Checking : FirestoreStatus()
    data class Connected(val databaseId: String, val message: String) : FirestoreStatus()
    data class DatastoreModeError(val databaseId: String, val message: String) : FirestoreStatus()
    data class PermissionError(val message: String) : FirestoreStatus()
    data class GeneralError(val message: String) : FirestoreStatus()
}

class FirestoreManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("stride_firestore_prefs", Context.MODE_PRIVATE)

    private val _status = MutableStateFlow<FirestoreStatus>(FirestoreStatus.Idle)
    val status: StateFlow<FirestoreStatus> = _status.asStateFlow()

    fun getDatabaseId(): String {
        return prefs.getString("database_id", "(default)") ?: "(default)"
    }

    fun setDatabaseId(dbId: String) {
        val clean = dbId.trim().ifBlank { "(default)" }
        prefs.edit().putString("database_id", clean).apply()
    }

    fun getFirestore(): FirebaseFirestore {
        val dbId = getDatabaseId()
        return try {
            val app = FirebaseApp.getInstance()
            if (dbId == "(default)" || dbId.isBlank()) {
                FirebaseFirestore.getInstance(app)
            } else {
                FirebaseFirestore.getInstance(app, dbId)
            }
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Error instantiating Firestore for database '$dbId'", e)
            FirebaseFirestore.getInstance()
        }
    }

    /**
     * Runs a comprehensive health-check test against the Firestore database.
     * Checks if the database is in Datastore mode, checks security rules,
     * signs in anonymously if needed, and writes an initial health ping document
     * to immediately instantiate the collections in Google Cloud Console.
     */
    suspend fun testConnectionAndInitCollections(): FirestoreStatus = withContext(Dispatchers.IO) {
        _status.value = FirestoreStatus.Checking
        val dbId = getDatabaseId()

        // 1. Ensure Firebase Auth has a session (anonymous if none) so security rules don't block
        try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
                Log.d("FirestoreManager", "Signed in anonymously for Firestore operations")
            }
        } catch (authEx: Exception) {
            Log.w("FirestoreManager", "Anonymous auth notice: ${authEx.message}")
        }

        try {
            val firestore = getFirestore()

            // 2. Perform a test write and read on the database
            val testDocRef = firestore.collection("_system_health").document("connection_test")
            val testData = mapOf(
                "status" to "ONLINE",
                "databaseId" to dbId,
                "timestamp" to System.currentTimeMillis(),
                "device" to "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                "appVersion" to "1.0.0"
            )
            testDocRef.set(testData).await()

            // 3. Read it back to verify full read/write capability
            val snapshot = testDocRef.get().await()
            if (!snapshot.exists()) {
                val err = FirestoreStatus.GeneralError("Test document was written but could not be read back.")
                _status.value = err
                return@withContext err
            }

            // 4. Create an initial document in users collection if missing
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "athlete_welcome"
            val userPing = mapOf(
                "uid" to currentUid,
                "status" to "ACTIVE",
                "initializedAt" to System.currentTimeMillis(),
                "databaseMode" to "Firestore Native"
            )
            firestore.collection("users").document(currentUid)
                .set(userPing, SetOptions.merge())
                .await()

            val success = FirestoreStatus.Connected(
                databaseId = dbId,
                message = "Successfully connected to Firestore Native! Collections are active."
            )
            _status.value = success
            return@withContext success

        } catch (e: Exception) {
            Log.e("FirestoreManager", "Firestore connection test failed", e)
            val msg = e.message ?: e.toString()

            val status = when {
                msg.contains("Datastore mode", ignoreCase = true) ||
                msg.contains("FAILED_PRECONDITION", ignoreCase = true) -> {
                    FirestoreStatus.DatastoreModeError(
                        databaseId = dbId,
                        message = "Your Firestore database is in 'Datastore mode' (Enterprise mode). Mobile apps require 'Firestore Native mode'. In Google Cloud Console or Firebase Console, create a database in Firestore Native mode or specify your Native database ID."
                    )
                }
                msg.contains("PERMISSION_DENIED", ignoreCase = true) -> {
                    FirestoreStatus.PermissionError(
                        message = "Firestore Security Rules are blocking writes. In Firebase Console -> Firestore Database -> Rules, allow reads and writes (e.g. 'allow read, write: if true;' for development, or 'allow read, write: if request.auth != null;')."
                    )
                }
                else -> {
                    FirestoreStatus.GeneralError(msg)
                }
            }
            _status.value = status
            return@withContext status
        }
    }

    /**
     * Uploads all local Room activities into Firestore so the 'activities' collection
     * is populated immediately in the Firebase / Google Cloud Console.
     */
    suspend fun syncAllLocalActivities(strideDao: StrideDao): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore()
            val activities = strideDao.getAllActivitiesSync()
            if (activities.isEmpty()) {
                return@withContext Result.success(0)
            }

            var syncedCount = 0
            for (act in activities) {
                val map = hashMapOf(
                    "id" to act.id,
                    "userId" to act.userId,
                    "userName" to act.userName,
                    "userPhotoUrl" to act.userPhotoUrl,
                    "activityType" to act.activityType,
                    "title" to act.title,
                    "description" to act.description,
                    "distanceMeters" to act.distanceMeters,
                    "durationSeconds" to act.durationSeconds,
                    "avgPaceSecPerKm" to act.avgPaceSecPerKm,
                    "avgSpeedKmh" to act.avgSpeedKmh,
                    "elevationGainMeters" to act.elevationGainMeters,
                    "calories" to act.calories,
                    "polylineJson" to act.polylineJson,
                    "timestamp" to act.timestamp,
                    "kudosCount" to act.kudosCount,
                    "commentsCount" to act.commentsCount,
                    "isPrivate" to act.isPrivate
                )
                firestore.collection("activities").document(act.id).set(map).await()
                syncedCount++
            }
            Result.success(syncedCount)
        } catch (e: Exception) {
            Log.e("FirestoreManager", "Failed to sync local activities to Firestore", e)
            Result.failure(e)
        }
    }
}
