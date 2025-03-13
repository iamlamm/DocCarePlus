package com.healthtech.doccareplus.ui.notification.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.domain.model.Notification
import com.healthtech.doccareplus.domain.repository.UserRepository
import com.healthtech.doccareplus.domain.service.NotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationService: NotificationService,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            
            val userId = userRepository.getCurrentUserId()
            if (userId == null) {
                _error.value = "Người dùng chưa đăng nhập"
                _isLoading.value = false
                return@launch
            }

            notificationService.observeNotifications(userId)
                .catch { e ->
                    _error.value = e.message ?: "Đã xảy ra lỗi"
                    _isLoading.value = false
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { notifications ->
                            _notifications.value = notifications
                            _error.value = null
                        },
                        onFailure = { e ->
                            _error.value = e.message ?: "Đã xảy ra lỗi"
                        }
                    )
                    _isLoading.value = false
                }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId()
                if (userId != null) {
                    notificationService.markAsRead(notificationId, userId)
                } else {
                    _error.value = "Người dùng chưa đăng nhập"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Đã xảy ra lỗi"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}