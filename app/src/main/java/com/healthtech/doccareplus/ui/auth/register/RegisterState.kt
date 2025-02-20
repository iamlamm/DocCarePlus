package com.healthtech.doccareplus.ui.auth.register

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    object Success : RegisterState()
    object EmailVerificationSent : RegisterState()

    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : RegisterState()
}