package com.healthtech.doccareplus.domain.repository

import com.healthtech.doccareplus.domain.model.Appointment
import com.healthtech.doccareplus.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeCurrentUser(): Flow<Result<User>>

    fun getCurrentUserId(): String?

    suspend fun getCurrentUser(): User?

    suspend fun updateUserAvatar(avatarUrl: String): Result<Unit>

    suspend fun updateUserProfile(user: User): Result<Unit>

    suspend fun getUserAppointments(userId: String): Flow<Result<List<Appointment>>>

    suspend fun updateFCMToken(token: String): Result<Unit>
}