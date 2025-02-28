package com.healthtech.doccareplus.domain.model


data class BookingResponse(
    val success: Boolean,
    val message: String,
    val appointmentId: Int? = null
)
