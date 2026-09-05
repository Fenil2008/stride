package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.local.StrideDatabase
import com.example.data.repository.AuthRepository
import com.example.data.repository.StrideRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import java.io.File

class StrideApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { StrideDatabase.getDatabase(this) }
    val firestoreManager by lazy { com.example.data.repository.FirestoreManager(this) }
    val emailAutomationService by lazy { com.example.service.EmailAutomationService(this, database.strideDao(), applicationScope) }
    val strideRepository by lazy { StrideRepository(database.strideDao(), applicationScope, firestoreManager) }
    val authRepository by lazy { AuthRepository(this, database.strideDao(), emailAutomationService, firestoreManager) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Configure osmdroid tile caching & user agent
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = packageName
        val basePath = File(cacheDir, "osmdroid")
        osmConfig.osmdroidBasePath = basePath
        osmConfig.osmdroidTileCache = File(basePath, "tiles")

        // Create tracking notification channel
        createNotificationChannel()

        // Real Firebase & Room data sync (no mock seed data)
        applicationScope.launch {
            strideRepository.purgeMockData()
            strideRepository.startRealtimeActivitiesSync(null)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_TRACKING,
                "Stride Workout Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live pace, distance, and duration during workout recording"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID_TRACKING = "stride_location_tracking"
        lateinit var instance: StrideApp
            private set
    }
}
