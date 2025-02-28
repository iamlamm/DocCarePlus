package com.healthtech.doccareplus.domain.mapper

import com.healthtech.doccareplus.data.local.entity.TimeSlotEntity
import com.healthtech.doccareplus.domain.model.TimeSlot

fun TimeSlotEntity.toTimeSlot(): TimeSlot {
    return TimeSlot(
        id = id,
        startTime = startTime,
        endTime = endTime,
        period = period
    )
}

fun TimeSlot.toTimeSlotEntity(): TimeSlotEntity {
    return TimeSlotEntity(
        id = id,
        startTime = startTime,
        endTime = endTime,
        period = period
    )
}