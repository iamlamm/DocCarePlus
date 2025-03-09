package com.healthtech.doccareplus.domain.repository

import com.healthtech.doccareplus.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeCurrentUser(): Flow<Result<User>>

    fun getCurrentUserId(): String?

    suspend fun updateUserAvatar(avatarUrl: String): Result<Unit>

    suspend fun updateUserProfile(user: User): Result<Unit>
}