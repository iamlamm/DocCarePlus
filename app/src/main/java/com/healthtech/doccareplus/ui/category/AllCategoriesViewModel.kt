package com.healthtech.doccareplus.ui.category

import androidx.lifecycle.ViewModel
import com.healthtech.doccareplus.domain.model.Category
import com.healthtech.doccareplus.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class AllCategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    val categories: Flow<List<Category>> = categoryRepository.getCategories()
}