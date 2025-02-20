package com.healthtech.doccareplus.data.local.datasource.impl

import com.healthtech.doccareplus.data.local.dao.CategoryDao
import com.healthtech.doccareplus.data.local.datasource.interfaces.CategoryLocalDataSource
import com.healthtech.doccareplus.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryLocalDataSourceImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryLocalDataSource {
    override fun getCategories(): Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    override suspend fun insertCategories(categories: List<CategoryEntity>) {
        categoryDao.insertCategories(categories)
    }

    override suspend fun deleleAllCategories() {
        categoryDao.deleteAllCategories()
    }
}