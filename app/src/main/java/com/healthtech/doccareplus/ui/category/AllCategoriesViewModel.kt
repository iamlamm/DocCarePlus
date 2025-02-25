package com.healthtech.doccareplus.ui.category

import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.common.base.BaseDataViewModel
import com.healthtech.doccareplus.common.state.UiState
import com.healthtech.doccareplus.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllCategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : BaseDataViewModel() {

    // Chỉ để theo dõi trạng thái đã tải xong chưa
    private val _isInitialDataLoaded = MutableStateFlow(false)
    val isInitialDataLoaded = _isInitialDataLoaded.asStateFlow()

    init {
        // Sử dụng lại phương thức từ BaseDataViewModel
        viewModelScope.launch(Dispatchers.IO) {
            observeCategories(categoryRepository)
            _isInitialDataLoaded.value = true
        }
    }

    // Hàm này chỉ gọi khi cần refresh dữ liệu
    fun refreshCategoriesIfNeeded() {
        if (categories.value !is UiState.Success) {
            viewModelScope.launch(Dispatchers.IO) {
                // Sử dụng phương thức refresh từ BaseDataViewModel
                refreshCategories(categoryRepository)
            }
        }
    }
}