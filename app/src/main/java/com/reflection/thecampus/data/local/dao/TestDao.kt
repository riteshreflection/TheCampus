package com.reflection.thecampus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reflection.thecampus.data.local.entity.TestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TestDao {
    
    @Query("SELECT * FROM tests WHERE id = :testId")
    suspend fun getTestById(testId: String): TestEntity?
    
    @Query("SELECT * FROM tests WHERE courseId = :courseId ORDER BY cachedAt DESC")
    fun getTestsByCourseFlow(courseId: String): Flow<List<TestEntity>>
    
    @Query("SELECT * FROM tests WHERE isFree = 1 ORDER BY cachedAt DESC")
    fun getFreeTestsFlow(): Flow<List<TestEntity>>
    
    @Query("SELECT * FROM tests ORDER BY cachedAt DESC")
    fun getAllTestsFlow(): Flow<List<TestEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTest(test: TestEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTests(tests: List<TestEntity>)
    
    @Query("DELETE FROM tests WHERE cachedAt < :timestamp")
    suspend fun deleteOldCache(timestamp: Long)
    
    @Query("DELETE FROM tests")
    suspend fun deleteAll()
}
