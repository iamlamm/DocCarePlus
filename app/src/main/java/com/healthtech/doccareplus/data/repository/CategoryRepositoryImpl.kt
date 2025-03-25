package com.healthtech.doccareplus.data.repository

import com.healthtech.doccareplus.data.local.datasource.impl.CategoryLocalDataSourceImpl
import com.healthtech.doccareplus.data.remote.datasource.impl.CategoryRemoteDataSourceImpl
import com.healthtech.doccareplus.domain.mapper.toCategory
import com.healthtech.doccareplus.domain.mapper.toCategoryEntity
import com.healthtech.doccareplus.domain.model.Category
import com.healthtech.doccareplus.domain.repository.CategoryRepository
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
class CategoryRepositoryImpl @Inject constructor(
    private val remoteDataSource: CategoryRemoteDataSourceImpl,
    private val localDataSource: CategoryLocalDataSourceImpl
) : CategoryRepository {
    override fun observeCategories(): Flow<Result<List<Category>>> = callbackFlow {
        try {
            localDataSource.getCategories().catch { e ->
                    if (e !is CancellationException) {
                        send(Result.failure(Exception("Lỗi khi tải dữ liệu local: ${e.message}")))
                    }
                }.map { listEntities -> listEntities.map { it.toCategory() } }
                .collect { localCategories ->
                    if (localCategories.isNotEmpty()) {
                        send(Result.success(localCategories))
                    }

                    try {
                        remoteDataSource.getCategories().catch { e ->
                                if (e !is CancellationException) {
                                    send(Result.failure(Exception("Lỗi khi tải dữ liệu remote: ${e.message}")))
                                }
                            }.collect { remoteCategories ->
                                localDataSource.deleteAllCategories()
                                localDataSource.insertCategories(remoteCategories.map { it.toCategoryEntity() })

                                send(Result.success(remoteCategories))
                            }
                    } catch (e: Exception) {
                        if (e !is CancellationException) {
                            send(Result.failure(Exception("Lỗi khi tải dữ liệu remote: ${e.message}")))
                            Timber.tag("CategoryRepository").e(e, "Error fetching remote data")
                        }
                    }
                }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                send(Result.failure(Exception("Lỗi tổng quát: ${e.message}")))
                Timber.tag("CategoryRepository").e(e, "General error")
            }
        }

        awaitClose {
            Timber.tag("CategoryRepository").d("CategoryRepository flow closed")
        }
    }
}