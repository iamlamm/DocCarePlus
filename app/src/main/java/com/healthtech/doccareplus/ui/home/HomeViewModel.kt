package com.healthtech.doccareplus.ui.home

import android.util.Log
import android.view.View
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.common.base.BaseDataViewModel
import com.healthtech.doccareplus.databinding.ActivityHomeBinding
import com.healthtech.doccareplus.domain.model.Gender
import com.healthtech.doccareplus.domain.model.User
import com.healthtech.doccareplus.domain.repository.CategoryRepository
import com.healthtech.doccareplus.domain.repository.DoctorRepository
import com.healthtech.doccareplus.domain.repository.UserRepository
import com.healthtech.doccareplus.domain.service.NotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val doctorRepository: DoctorRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService
) : BaseDataViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    // Trạng thái theo dõi việc tải dữ liệu ban đầu
    private val _isInitialLoadComplete = MutableStateFlow(false)
    val isInitialLoadComplete = _isInitialLoadComplete.asStateFlow()

    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount

    // Lưu ID cuộc hẹn được chọn từ thông báo
    private val _selectedAppointmentId = MutableStateFlow<String?>(null)
    val selectedAppointmentId = _selectedAppointmentId.asStateFlow()

    init {
        // Khởi động tải dữ liệu ngay khi ViewModel được tạo
        observeData()
        observeUnreadNotifications()
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

//            // Chuẩn bị resource ID avatar dựa trên gender
//            val avatarResId = when (user.gender) {
//                Gender.MALE -> R.mipmap.avatar_male_default
//                Gender.FEMALE -> R.mipmap.avatar_female_default
//                else -> R.mipmap.avatar_bear_default
//            }
//
//            // Trả về thông tin cần thiết để Activity/Fragment có thể load avatar
//            if (user.avatar.isNullOrEmpty()) {
//                // Sử dụng avatar mặc định
//                ivUserAvatar.setImageResource(avatarResId)
//            } else {
//                // Để Activity/Fragment xử lý việc load ảnh
//                // Glide.with(ivUserAvatar).load(user.avatar).error(avatarResId).into(ivUserAvatar)
//                // Thay vào đó, chỉ set tag để sử dụng bên ngoài
//                ivUserAvatar.tag = user.avatar
//                ivUserAvatar.setImageResource(avatarResId) // Default trước khi load
//            }

            if (!user.avatar.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(user.avatar)
                    .placeholder(R.mipmap.avatar_bear_default)
                    .error(R.mipmap.avatar_bear_default)
                    .circleCrop()
                    .into(binding.ivUserAvatar)
            } else {
                val avatarResId = when (user.gender) {
                    Gender.MALE -> R.mipmap.avatar_male_default
                    Gender.FEMALE -> R.mipmap.avatar_female_default
                    else -> R.mipmap.avatar_bear_default
                }
                ivUserAvatar.tag = null
                ivUserAvatar.setImageResource(avatarResId)
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

    /**
     * Preload các tài nguyên cần thiết cho màn hình AllCategories
     * Giúp chuyển màn hình mượt mà hơn
     */
    fun preloadCategoriesScreen() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Kích hoạt flow observeCategories nhưng chỉ lấy giá trị đầu tiên (local data)
                categoryRepository.observeCategories().first()

                // Không cần thiết phải refreshCategories() vì:
                // 1. observeCategories() đã tự động fetch từ remote sau khi emit local
                // 2. Nếu vẫn muốn refresh, có thể làm như sau:
                /*
                launch { 
                    // Gọi phương thức refresh từ BaseDataViewModel
                    refreshCategories(categoryRepository)
                }
                */
            } catch (e: Exception) {
                // Bỏ qua lỗi khi preload - không ảnh hưởng UX
            }
        }
    }

    /**
     * Preload các tài nguyên cần thiết cho màn hình AllDoctors
     * Giúp chuyển màn hình mượt mà hơn
     */
    fun preloadDoctorsScreen() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Tương tự như categories
                doctorRepository.observeDoctors().first()
            } catch (e: Exception) {
                // Bỏ qua lỗi khi preload
            }
        }
    }

    private fun observeUnreadNotifications() {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId() ?: return@launch
            notificationService.observeNotifications(userId)
                .collect { result ->
                    result.onSuccess { notifications ->
                        _unreadNotificationCount.value = notifications.count { !it.read }
                    }
                    result.onFailure { error ->
                        Log.e("observeUnreadNotifications", error.message ?: "Unknow error")
                    }
                }
        }
    }

    fun updateNotificationBadge(binding: ActivityHomeBinding) {
        binding.tvNotificationBadge.apply {
            val count = unreadNotificationCount.value
            visibility = if (count > 0) View.VISIBLE else View.GONE
            text = if (count > 99) "99+" else count.toString()
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                // Lấy ID người dùng hiện tại
                val userId = currentUser.value?.id ?: return@launch
                
                // Gọi service để đánh dấu thông báo đã đọc
                notificationService.markAsRead(userId, notificationId)
                
                // Cập nhật lại số lượng thông báo chưa đọc
                refreshUnreadNotifications()
            } catch (e: Exception) {
                // Xử lý lỗi nếu cần
                Log.e("HomeViewModel", "Error marking notification as read", e)
            }
        }
    }

    // Thêm phương thức refresh nếu chưa có
    fun refreshUnreadNotifications() {
        viewModelScope.launch {
            observeUnreadNotifications()
        }
    }

    fun setSelectedAppointmentId(id: String) {
        _selectedAppointmentId.value = id
    }

    /**
     * Preload các tài nguyên cần thiết cho màn hình Appointments
     * Giúp chuyển màn hình mượt mà hơn
     */
    fun preloadAppointmentsScreen() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Lấy người dùng hiện tại một cách đồng bộ
                val user = userRepository.getCurrentUser()
                
                // Nếu có người dùng, preload cuộc hẹn của họ
                user?.let { currentUser ->
                    // Chỉ lấy giá trị đầu tiên từ flow để preload dữ liệu
                    userRepository.getUserAppointments(currentUser.id).first()
                }
            } catch (e: Exception) {
                // Bỏ qua lỗi khi preload - không ảnh hưởng UX
                Log.e("HomeViewModel", "Error preloading appointments", e)
            }
        }
    }
}