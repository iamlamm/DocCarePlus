package com.healthtech.doccareplus.domain.model

data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val time: Long = 0,
    val read: Boolean = false,
    val type: NotificationType = NotificationType.APPOINTMENT_BOOKED,
    val date: String = "",
    val appointmentId: String = ""
)