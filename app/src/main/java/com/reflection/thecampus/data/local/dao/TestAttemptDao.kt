package com.reflection.thecampus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.reflection.thecampus.data.local.entity.TestAttemptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TestAttemptDao {
    
    @Query("SELECT * FROM test_attempts WHERE studentId = :userId ORDER BY submittedAt DESC")
    fun getAttemptsByUserFlow(userId: String): Flow<List<TestAttemptEntity>>
    
    @Query("SELECT * FROM test_attempts WHERE testId = :testId AND studentId = :userId ORDER BY submittedAt DESC")
    fun getAttemptsByTestFlow(testId: String, userId: String): Flow<List<TestAttemptEntity>>
    
    @Query("SELECT * FROM test_attempts WHERE isSynced = 0")
    suspend fun getUnsyncedAttempts(): List<TestAttemptEntity>
    
    @Query("SELECT * FROM test_attempts WHERE id = :attemptId")
    suspend fun getAttemptById(attemptId: String): TestAttemptEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: TestAttemptEntity)
    
    @Query("UPDATE test_attempts SET isSynced = 1 WHERE id = :attemptId")
    suspend fun markAsSynced(attemptId: String)
    
    @Query("DELETE FROM test_attempts WHERE studentId = :userId")
    suspend fun deleteUserAttempts(userId: String)
    
    @Query("DELETE FROM test_attempts")
    suspend fun deleteAll()
}
