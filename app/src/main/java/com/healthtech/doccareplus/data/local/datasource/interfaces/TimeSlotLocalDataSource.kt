package com.healthtech.doccareplus.data.local.datasource.interfaces

import com.healthtech.doccareplus.domain.model.TimePeriod
import com.healthtech.doccareplus.domain.model.TimeSlot
import kotlinx.coroutines.flow.Flow

interface TimeSlotLocalDataSource {
    fun getAllTimeSlots(): Flow<List<TimeSlot>>
    
    fun getTimeSlotsByPeriod(period: TimePeriod): Flow<List<TimeSlot>>

    suspend fun saveTimeSlots(timeSlots: List<TimeSlot>)

    suspend fun deleteAllTimeSlots()

    suspend fun deleteTimeSlotsByPeriod(period: TimePeriod)
}