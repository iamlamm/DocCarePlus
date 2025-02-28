package com.healthtech.doccareplus.domain.repository

import com.healthtech.doccareplus.domain.model.TimeSlot
import kotlinx.coroutines.flow.Flow

interface TimeSlotRepository {
    fun observeTimeSlots(): Flow<Result<List<TimeSlot>>>
}