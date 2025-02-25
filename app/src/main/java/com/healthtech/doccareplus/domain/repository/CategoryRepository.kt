package com.healthtech.doccareplus.domain.repository

import com.healthtech.doccareplus.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(): Flow<Result<List<Category>>>
}