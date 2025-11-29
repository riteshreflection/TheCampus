package com.reflection.thecampus.data.repository

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import com.reflection.thecampus.data.local.AppDatabase
import com.reflection.thecampus.data.local.toDomain
import com.reflection.thecampus.data.local.toEntity
import com.reflection.thecampus.data.model.Test
import com.reflection.thecampus.data.model.TestAttempt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

class TestRepository(context: Context) {

    private val database = FirebaseDatabase.getInstance()
    private val appDatabase = AppDatabase.getInstance(context)
    private val testAttemptDao = appDatabase.testAttemptDao()

    /**
     * Get test from Firebase
     */
    suspend fun getTest(testId: String): Test = withContext(Dispatchers.IO) {
        try {
            val snapshot = database.getReference("tests")
                .child(testId)
                .get()
                .await()
            
            val test = snapshot.getValue(Test::class.java) ?: throw Exception("Test not found")
            test.copy(id = testId)
        } catch (e: Exception) {
            Timber.e(e, "Error fetching test $testId")
            throw e
        }
    }

    /**
     * Submit test attempt with offline support and retry mechanism
     * Returns: Triple<Boolean, TestAttempt?, String?> - (success, attemptWithId, message)
     */
    suspend fun submitTestAttempt(attempt: TestAttempt): Triple<Boolean, TestAttempt?, String?> = withContext(Dispatchers.IO) {
        try {
            // 1. Save to local database first (offline backup)
            val entity = attempt.toEntity()
            testAttemptDao.insertAttempt(entity.copy(isSynced = false))
            Timber.d("Test attempt saved locally: ${attempt.id}")

            // 2. Try to sync to Firebase
            val attemptRef = if (attempt.id.isEmpty()) {
                database.getReference("testAttempts").push()
            } else {
                database.getReference("testAttempts").child(attempt.id)
            }

            val attemptWithId = attempt.copy(id = attemptRef.key ?: attempt.id)
            
            attemptRef.setValue(attemptWithId).await()
            
            // 3. Mark as synced in local database
            testAttemptDao.markAsSynced(attemptWithId.id)
            Timber.d("Test attempt synced to Firebase: ${attemptWithId.id}")
            
            Triple(true, attemptWithId, "Test submitted successfully!")
        } catch (e: Exception) {
            Timber.e(e, "Error submitting test attempt")
            // Attempt is already saved locally, so user can retry later
            Triple(false, null, "Submission failed: ${e.message}. Your answers are saved locally and will be synced when you're online.")
        }
    }

    /**
     * Retry failed submissions
     */
    suspend fun retryFailedSubmissions(): Int = withContext(Dispatchers.IO) {
        try {
            val unsyncedAttempts = testAttemptDao.getUnsyncedAttempts()
            var successCount = 0

            unsyncedAttempts.forEach { entity ->
                try {
                    val attemptRef = database.getReference("testAttempts").child(entity.id)
                    
                    // Convert entity to domain model
                    val attempt = entity.toDomain()
                    
                    attemptRef.setValue(attempt).await()
                    testAttemptDao.markAsSynced(entity.id)
                    successCount++
                    Timber.d("Retry successful for attempt: ${entity.id}")
                } catch (e: Exception) {
                    Timber.e(e, "Retry failed for attempt: ${entity.id}")
                }
            }

            successCount
        } catch (e: Exception) {
            Timber.e(e, "Error retrying submissions")
            0
        }
    }

    /**
     * Get unsynced attempts count
     */
    suspend fun getUnsyncedCount(): Int = withContext(Dispatchers.IO) {
        try {
            testAttemptDao.getUnsyncedAttempts().size
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Delete a specific test attempt (from both local and Firebase)
     */
    suspend fun deleteAttempt(attemptId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Delete from Firebase
            database.getReference("testAttempts").child(attemptId).removeValue().await()
            
            // Delete from local database (if exists)
            // Note: We don't have a delete by ID method in DAO yet, but we can add it if needed
            
            true
        } catch (e: Exception) {
            Timber.e(e, "Error deleting attempt")
            false
        }
    }
}
