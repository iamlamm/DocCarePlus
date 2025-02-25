package com.healthtech.doccareplus.ui.category

import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.common.base.BaseDataViewModel
import com.healthtech.doccareplus.common.state.UiState
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

    init {
        // Bắt đầu load dữ liệu ngay khi khởi tạo
        viewModelScope.launch {
            // Sử dụng observeCategories sẽ tự động emit local data trước, sau đó remote
            // Điều này phù hợp với mô hình của CategoryRepositoryImpl
            observeCategories(categoryRepository)
            _isInitialDataLoaded.value = true
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
}