package com.healthtech.doccareplus.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.data.local.preferences.UserPreferences
import com.healthtech.doccareplus.domain.repository.CategoryRepository
import com.healthtech.doccareplus.domain.repository.DoctorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val categoryRepository: CategoryRepository,
    private val doctorRepository: DoctorRepository
) : ViewModel() {
    private val _startDestination = MutableStateFlow<Int>(0)
    val startDestination = _startDestination.asStateFlow()
    
    // Trạng thái loading để theo dõi tiến trình tải dữ liệu
    private val _isDataPreloaded = MutableStateFlow(false)
    val isDataPreloaded = _isDataPreloaded.asStateFlow()

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            val destination = if (userPreferences.isUserLoggedIn()) {
                R.id.homeFragment
            } else {
                R.id.loginFragment
            }
            
            // Nếu user đã login, bắt đầu preload data
            if (destination == R.id.homeFragment) {
                preLoadHomeData()
            }
            
            // Cập nhật destination sau khi đã kiểm tra
            _startDestination.value = destination
        }
    }

    fun preLoadHomeData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Tải song song các loại dữ liệu để tối ưu thời gian
                launch { categoryRepository.observeCategories().first() }
                launch { doctorRepository.observeDoctors().first() }
                
                // Đánh dấu đã tải xong dữ liệu
                withContext(Dispatchers.Main) {
                    _isDataPreloaded.value = true
                }
            } catch (e: Exception) {
                // Log lỗi nếu cần thiết, nhưng vẫn cho phép tiếp tục
                // để tránh kẹt ở màn hình splash
                withContext(Dispatchers.Main) {
                    _isDataPreloaded.value = true
                }
            }
        }
    }
}