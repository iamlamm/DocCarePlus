package com.healthtech.doccareplus.domain.model

data class BookingRequest(
    val doctorId: Int,
    val userId: String,
    val date: String,
    val slotId: Int
)