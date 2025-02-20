package com.healthtech.doccareplus.data.repository

import com.healthtech.doccareplus.data.local.preferences.UserPreferences
import com.healthtech.doccareplus.data.remote.api.AuthApi
import com.healthtech.doccareplus.domain.model.User
import com.healthtech.doccareplus.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val userPreferences: UserPreferences
) : AuthRepository {

    override suspend fun login(email: String, password: String, rememberMe: Boolean): Result<User> {
        return try {
            val result = authApi.login(email, password)
            if (result.isSuccess) {
                result.getOrNull()?.let { user ->
                    if (rememberMe) {
                        userPreferences.saveUser(user)

                    } else {
                        userPreferences.clearUser()
                    }
                }
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        phoneNumber: String
    ): Result<User> {
        return try {
            val result = authApi.register(name, email, password, phoneNumber)
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            val result = authApi.resetPassword(email)
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override fun getCurrentUser(): User? {
        return userPreferences.getUser()
    }

    override fun logout() {
        userPreferences.clearUser()
        authApi.signOut()
    }
}