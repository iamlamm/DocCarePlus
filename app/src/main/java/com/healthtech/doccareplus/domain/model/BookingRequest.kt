package com.healthtech.doccareplus.domain.model

data class BookingRequest(
    val doctorId: String,
    val userId: String,
    val date: String,
    val slotId: Int,
    val symptoms: String? = null,
    val notes: String? = null,
    val userName: String = ""
)