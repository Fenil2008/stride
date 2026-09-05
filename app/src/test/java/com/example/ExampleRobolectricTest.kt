package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.StrideDatabase
import com.example.data.model.ActivityEntity
import com.example.data.model.CommentEntity
import com.example.data.repository.StrideRepository
import com.example.service.TrackingService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: StrideDatabase
    private lateinit var repository: StrideRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, StrideDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = StrideRepository(db.strideDao(), kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAppName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Stryde", appName)
    }

    @Test
    fun testTrackingServiceFormatting() {
        assertEquals("05:00", TrackingService.formatTime(300))
        assertEquals("1:05:20", TrackingService.formatTime(3920))
        assertEquals("5.00 km", TrackingService.formatDistance(5000.0, isImperial = false))
        assertEquals("3.11 mi", TrackingService.formatDistance(5000.0, isImperial = true))
        assertEquals("5:00 /km", TrackingService.formatPace(300.0, isImperial = false))
    }

    @Test
    fun testRoomDatabaseActivityInsertionAndKudos() = runBlocking {
        val act = ActivityEntity(
            id = UUID.randomUUID().toString(),
            userId = "user_123",
            userName = "Alex Runner",
            activityType = "RUN",
            title = "Morning 5K",
            distanceMeters = 5000.0,
            durationSeconds = 1500,
            avgPaceSecPerKm = 300.0,
            avgSpeedKmh = 12.0,
            polylineJson = """[{"lat":37.77,"lng":-122.41}]""",
            kudosCount = 5,
            isKudoedByMe = false
        )

        db.strideDao().insertActivity(act)
        val activities = db.strideDao().getAllActivities().first()
        assertEquals(1, activities.size)
        assertEquals("Morning 5K", activities[0].title)

        // Toggle Kudos
        repository.toggleKudos(activities[0])
        val updated = db.strideDao().getActivityById(act.id).first()
        assertNotNull(updated)
        assertEquals(6, updated!!.kudosCount)
        assertTrue(updated.isKudoedByMe)
    }

    @Test
    fun testRoomDatabaseComments() = runBlocking {
        val actId = "test_activity_456"
        val comment = CommentEntity(
            id = "c_1",
            activityId = actId,
            userId = "user_789",
            userName = "Jordan",
            text = "Great run!",
            timestamp = System.currentTimeMillis()
        )
        db.strideDao().insertComment(comment)
        val comments = db.strideDao().getCommentsForActivity(actId).first()
        assertEquals(1, comments.size)
        assertEquals("Great run!", comments[0].text)
    }
}

