package com.healthtech.doccareplus.data.remote.datasource.impl

import com.healthtech.doccareplus.data.remote.api.FirebaseApi
import com.healthtech.doccareplus.data.remote.datasource.interfaces.CategoryRemoteDataSource
import com.healthtech.doccareplus.domain.model.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRemoteDataSourceImpl @Inject constructor(
    private val firebaseApi: FirebaseApi
) : CategoryRemoteDataSource {
    override fun getCategories(): Flow<List<Category>> = firebaseApi.getCategories()
}