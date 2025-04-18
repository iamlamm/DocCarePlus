package com.healthtech.doccareplus.data.remote.datasource.interfaces

import com.healthtech.doccareplus.domain.model.TimeSlot
import kotlinx.coroutines.flow.Flow

interface TimeSlotRemoteDataSource {
    fun getAllTimeSlots(): Flow<List<TimeSlot>>
}