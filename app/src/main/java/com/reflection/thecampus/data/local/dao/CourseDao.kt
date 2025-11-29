package com.reflection.thecampus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reflection.thecampus.data.local.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    
    @Query("SELECT * FROM courses WHERE isEnrolled = 1 ORDER BY cachedAt DESC")
    fun getEnrolledCoursesFlow(): Flow<List<CourseEntity>>
    
    @Query("SELECT * FROM courses WHERE id = :courseId")
    suspend fun getCourseById(courseId: String): CourseEntity?
    
    @Query("SELECT * FROM courses WHERE status = 'active' ORDER BY cachedAt DESC")
    fun getAllActiveCoursesFlow(): Flow<List<CourseEntity>>
    
    @Query("SELECT * FROM courses ORDER BY cachedAt DESC")
    fun getAllCoursesFlow(): Flow<List<CourseEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)
    
    @Query("UPDATE courses SET isEnrolled = 1, lastSyncedAt = :timestamp WHERE id = :courseId")
    suspend fun markAsEnrolled(courseId: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE courses SET isEnrolled = 0 WHERE id = :courseId")
    suspend fun markAsUnenrolled(courseId: String)
    
    @Query("DELETE FROM courses WHERE cachedAt < :timestamp")
    suspend fun deleteOldCache(timestamp: Long)
    
    @Query("DELETE FROM courses")
    suspend fun deleteAll()
}
