package com.healthtech.doccareplus.ui.profile

sealed class UpdateProfileState {
    data object Idle : UpdateProfileState()
    data object Loading : UpdateProfileState()
    data class Success(val message: String) : UpdateProfileState()
    data class Error(val message: String) : UpdateProfileState()
}