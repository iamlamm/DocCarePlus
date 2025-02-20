package com.healthtech.doccareplus.data.remote.datasource.interfaces

import com.healthtech.doccareplus.domain.model.Doctor
import kotlinx.coroutines.flow.Flow

interface DoctorRemoteDataSource {
    fun getDoctors(): Flow<List<Doctor>>
}