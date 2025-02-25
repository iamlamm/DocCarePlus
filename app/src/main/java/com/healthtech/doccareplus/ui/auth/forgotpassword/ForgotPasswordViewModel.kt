package com.healthtech.doccareplus.ui.auth.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _forgotPasswordState =
        MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val forgotPasswordState = _forgotPasswordState.asStateFlow()

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _forgotPasswordState.value = ForgotPasswordState.Loading
            try {
                val result = authRepository.resetPassword(email)
                if (result.isSuccess) {
                    _forgotPasswordState.value = ForgotPasswordState.Success
                } else {
                    _forgotPasswordState.value = ForgotPasswordState.Error(
                        result.exceptionOrNull()?.message ?: "Không thể gửi Email khôi phục"
                    )
                }
            } catch (e: Exception) {
                _forgotPasswordState.value = ForgotPasswordState.Error(e.message ?: "Đã xảy ra lỗi")
            }
        }
    }

    fun resetForgotPasswordState() {
        _forgotPasswordState.value = ForgotPasswordState.Idle
    }
}