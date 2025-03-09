package com.healthtech.doccareplus.data.repository

import com.healthtech.doccareplus.data.local.preferences.UserPreferences
import com.healthtech.doccareplus.data.remote.api.AuthApi
import com.healthtech.doccareplus.domain.model.User
import com.healthtech.doccareplus.domain.repository.AuthRepository
import com.zegocloud.zimkit.services.ZIMKit
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
        phoneNumber: String,
        avatar: String
    ): Result<User> {
        return try {
            val result = authApi.register(name, email, password, phoneNumber, avatar)
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

    override suspend fun updateEmail(
        currentPassword: String,
        newEmail: String
    ): Result<Unit> {
        return try {
            val result = authApi.updateEmail(currentPassword, newEmail)
            // Không cập nhật local storage ngay lập tức
            // Chỉ cập nhật khi email đã được xác thực
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelEmailChange(): Result<Unit> {
        return try {
            authApi.cancelEmailChange()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkPendingEmailChange(): Result<String?> {
        return try {
            authApi.checkPendingEmailChange()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

//    override suspend fun getCurrentUser(): Result<User> {
//        return try {
//            val localUser = userPreferences.getUser()
//            if (localUser != null) {
//                Result.success(localUser)
//            }
//
//            val remoteResult = authApi.fetchCurrentUser()
//            if (remoteResult.isSuccess) {
//                val remoteUser = remoteResult.getOrNull()
//                if (remoteUser != null && remoteUser != localUser) {
//                    userPreferences.saveUser(remoteUser)
//                }
//            }
//            remoteResult
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

    override fun logout() {
        ZIMKit.disconnectUser()
        userPreferences.clearUser()
        authApi.signOut()
    }
}