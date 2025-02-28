package com.healthtech.doccareplus.domain.model

data class TimeSlot(
    val id: Int,
    val startTime: String,
    val endTime: String,
    val period: TimePeriod
)