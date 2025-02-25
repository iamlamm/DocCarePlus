package com.healthtech.doccareplus.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.common.base.BaseDataViewModel
import com.healthtech.doccareplus.databinding.ActivityHomeBinding
import com.healthtech.doccareplus.domain.model.Gender
import com.healthtech.doccareplus.domain.model.User
import com.healthtech.doccareplus.domain.repository.CategoryRepository
import com.healthtech.doccareplus.domain.repository.DoctorRepository
import com.healthtech.doccareplus.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val doctorRepository: DoctorRepository,
    private val userRepository: UserRepository
) : BaseDataViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()
    
    // Trạng thái theo dõi việc tải dữ liệu ban đầu
    private val _isInitialLoadComplete = MutableStateFlow(false)
    val isInitialLoadComplete = _isInitialLoadComplete.asStateFlow()

    init {
        // Khởi động tải dữ liệu ngay khi ViewModel được tạo
        observeData()
    }

    private fun observeData() {
        // Tải dữ liệu người dùng (không ưu tiên cao)
        observeUser()
        
        // Tải dữ liệu categories và doctors (ưu tiên cao)
        viewModelScope.launch(Dispatchers.IO) {
            // Sử dụng Dispatchers.IO để không block main thread
            observeCategories(categoryRepository)
            observeDoctors(doctorRepository)
            
            // Đánh dấu đã tải xong dữ liệu ban đầu
            withContext(Dispatchers.Main) {
                _isInitialLoadComplete.value = true
            }
        }
    }

    private fun observeUser() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userRepository.observeCurrentUser().collect { result ->
                    result.onSuccess { user ->
                        withContext(Dispatchers.Main) {
                            _currentUser.value = user
                        }
                    }.onFailure { error ->
                        // Log lỗi nếu cần, nhưng không ảnh hưởng đến luồng chính
                    }
                }
            } catch (e: Exception) {
                // Xử lý ngoại lệ nếu cần
            }
        }
    }

    fun updateUserUI(binding: ActivityHomeBinding, user: User) {
        // Ở đây chỉ cập nhật UI theo data, không xử lý Glide
        binding.apply {
            tvUserName.text = user.name
            
            // Chuẩn bị resource ID avatar dựa trên gender
            val avatarResId = when (user.gender) {
                Gender.MALE -> R.mipmap.avatar_male_default
                Gender.FEMALE -> R.mipmap.avatar_female_default
                else -> R.mipmap.avatar_bear_default
            }
            
            // Trả về thông tin cần thiết để Activity/Fragment có thể load avatar
            if (user.avatar.isNullOrEmpty()) {
                // Sử dụng avatar mặc định
                ivUserAvatar.setImageResource(avatarResId)
            } else {
                // Để Activity/Fragment xử lý việc load ảnh
                // Glide.with(ivUserAvatar).load(user.avatar).error(avatarResId).into(ivUserAvatar)
                // Thay vào đó, chỉ set tag để sử dụng bên ngoài
                ivUserAvatar.tag = user.avatar
                ivUserAvatar.setImageResource(avatarResId) // Default trước khi load
            }
        }
    }

    fun refreshData() {
        // Đặt lại trạng thái tải
        _isInitialLoadComplete.value = false
        
        // Gọi phương thức refresh của BaseDataViewModel
        refreshAllData(categoryRepository, doctorRepository)
        
        // Tải lại thông tin user
        observeUser()
    }

    override fun onCleared() {
        super.onCleared()
        // BaseDataViewModel đã xử lý việc hủy các job và cache
    }
}