package com.healthtech.doccareplus.data.repository

import android.util.Log
import com.healthtech.doccareplus.data.local.datasource.impl.CategoryLocalDataSourceImpl
import com.healthtech.doccareplus.domain.model.Category
import com.healthtech.doccareplus.data.remote.datasource.impl.CategoryRemoteDataSourceImpl
import com.healthtech.doccareplus.domain.mapper.toCategory
import com.healthtech.doccareplus.domain.mapper.toCategoryEntity
import com.healthtech.doccareplus.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

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
    override fun getCategories(): Flow<List<Category>> = channelFlow {
        launch {
            try {
                // Emit local data first
                localDataSource.getCategories().map { entities ->
                    entities.map {
                        it.toCategory()
                    }
                }.collect { category ->
                    if (category.isNotEmpty()) {
                        send(category)
                    }
                }
            } catch (_: Exception) {

            }
        }

        launch {
            // Then fetch from remote
            try {
                remoteDataSource.getCategories().collect { categories ->
                    // Save to local database
                    localDataSource.insertCategories(categories.map { it.toCategoryEntity() })
                    // Emit new data
                    send(categories)
                }
            } catch (e: Exception) {
                Log.e("CategoryRepository", "Error fetching remote data", e)
            }
        }
    }
}