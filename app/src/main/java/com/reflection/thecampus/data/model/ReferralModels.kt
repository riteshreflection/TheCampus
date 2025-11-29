package com.reflection.thecampus.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ReferralData(
    val code: String? = null,
    val referredBy: String? = null
)

@IgnoreExtraProperties
data class ReferralWallet(
    val balance: Double = 0.0,
    val totalEarned: Double = 0.0,
    val transactions: Map<String, Transaction> = emptyMap()
)

@IgnoreExtraProperties
data class Transaction(
    val type: String? = null, // "credit" or "debit"
    val amount: Double = 0.0,
    val description: String? = null,
    val orderId: String? = null,
    val timestamp: Long = 0,
    val status: String? = null // "pending", "approved", "rejected" (for withdrawals)
)
