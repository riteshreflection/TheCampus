package com.reflection.thecampus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.reflection.thecampus.data.local.dao.CourseDao
import com.reflection.thecampus.data.local.dao.TestAttemptDao
import com.reflection.thecampus.data.local.dao.TestDao
import com.reflection.thecampus.data.local.dao.UserProfileDao
import com.reflection.thecampus.data.local.entity.CourseEntity
import com.reflection.thecampus.data.local.entity.TestAttemptEntity
import com.reflection.thecampus.data.local.entity.TestEntity
import com.reflection.thecampus.data.local.entity.UserProfileEntity

@Database(
    entities = [
        CourseEntity::class,
        TestEntity::class,
        UserProfileEntity::class,
        TestAttemptEntity::class
    ],
    version = 3, // Increment version for schema change
    exportSchema = false
)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun courseDao(): CourseDao
    abstract fun testDao(): TestDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun testAttemptDao(): TestAttemptDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "the_campus_database"
                )
                    .fallbackToDestructiveMigration() // For development only
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Clear the database instance (useful for logout)
         */
        fun clearInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
