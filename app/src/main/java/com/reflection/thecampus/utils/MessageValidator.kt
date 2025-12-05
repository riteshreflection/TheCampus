package com.reflection.thecampus.utils

object MessageValidator {
    
    private const val MAX_MESSAGE_LENGTH = 2000
    private const val MAX_MESSAGES_PER_MINUTE = 5
    private const val MAX_MESSAGES_PER_HOUR = 50
    private const val SPAM_THRESHOLD_MS = 2000L // 2 seconds
    
    private val messageTimestampsMinute = mutableListOf<Long>()
    private val messageTimestampsHour = mutableListOf<Long>()
    private var lastMessageText: String? = null
    private var lastMessageTime: Long = 0
    
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null,
        val remainingTimeSeconds: Int = 0 // Time until user can send again
    )
    
    fun validateMessage(text: String): ValidationResult {
        // Check if empty
        if (text.isBlank()) {
            return ValidationResult(false, "Message cannot be empty")
        }
        
        // Check length
        if (text.length > MAX_MESSAGE_LENGTH) {
            return ValidationResult(false, "Message too long (max $MAX_MESSAGE_LENGTH characters)")
        }
        
        // Check for profanity
        val profanityError = ProfanityFilter.validateMessage(text)
        if (profanityError != null) {
            return ValidationResult(false, profanityError)
        }
        
        val now = System.currentTimeMillis()
        
        // Clean up old timestamps
        messageTimestampsMinute.removeAll { now - it > 60000 } // Remove older than 1 minute
        messageTimestampsHour.removeAll { now - it > 3600000 } // Remove older than 1 hour
        
        // Check hourly limit first
        if (messageTimestampsHour.size >= MAX_MESSAGES_PER_HOUR) {
            val oldestTimestamp = messageTimestampsHour.minOrNull() ?: now
            val remainingMs = 3600000 - (now - oldestTimestamp)
            val remainingMinutes = (remainingMs / 60000).toInt()
            return ValidationResult(
                false, 
                "Hourly limit reached ($MAX_MESSAGES_PER_HOUR messages/hour)",
                remainingMinutes * 60
            )
        }
        
        // Check per-minute rate limiting
        if (messageTimestampsMinute.size >= MAX_MESSAGES_PER_MINUTE) {
            val oldestTimestamp = messageTimestampsMinute.minOrNull() ?: now
            val remainingMs = 60000 - (now - oldestTimestamp)
            val remainingSeconds = (remainingMs / 1000).toInt() + 1
            return ValidationResult(
                false, 
                "Slow down! You can send again in",
                remainingSeconds
            )
        }
        
        // Check for spam (duplicate messages)
        if (text == lastMessageText && (now - lastMessageTime) < SPAM_THRESHOLD_MS) {
            return ValidationResult(false, "Please don't send duplicate messages")
        }
        
        // Update tracking
        messageTimestampsMinute.add(now)
        messageTimestampsHour.add(now)
        lastMessageText = text
        lastMessageTime = now
        
        return ValidationResult(true)
    }
    
    fun getRemainingQuota(): Pair<Int, Int> {
        val now = System.currentTimeMillis()
        messageTimestampsMinute.removeAll { now - it > 60000 }
        messageTimestampsHour.removeAll { now - it > 3600000 }
        
        return Pair(
            MAX_MESSAGES_PER_MINUTE - messageTimestampsMinute.size,
            MAX_MESSAGES_PER_HOUR - messageTimestampsHour.size
        )
    }
    
    fun reset() {
        messageTimestampsMinute.clear()
        messageTimestampsHour.clear()
        lastMessageText = null
        lastMessageTime = 0
    }
}
