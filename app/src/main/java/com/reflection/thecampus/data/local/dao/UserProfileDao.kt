package com.reflection.thecampus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reflection.thecampus.data.local.entity.UserProfileEntity

@Dao
interface UserProfileDao {
    
    @Query("SELECT * FROM user_profile WHERE userId = :userId")
    suspend fun getUserProfile(userId: String): UserProfileEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)
    
    @Query("UPDATE user_profile SET lastSyncedAt = :timestamp WHERE userId = :userId")
    suspend fun updateSyncTimestamp(userId: String, timestamp: Long)
    
    @Query("DELETE FROM user_profile WHERE userId = :userId")
    suspend fun deleteUserProfile(userId: String)
    
    @Query("DELETE FROM user_profile")
    suspend fun deleteAll()
}
