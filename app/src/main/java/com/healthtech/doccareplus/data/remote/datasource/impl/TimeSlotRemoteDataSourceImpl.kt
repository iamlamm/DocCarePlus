package com.healthtech.doccareplus.data.remote.datasource.impl

import com.healthtech.doccareplus.data.remote.api.FirebaseApi
import com.healthtech.doccareplus.data.remote.datasource.interfaces.TimeSlotRemoteDataSource
import com.healthtech.doccareplus.domain.model.TimeSlot
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TimeSlotRemoteDataSourceImpl @Inject constructor(
    private val firebaseApi: FirebaseApi
) : TimeSlotRemoteDataSource {
    override fun getAllTimeSlots(): Flow<List<TimeSlot>> {
        return firebaseApi.getTimeSlots()
    }
}