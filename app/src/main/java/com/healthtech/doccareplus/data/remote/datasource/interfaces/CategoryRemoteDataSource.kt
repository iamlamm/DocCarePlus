package com.healthtech.doccareplus.data.remote.datasource.interfaces

import com.healthtech.doccareplus.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRemoteDataSource {
    fun getCategories(): Flow<List<Category>>
}