package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ActivityEntity
import com.example.data.model.AutomatedEmailEntity
import com.example.data.model.CommentEntity
import com.example.data.model.UserProfileEntity

@Database(
    entities = [
        ActivityEntity::class,
        CommentEntity::class,
        UserProfileEntity::class,
        AutomatedEmailEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class StrideDatabase : RoomDatabase() {

    abstract fun strideDao(): StrideDao

    companion object {
        @Volatile
        private var INSTANCE: StrideDatabase? = null

        fun getDatabase(context: Context): StrideDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StrideDatabase::class.java,
                    "stride_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
