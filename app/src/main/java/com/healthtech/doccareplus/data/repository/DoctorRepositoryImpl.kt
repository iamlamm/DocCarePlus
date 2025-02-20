package com.healthtech.doccareplus.data.repository

import android.util.Log
import com.healthtech.doccareplus.data.local.datasource.impl.DoctorLocalDataSourceImpl
import com.healthtech.doccareplus.domain.model.Doctor
import com.healthtech.doccareplus.data.remote.datasource.impl.DoctorRemoteDataSourceImpl
import com.healthtech.doccareplus.domain.mapper.toDoctor
import com.healthtech.doccareplus.domain.mapper.toDoctorEntity
import com.healthtech.doccareplus.domain.repository.DoctorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DoctorRepositoryImpl @Inject constructor(
    private val remoteDataSource: DoctorRemoteDataSourceImpl,
    private val localDataSource: DoctorLocalDataSourceImpl
) : DoctorRepository {
    override fun getDoctors(): Flow<List<Doctor>> = channelFlow {
        launch {
            try {
                // Local trước rồi mới đến remote
                localDataSource.getDoctors().map { entities ->
                    entities.map {
                        it.toDoctor()
                    }
                }.collect { doctor ->
                    if (doctor.isNotEmpty()) {
                        send(doctor)
                    }
                }
            } catch (e: Exception) {
                Log.e("CategoryRepository", "Error ${e.message}")
            }
        }

        launch {
            // Remote
            try {
                remoteDataSource.getDoctors().collect { doctors ->
                    localDataSource.insertDoctors(doctors.map { it.toDoctorEntity() })
                    send(doctors)
                }

            } catch (e: Exception) {
                Log.e("DoctorRepository", "Error fetching remote data", e)
            }
        }

    }
}