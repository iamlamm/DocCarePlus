package com.healthtech.doccareplus.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.data.local.preferences.UserPreferences
import com.healthtech.doccareplus.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences
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

    fun updateRememberMe(isChecked: Boolean){
        userPreferences.saveRememberMe(isChecked)
        _rememberMeState.value = isChecked
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }
}