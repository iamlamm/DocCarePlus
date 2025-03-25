package com.healthtech.doccareplus.data.repository

import com.healthtech.doccareplus.data.local.datasource.impl.DoctorLocalDataSourceImpl
import com.healthtech.doccareplus.data.remote.datasource.impl.DoctorRemoteDataSourceImpl
import com.healthtech.doccareplus.domain.mapper.toDoctor
import com.healthtech.doccareplus.domain.mapper.toDoctorEntity
import com.healthtech.doccareplus.domain.model.Doctor
import com.healthtech.doccareplus.domain.repository.DoctorRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException


@Singleton
class DoctorRepositoryImpl @Inject constructor(
    private val remoteDataSource: DoctorRemoteDataSourceImpl,
    private val localDataSource: DoctorLocalDataSourceImpl
) : DoctorRepository {
    override fun observeDoctors(): Flow<Result<List<Doctor>>> = callbackFlow {
        try {
            localDataSource.getDoctors()
                .catch { e ->
                    if (e !is CancellationException) {
                        send(Result.failure(Exception("Lỗi khi tải dữ liệu local: ${e.message}")))
                    }
                }
                .map { listEntities -> listEntities.map { it.toDoctor() } }
                .collect { localDoctors ->
                    if (localDoctors.isNotEmpty()) {
                        send(Result.success(localDoctors))
                    }

                    try {
                        remoteDataSource.getDoctors()
                            .catch { e ->
                                if (e !is CancellationException) {
                                    send(Result.failure(Exception("Lỗi khi tải dữ liệu remote: ${e.message}")))
                                }
                            }
                            .collect { remoteDoctors ->
                                localDataSource.deleteAllDoctors()
                                localDataSource.insertDoctors(remoteDoctors.map { it.toDoctorEntity() })
                                send(Result.success(remoteDoctors))
                            }
                    } catch (e: Exception) {
                        if (e !is CancellationException) {
                            send(Result.failure(Exception("Lỗi khi tải dữ liệu remote: ${e.message}")))
                            Timber.tag("DoctorRepository").e(e, "Error fetching remote data")
                        }
                    }
                }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                send(Result.failure(Exception("Lỗi tổng quát: ${e.message}")))
                Timber.tag("DoctorRepository").e(e, "General error")
            }
        }

        awaitClose {
            Timber.tag("DoctorRepository").d("DoctorRepository flow closed")
        }
    }

    override fun getDoctorsByCategory(categoryId: Int): Flow<Result<List<Doctor>>> = callbackFlow {
        try {
            localDataSource.getDoctors()
                .catch { e ->
                    if (e !is CancellationException) {
                        send(Result.failure(Exception("Lỗi khi tải dữ liệu local: ${e.message}")))
                    }
                }
                .map { listEntities ->
                    listEntities.filter { it.categoryId == categoryId }
                        .map { it.toDoctor() }
                }
                .collect { filteredLocalDoctors ->
                    if (filteredLocalDoctors.isNotEmpty()) {
                        send(Result.success(filteredLocalDoctors))
                    }

                    try {
                        remoteDataSource.getDoctors()
                            .catch { e ->
                                if (e !is CancellationException) {
                                    send(Result.failure(Exception("Lỗi khi tải dữ liệu remote: ${e.message}")))
                                }
                            }
                            .collect { remoteDoctors ->
                                val filteredRemoteDoctors =
                                    remoteDoctors.filter { it.categoryId == categoryId }

                                send(Result.success(filteredRemoteDoctors))
                            }
                    } catch (e: Exception) {
                        if (e !is CancellationException) {
                            send(Result.failure(Exception("Lỗi khi tải dữ liệu remote: ${e.message}")))
                            Timber.tag("DoctorRepository")
                                .e(e, "Error fetching remote data by category")
                        }
                    }
                }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                send(Result.failure(Exception("Lỗi tổng quát: ${e.message}")))
                Timber.tag("DoctorRepository").e(e, "General error in getDoctorsByCategory")
            }
        }

        awaitClose {
            Timber.d("DoctorRepository: getDoctorsByCategory flow closed")
        }
    }
}