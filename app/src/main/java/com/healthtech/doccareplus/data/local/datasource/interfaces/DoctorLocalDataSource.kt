package com.healthtech.doccareplus.data.local.datasource.interfaces

import com.healthtech.doccareplus.data.local.entity.DoctorEntity
import kotlinx.coroutines.flow.Flow

interface DoctorLocalDataSource {
    fun getDoctors(): Flow<List<DoctorEntity>>

    suspend fun insertDoctors(doctors: List<DoctorEntity>)

    suspend fun deleteAllDoctors()
}