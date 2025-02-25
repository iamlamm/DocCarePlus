package com.healthtech.doccareplus.data.repository

import android.util.Log
import com.healthtech.doccareplus.data.local.datasource.impl.CategoryLocalDataSourceImpl
import com.healthtech.doccareplus.domain.model.Category
import com.healthtech.doccareplus.data.remote.datasource.impl.CategoryRemoteDataSourceImpl
import com.healthtech.doccareplus.domain.mapper.toCategory
import com.healthtech.doccareplus.domain.mapper.toCategoryEntity
import com.healthtech.doccareplus.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/*
.get -> Flow<List<CategoryEntity>> .map1: List<CategoryEntity> → List<Category> .map2: CategoryEntity → Category

localDataSource.getCategories() // Flow<List<CategoryEntity>>
.map { entities ->         // entities là List<CategoryEntity>
    entities.map { entity -> // entity là một CategoryEntity
        entity.toCategory()  // chuyển đổi thành Category
    }
}
tìm hiểu về .map của Flow và List

@Singleton: chỉ định rằng chỉ một instance duy nhất của lớp CategoryRepositoryImpl sẽ được tạo ra và được chia sẻ trong toàn bộ phạm vi (scope) của component mà nó được cung cấp.
*/

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val remoteDataSource: CategoryRemoteDataSourceImpl,
    private val localDataSource: CategoryLocalDataSourceImpl
) : CategoryRepository {
    override fun observeCategories(): Flow<Result<List<Category>>> = channelFlow {
        launch {
            try {
                // Emit local data first
                localDataSource.getCategories().catch { e ->
                    if (e !is CancellationException) {
                        send(Result.failure(Exception("Lỗi khi tải dữ liệu local: ${e.message}")))
                    }
                }
                    .map { listEntities ->
                        listEntities.map {
                            it.toCategory()
                        }
                    }.collect { listCategories ->
                        if (listCategories.isNotEmpty()) {
                            send(Result.success(listCategories))
                        }
                    }
            } catch (e: Exception) {
                send(Result.failure(Exception("Lỗi khi tải dữ liệu local: ${e.message}")))
                Log.e("CategoryRepository", "Error loading local data", e)
            }
        }

        launch {
            // Then fetch from remote
            try {
                remoteDataSource.getCategories().catch { e ->
                    if (e !is CancellationException) {
                        send(Result.failure(Exception("Lỗi khi tải dữ liệu remote: ${e.message}")))
                    }
                }.collect { listCategories ->
                    // Save to local database
                    localDataSource.insertCategories(listCategories.map { it.toCategoryEntity() })
                    // Emit new data
                    send(Result.success(listCategories))
                }
            } catch (e: Exception) {
                send(Result.failure(Exception("Lỗi khi tải dữ liệu remote: ${e.message}")))
                Log.e("CategoryRepository", "Error fetching remote data", e)
            }
        }
    }
}