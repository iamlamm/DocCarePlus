package com.healthtech.doccareplus.domain.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val role: UserRole = UserRole.PATIENT,
    val avatar: String = "",
    val createdAt: Long = System.currentTimeMillis()
)