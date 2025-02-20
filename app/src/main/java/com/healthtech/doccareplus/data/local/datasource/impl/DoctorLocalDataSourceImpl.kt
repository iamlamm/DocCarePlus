package com.healthtech.doccareplus.data.local.datasource.impl

import com.healthtech.doccareplus.data.local.dao.DoctorDao
import com.healthtech.doccareplus.data.local.datasource.interfaces.DoctorLocalDataSource
import com.healthtech.doccareplus.data.local.entity.DoctorEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DoctorLocalDataSourceImpl @Inject constructor(
    private val doctorDao: DoctorDao
) : DoctorLocalDataSource {
    override fun getDoctors(): Flow<List<DoctorEntity>> = doctorDao.getAllDoctors()

    override suspend fun insertDoctors(doctors: List<DoctorEntity>) {
        doctorDao.insertDoctors(doctors)
    }

    override suspend fun deleteAllDoctors() {
        doctorDao.deleteAllDoctor()
    }
}