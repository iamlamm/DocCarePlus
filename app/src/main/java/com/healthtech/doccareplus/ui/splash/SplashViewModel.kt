package com.healthtech.doccareplus.ui.splash

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.data.local.preferences.UserPreferences
import com.healthtech.doccareplus.domain.repository.CategoryRepository
import com.healthtech.doccareplus.domain.repository.DoctorRepository
import com.zegocloud.zimkit.services.ZIMKit
import dagger.hilt.android.lifecycle.HiltViewModel
import im.zego.zim.enums.ZIMErrorCode
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
    
    // Trạng thái preload login resources
    private val _loginResourcesPreloaded = MutableStateFlow(false)
    val loginResourcesPreloaded = _loginResourcesPreloaded.asStateFlow()

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            val isLoggedIn = userPreferences.isUserLoggedIn()
            val destination = if (isLoggedIn) {
                // Reconnect ZIMKit nếu user đã đăng nhập từ trước
                userPreferences.getUser()?.let { user ->
                    reconnectZIMKit(user.id.toString(), user.name)
                }
                
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
    
    private fun reconnectZIMKit(userId: String, userName: String) {
        val avatarUrl = "https://storage.zego.im/IMKit/avatar/avatar-0.png"
        
        // Không thể kiểm tra trực tiếp trạng thái đăng nhập, 
        // nên luôn thử kết nối lại - ZIMKit sẽ tự xử lý
        ZIMKit.connectUser(userId, userName, avatarUrl) { error ->
            if (error.code != ZIMErrorCode.SUCCESS) {
                Log.e("SplashViewModel", "ZIMKit reconnect failed: ${error.message}")
            } else {
                Log.d("SplashViewModel", "ZIMKit reconnect success with userId: $userId")
            }
        }
    }
    
    fun preloadLoginResources() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Tải trước các tài nguyên cần thiết cho LoginFragment
                
                // 1. Tải trước các cấu hình đăng nhập (nếu có)
//                userPreferences.getUser()
                
                // 2. Tải trước các tài nguyên hình ảnh (nếu có)
                withContext(Dispatchers.Main) {
                    // Giúp tải trước các tài nguyên UI
                    // Một số cách tiếp cận có thể là:
                    // - Sử dụng Glide/Picasso để tải trước hình ảnh
                    // - Inflate layouts không hiển thị để cache chúng
                }
                
                // 3. Tải trước language resources hoặc các thông tin khác
                
                _loginResourcesPreloaded.value = true
            } catch (e: Exception) {
                // Log lỗi nếu có
                _loginResourcesPreloaded.value = true // Vẫn tiếp tục chuyển màn hình
            }
        }
    }
    
    fun preLoadHomeData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val categoriesJob = launch { categoryRepository.observeCategories().first() }
                val doctorsJob = launch { doctorRepository.observeDoctors().first() }
                categoriesJob.join()
                doctorsJob.join()
                _isDataPreloaded.value = true
            } catch (e: Exception) {
                _isDataPreloaded.value = true
            }
        }
    }
}