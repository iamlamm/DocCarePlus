package com.healthtech.doccareplus.data.repository

import com.healthtech.doccareplus.data.local.datasource.interfaces.TimeSlotLocalDataSource
import com.healthtech.doccareplus.data.remote.datasource.interfaces.TimeSlotRemoteDataSource
import com.healthtech.doccareplus.domain.model.TimeSlot
import com.healthtech.doccareplus.domain.repository.TimeSlotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

class TimeSlotRepositoryImpl @Inject constructor(
    private val localDataSource: TimeSlotLocalDataSource,
    private val remoteDataSource: TimeSlotRemoteDataSource
) : TimeSlotRepository {
    override fun observeTimeSlots(): Flow<Result<List<TimeSlot>>> = flow {
        try {
            Timber.d("Starting to observe time slots")

            val localSlots = localDataSource.getAllTimeSlots().firstOrNull() ?: emptyList()
            if (localSlots.isNotEmpty()) {
                Timber.d("Local time slots loaded: %s", localSlots.size)
                emit(Result.success(localSlots))
            }

            try {
                val remoteSlots = remoteDataSource.getAllTimeSlots().first()
                Timber.d("Remote time slots received: %s", remoteSlots.size)
                if (remoteSlots.isNotEmpty()) {
                    localDataSource.saveTimeSlots(remoteSlots)
                }
                emit(Result.success(remoteSlots))
            } catch (e: Exception) {
                Timber.e("Error loading remote slots: %s", e.message)
                if (localSlots.isEmpty()) {
                    emit(Result.failure(e))
                }
            }
        } catch (e: Exception) {
            Timber.e("Error in observeTimeSlots: %s", e.message)
            emit(Result.failure(e))
        }
    }
}