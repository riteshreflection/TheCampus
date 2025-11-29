package com.reflection.thecampus.data.model

import androidx.annotation.Keep

@Keep
data class SiteAnnouncement(
    val id: String = "",
    val message: String = "",
    val status: String = "",  // "active" or "inactive"
    val ctaText: String = "",
    val ctaLink: String = "",
    val createdAt: Long = 0
)
