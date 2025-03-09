package com.healthtech.doccareplus.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.domain.model.User
import com.healthtech.doccareplus.domain.repository.AuthRepository
import com.healthtech.doccareplus.domain.repository.UserRepository
import com.healthtech.doccareplus.domain.service.CloudinaryService
import com.healthtech.doccareplus.domain.service.CloudinaryUploadState
import com.healthtech.doccareplus.ui.profile.editprofile.EmailChangeState
import com.healthtech.doccareplus.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val cloudinaryService: CloudinaryService
) : ViewModel() {
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState = _profileState.asStateFlow()

    private val _emailChangeState = MutableStateFlow<EmailChangeState>(EmailChangeState.Idle)
    val emailChangeState: StateFlow<EmailChangeState> = _emailChangeState

    private val _pendingEmail = MutableStateFlow<String?>(null)
    val pendingEmail: StateFlow<String?> = _pendingEmail

    private val _avatarUploadState =
        MutableStateFlow<CloudinaryUploadState>(CloudinaryUploadState.Idle)
    val avatarUploadState: StateFlow<CloudinaryUploadState> = _avatarUploadState

    private val _updateProfileState = MutableStateFlow<UpdateProfileState>(UpdateProfileState.Idle)
    val updateProfileState: StateFlow<UpdateProfileState> = _updateProfileState

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

        checkPendingEmailChange()
    }

    fun updateEmail(currentPassword: String, newEmail: String) {
        viewModelScope.launch {
            _emailChangeState.value = EmailChangeState.Loading
            authRepository.updateEmail(currentPassword, newEmail)
                .onSuccess {
                    _emailChangeState.value =
                        EmailChangeState.Success("Email xác minh đã được gửi đến $newEmail")
                    _pendingEmail.value = newEmail
                    checkPendingEmailChange()
                }
                .onFailure { exception ->
                    _emailChangeState.value =
                        EmailChangeState.Error(exception.message ?: "Lỗi không xác định")
                }
        }
    }

    fun cancelEmailChange() {
        viewModelScope.launch {
            _emailChangeState.value = EmailChangeState.Loading
            authRepository.cancelEmailChange()
                .onSuccess {
                    _emailChangeState.value = EmailChangeState.Success("Đã hủy thay đổi email")
                    _pendingEmail.value = null
                }
                .onFailure { exception ->
                    _emailChangeState.value =
                        EmailChangeState.Error(exception.message ?: "Lỗi không xác định")
                }
        }
    }

    private fun checkPendingEmailChange() {
        viewModelScope.launch {
            authRepository.checkPendingEmailChange()
                .onSuccess { email ->
                    _pendingEmail.value = email
                }
        }
    }

    fun uploadAvatar(imageUri: Uri) {
        viewModelScope.launch {
            _avatarUploadState.value = CloudinaryUploadState.Loading()

            cloudinaryService.uploadImage(
                imageUri = imageUri,
                folder = Constants.CLOUDINARY_FOLDER_STORE_AVATAR,
                fileName = userRepository.getCurrentUserId(),
                overwrite = true
            ).collectLatest { state ->
                _avatarUploadState.value = state

                if (state is CloudinaryUploadState.Success) {
                    updateUserAvatar(state.imageUrl)
                }
            }
        }
    }

    private fun updateUserAvatar(avatarUrl: String) {
        viewModelScope.launch {
            userRepository.updateUserAvatar(avatarUrl).onFailure { error ->
                _avatarUploadState.value = CloudinaryUploadState.Error(
                    "Tải ảnh thành công nhưng cập nhật thông tin thất bại: ${error.message ?: "Lỗi không xác định"}"
                )
            }
        }
    }

    fun resetAvatarUploadState() {
        _avatarUploadState.value = CloudinaryUploadState.Idle
    }

    fun updateProfile(updatedUser: User) {
        viewModelScope.launch {
            _updateProfileState.value = UpdateProfileState.Loading

            userRepository.updateUserProfile(updatedUser)
                .onSuccess {
                    _updateProfileState.value =
                        UpdateProfileState.Success("Cập nhật thông tin thành công")
                }
                .onFailure { error ->
                    _updateProfileState.value =
                        UpdateProfileState.Error(error.message ?: "Lỗi không xác định")
                }
        }
    }

    fun resetUpdateProfileState() {
        _updateProfileState.value = UpdateProfileState.Idle
    }
}