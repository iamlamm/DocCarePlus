package com.healthtech.doccareplus.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.domain.repository.AuthRepository
import com.healthtech.doccareplus.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState = _profileState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.observeCurrentUser()
                .collect { result ->
                    result.onSuccess { user ->
                        _profileState.value = ProfileState.Success(user)
                    }.onFailure { error ->
                        _profileState.value =
                            ProfileState.Error(error.message ?: "Lỗi không xác định")
                    }
                }
        }
    }

    fun logout() {
        authRepository.logout()
    }
}