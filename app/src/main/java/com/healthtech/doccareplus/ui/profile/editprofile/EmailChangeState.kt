package com.healthtech.doccareplus.ui.profile.editprofile

sealed class EmailChangeState {
    object Idle : EmailChangeState()
    object Loading : EmailChangeState()
    data class Success(val message: String) : EmailChangeState()
    data class Error(val message: String) : EmailChangeState()
}