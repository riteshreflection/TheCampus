package com.reflection.thecampus.data.model

data class LoginHistory(
    val id: String = "",
    val userId: String = "",
    val deviceId: String = "",
    val deviceModel: String = "",
    val osVersion: String = "",
    val loginTime: Long = 0,
    val appVersion: String = ""
)
