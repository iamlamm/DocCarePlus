package com.healthtech.doccareplus.domain.model

data class Appointment(
    val id: String,
    val doctorId: String,
    val userId: String,
    val date: String,
    val slotId: Int,
    val doctorName: String,
    val doctorAvatar: String = "",
    val symptoms: String? = null,
    val notes: String? = null,
    val status: String = "upcoming", // upcoming, completed, cancelled
    val createdAt: Long = System.currentTimeMillis()
)
