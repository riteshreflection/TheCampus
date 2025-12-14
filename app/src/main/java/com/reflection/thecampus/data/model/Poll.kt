package com.reflection.thecampus.data.model

data class Poll(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "single", // "single", "multiple", "quiz"
    val options: List<PollOption> = emptyList(),
    val allowReselection: Boolean = false,
    val status: String = "draft", // "draft", "active", "closed"
    val createdAt: Long = 0L,
    val createdBy: String = "",
    val expiresAt: Long? = null,
    val courseIds: List<String>? = null, // null or empty = global poll
    val responses: Map<String, PollResponse> = emptyMap()
) {
    fun hasUserVoted(userId: String): Boolean {
        return responses.containsKey(userId)
    }
    
    fun getUserResponse(userId: String): PollResponse? {
        return responses[userId]
    }
    
    fun isExpired(): Boolean {
        return expiresAt != null && System.currentTimeMillis() > expiresAt
    }
    
    fun isActive(): Boolean {
        return status == "active" && !isExpired()
    }
    
    fun isGlobal(): Boolean {
        return courseIds == null || courseIds.isEmpty()
    }
    
    fun getTotalVotes(): Int {
        return responses.size
    }
    
    fun getOptionVoteCount(optionId: String): Int {
        return responses.values.count { response ->
            response.selectedOptions.contains(optionId)
        }
    }
    
    fun getOptionPercentage(optionId: String): Int {
        val total = getTotalVotes()
        if (total == 0) return 0
        val count = getOptionVoteCount(optionId)
        return ((count.toFloat() / total) * 100).toInt()
    }
}
