package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ActivityEntity
import com.example.data.model.CommentEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StrideDao {

    @Query("SELECT * FROM activities ORDER BY timestamp DESC")
    fun getAllActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities ORDER BY timestamp DESC")
    suspend fun getAllActivitiesSync(): List<ActivityEntity>

    @Query("SELECT * FROM activities WHERE id = :id LIMIT 1")
    fun getActivityById(id: String): Flow<ActivityEntity?>

    @Query("SELECT * FROM activities WHERE userId = :userId ORDER BY timestamp DESC")
    fun getUserActivities(userId: String): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE activityType = :type ORDER BY timestamp DESC")
    fun getActivitiesByType(type: String): Flow<List<ActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityEntity>)

    @Query("UPDATE activities SET kudosCount = :count, isKudoedByMe = :isKudoed WHERE id = :id")
    suspend fun updateKudos(id: String, count: Int, isKudoed: Boolean)

    @Query("UPDATE activities SET commentsCount = commentsCount + 1 WHERE id = :id")
    suspend fun incrementCommentsCount(id: String)

    @Query("SELECT * FROM comments WHERE activityId = :activityId ORDER BY timestamp ASC")
    fun getCommentsForActivity(activityId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("SELECT * FROM user_profile WHERE uid = :uid LIMIT 1")
    fun getUserProfile(uid: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE uid = :uid LIMIT 1")
    suspend fun getUserProfileSync(uid: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    @Query("SELECT COUNT(*) FROM activities")
    suspend fun getActivityCount(): Int

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivity(id: String)

    @Query("DELETE FROM comments WHERE activityId = :activityId")
    suspend fun deleteCommentsForActivity(activityId: String)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: String)

    @Query("UPDATE activities SET commentsCount = MAX(0, commentsCount - 1) WHERE id = :id")
    suspend fun decrementCommentsCount(id: String)

    @Query("DELETE FROM activities WHERE id LIKE 'sample_%' OR userId IN ('user_sarah_101', 'user_marcus_102', 'user_elena_103', 'user_david_104')")
    suspend fun deleteMockActivities()

    @Query("DELETE FROM comments WHERE id LIKE 'sample_%' OR userId IN ('user_sarah_101', 'user_marcus_102', 'user_elena_103', 'user_david_104')")
    suspend fun deleteMockComments()

    // Automated Email Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmail(email: com.example.data.model.AutomatedEmailEntity)

    @Query("SELECT * FROM automated_emails ORDER BY sentTimestamp DESC")
    fun getAllEmails(): Flow<List<com.example.data.model.AutomatedEmailEntity>>

    @Query("SELECT * FROM automated_emails WHERE recipientEmail = :email ORDER BY sentTimestamp DESC")
    fun getEmailsForRecipient(email: String): Flow<List<com.example.data.model.AutomatedEmailEntity>>

    @Query("SELECT * FROM automated_emails WHERE id = :id LIMIT 1")
    suspend fun getEmailById(id: String): com.example.data.model.AutomatedEmailEntity?

    @Query("UPDATE automated_emails SET isRead = 1, status = 'READ' WHERE id = :id")
    suspend fun markEmailAsRead(id: String)

    @Query("DELETE FROM automated_emails WHERE id = :id")
    suspend fun deleteEmail(id: String)

    @Query("DELETE FROM automated_emails")
    suspend fun clearAllEmails()
}
