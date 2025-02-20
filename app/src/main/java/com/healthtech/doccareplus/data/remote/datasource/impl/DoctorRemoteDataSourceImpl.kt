package com.healthtech.doccareplus.data.remote.datasource.impl

import com.healthtech.doccareplus.data.remote.api.FirebaseApi
import com.healthtech.doccareplus.data.remote.datasource.interfaces.DoctorRemoteDataSource
import com.healthtech.doccareplus.domain.model.Doctor
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DoctorRemoteDataSourceImpl @Inject constructor(
    private val firebaseApi: FirebaseApi
) : DoctorRemoteDataSource {
    override fun getDoctors(): Flow<List<Doctor>> = firebaseApi.getDoctors()
}