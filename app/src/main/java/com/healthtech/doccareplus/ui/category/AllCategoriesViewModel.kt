package com.healthtech.doccareplus.ui.category

import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.common.base.BaseDataViewModel
import com.healthtech.doccareplus.common.state.UiState
import com.healthtech.doccareplus.domain.model.Category
import com.healthtech.doccareplus.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AllCategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : BaseDataViewModel() {

    // Chỉ để theo dõi trạng thái đã tải xong chưa
    private val _isInitialDataLoaded = MutableStateFlow(false)
    val isInitialDataLoaded = _isInitialDataLoaded.asStateFlow()

    // Theo dõi thời gian cập nhật cuối
    private var lastRefreshTime = 0L

    // Thêm biến cho tìm kiếm
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Danh sách kết quả tìm kiếm
    private val _searchResults = MutableStateFlow<List<Category>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive = _isSearchActive.asStateFlow()

    // Lưu trữ danh sách gốc
    private var originalCategories = listOf<Category>()

    init {
        viewModelScope.launch {
            // Load data ban đầu
            observeCategories(categoryRepository)
            _isInitialDataLoaded.value = true

            // Quan sát categories để cập nhật state
            categories.collect { state ->
                if (state is UiState.Success) {
                    originalCategories = state.data  // Luôn cập nhật originalCategories trước
                    
                    if (!_isSearchActive.value) {
                        _searchResults.value = originalCategories
                    } else {
                        filterCategories(_searchQuery.value)  // Nếu đang search thì filter lại
                    }
                }
            }
        }
    }

    // Hàm đặt query tìm kiếm
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterCategories(query)
    }

    // Hàm lọc danh sách
    private fun filterCategories(query: String) {
        if (query.isEmpty()) {
            _searchResults.value = originalCategories
            return
        }

        val filteredList = originalCategories.filter {
            it.name.contains(query, ignoreCase = true)
        }

        _searchResults.value = filteredList
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            clearSearch()
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        categories.value.let { state ->
            if (state is UiState.Success) {
                _searchResults.value = state.data
            }
        }
    }

    // Hàm này kiểm tra và refresh chỉ khi cần thiết
    fun checkAndRefreshIfNeeded() {
        val currentTime = System.currentTimeMillis()
        // Chỉ refresh nếu đã trôi qua ít nhất 1 phút từ lần cuối
        if (currentTime - lastRefreshTime > 60000) {
            refreshCategories()
        }
    }

    // Hàm này gọi khi người dùng chủ động muốn refresh
    fun refreshCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            lastRefreshTime = System.currentTimeMillis()
            refreshCategories(categoryRepository)
        }
    }

    // Thêm method để restore state
    fun restoreState() {
        if (originalCategories.isNotEmpty()) {
            if (!_isSearchActive.value) {
                _searchResults.value = originalCategories
            } else {
                filterCategories(_searchQuery.value)
            }
        }
    }
}