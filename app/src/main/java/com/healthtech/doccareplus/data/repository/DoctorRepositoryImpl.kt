package com.healthtech.doccareplus.data.repository

import android.util.Log
import com.healthtech.doccareplus.data.local.datasource.impl.DoctorLocalDataSourceImpl
import com.healthtech.doccareplus.domain.model.Doctor
import com.healthtech.doccareplus.data.remote.datasource.impl.DoctorRemoteDataSourceImpl
import com.healthtech.doccareplus.domain.mapper.toDoctor
import com.healthtech.doccareplus.domain.mapper.toDoctorEntity
import com.healthtech.doccareplus.domain.repository.DoctorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException


@Singleton
class DoctorRepositoryImpl @Inject constructor(
    private val remoteDataSource: DoctorRemoteDataSourceImpl,
    private val localDataSource: DoctorLocalDataSourceImpl
) : DoctorRepository {
    override fun observeDoctors(): Flow<Result<List<Doctor>>> = channelFlow {
        launch {
            try {
                // Local trước rồi mới đến remote
                localDataSource.getDoctors().catch { e ->
                    if (e !is CancellationException) {
                        send(Result.failure(Exception("Lỗi khi tải dữ liệu local: ${e.message}")))
                    }
                }.map { listEntities ->
                    listEntities.map {
                        it.toDoctor()
                    }
                }.collect { listDoctors ->
                    if (listDoctors.isNotEmpty()) {
                        send(Result.success(listDoctors))
                    }
                }
            } catch (e: Exception) {
                send(Result.failure(Exception("Lỗi khi tải dữ liệu local: ${e.message}")))
                Log.e("DoctorRepository", "Error loading local data", e)
            }
        }

        launch {
            // Remote
            try {
                remoteDataSource.getDoctors().catch { e ->
                    if (e !is CancellationException) {
                        send(Result.failure(Exception("Lỗi khi tải dữ liệu remote: ${e.message}")))
                    }
                }.collect { listDoctors ->
                    localDataSource.insertDoctors(listDoctors.map { it.toDoctorEntity() })
                    send(Result.success(listDoctors))
                }

            } catch (e: Exception) {
                send(Result.failure(Exception("Lỗi khi tải dữ liệu remote: ${e.message}")))
                Log.e("DoctorRepository", "Error fetching remote data", e)
            }
        }

    }
}