package com.reflection.thecampus.data.local.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val email: String,
    val fullName: String,
    val mobileNumber: String,
    val profilePictureUrl: String,
    val createdAt: Long,
    val lastSyncedAt: Long = System.currentTimeMillis()
)
