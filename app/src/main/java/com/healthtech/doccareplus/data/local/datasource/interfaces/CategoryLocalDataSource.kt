package com.healthtech.doccareplus.data.local.datasource.interfaces

import com.healthtech.doccareplus.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryLocalDataSource {
    fun getCategories(): Flow<List<CategoryEntity>>

    suspend fun insertCategories(categories: List<CategoryEntity>)

    suspend fun deleteAllCategories()
}