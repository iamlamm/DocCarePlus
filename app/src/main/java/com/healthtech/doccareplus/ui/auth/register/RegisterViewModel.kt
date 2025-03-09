package com.healthtech.doccareplus.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.data.local.preferences.UserPreferences
import com.healthtech.doccareplus.domain.model.User
import com.healthtech.doccareplus.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState = _registerState.asStateFlow()

    private val defaultAvatars = listOf(
        "https://res.cloudinary.com/daull03yv/image/upload/v1741287119/bear2_c14hzy.png",
        "https://res.cloudinary.com/daull03yv/image/upload/v1741287120/koala_od4pk3.png",
        "https://res.cloudinary.com/daull03yv/image/upload/v1741287119/polar_bear_q7xdyz.png"
    )

    private fun getRandomAvatar(): String {
        return defaultAvatars.random()
    }

    fun register(name: String, email: String, password: String, phoneNumber: String) {
        if (_registerState.value is RegisterState.Loading) {
            return
        }
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            try {
                val randomAvatar = getRandomAvatar()
                val result =
                    authRepository.register(name, email, password, phoneNumber, randomAvatar)
                if (result.isSuccess) {
                    _registerState.value = RegisterState.EmailVerificationSent
                } else {
                    _registerState.value = RegisterState.Error(
                        message = result.exceptionOrNull()?.message ?: "Đăng ký thất bại"
                    )
                }
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error(
                    message = e.message ?: "Đã xảy ra lỗi",
                    throwable = e
                )
            }
        }
    }

    fun resetRegisterState() {
        _registerState.value = RegisterState.Idle
    }
}