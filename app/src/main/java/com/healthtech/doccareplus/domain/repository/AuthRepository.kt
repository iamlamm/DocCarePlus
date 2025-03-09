package com.healthtech.doccareplus.domain.repository

import com.healthtech.doccareplus.domain.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String, rememberMe: Boolean): Result<User>

    suspend fun register(
        name: String, email: String, password: String, phoneNumber: String, avatar: String
    ): Result<User>

    suspend fun resetPassword(email: String): Result<Unit>

    fun logout()

    suspend fun updateEmail(
        currentPassword: String,
        newEmail: String
    ): Result<Unit>
    
    suspend fun cancelEmailChange(): Result<Unit>
    
    suspend fun checkPendingEmailChange(): Result<String?>
}