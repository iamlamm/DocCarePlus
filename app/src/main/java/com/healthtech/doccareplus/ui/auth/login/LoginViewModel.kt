package com.healthtech.doccareplus.ui.auth.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.DocCarePlusApplication
import com.healthtech.doccareplus.data.local.preferences.UserPreferences
import com.healthtech.doccareplus.domain.repository.AuthRepository
import com.zegocloud.zimkit.services.ZIMKit
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import im.zego.zim.enums.ZIMErrorCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState = _loginState.asStateFlow()

    private val _rememberMeState = MutableStateFlow(false)
    val rememberMeState = _rememberMeState.asStateFlow()

    init {
        _rememberMeState.value = userPreferences.isRememberMeChecked()
    }

    fun login(email: String, password: String, rememberMe: Boolean) {
        if (_loginState.value is LoginState.Loading) {
            return
        }
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                userPreferences.saveRememberMe(rememberMe)

                val result = authRepository.login(email, password, rememberMe)
                if (result.isSuccess) {
                    val user = userPreferences.getUser()

                    if (user != null) {
                        val userId = user.id
                        val userName = user.name
                        val userAvatar = user.avatar!!
                        connectToZegoCloud(userId, userName, userAvatar)
                    }

                    _loginState.value = LoginState.Success
                } else {
                    _loginState.value =
                        LoginState.Error(result.exceptionOrNull()?.message ?: "Đăng nhập thất bại")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Đã xảy ra lỗi")
            }
        }
    }

    fun updateRememberMe(isChecked: Boolean) {
        userPreferences.saveRememberMe(isChecked)
        _rememberMeState.value = isChecked
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    private fun connectToZegoCloud(userId: String, userName: String, userAvatar: String) {
        ZIMKit.connectUser(userId, userName, userAvatar) { error ->
            if (error.code != ZIMErrorCode.SUCCESS) {
                Log.e("LoginViewModel", "ZIMKit connect failed: ${error.message}")
            } else {
                Log.d("LoginViewModel", "ZIMKit connect success with userId: $userId")
            }
        }

        val app = context.applicationContext as DocCarePlusApplication
        app.initZegoCallService(userId, userName)
    }
}