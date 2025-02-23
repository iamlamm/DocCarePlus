package com.healthtech.doccareplus.ui.profile

import com.healthtech.doccareplus.domain.model.User

sealed class ProfileState {
    data object Idle : ProfileState()
    data class Success(val user: User) : ProfileState()
    data class Error(val message: String) : ProfileState()
}