package com.healthtech.doccareplus.data.repository

import com.healthtech.doccareplus.data.remote.api.AuthApi
import com.healthtech.doccareplus.data.remote.api.FirebaseApi
import com.healthtech.doccareplus.domain.model.User
import com.healthtech.doccareplus.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firebaseApi: FirebaseApi,
    private val authApi: AuthApi
) : UserRepository {
    override fun observeCurrentUser(): Flow<Result<User>> {
        val currentUserId =
            authApi.getCurrentUserId() ?: return flowOf(Result.failure(Exception("Not logged in")))
        return firebaseApi.observeUser(currentUserId)
    }
}