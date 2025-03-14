package com.healthtech.doccareplus.data.repository

import com.healthtech.doccareplus.data.remote.api.AuthApi
import com.healthtech.doccareplus.data.remote.api.FirebaseApi
import com.healthtech.doccareplus.domain.model.Appointment
import com.healthtech.doccareplus.domain.model.User
import com.healthtech.doccareplus.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import timber.log.Timber
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

    override fun getCurrentUserId(): String? {
        return authApi.getCurrentUserId()
    }

    override suspend fun getCurrentUser(): User? {
        val currentUserId = authApi.getCurrentUserId() ?: return null
        return try {
            firebaseApi.getUser(currentUserId).getOrNull()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get current user")
            null
        }
    }

    override suspend fun getUserAppointments(userId: String): Flow<Result<List<Appointment>>> {
        return firebaseApi.observeUserAppointments(userId)
            .map { result ->
                result.map { appointments ->
                    // Thêm thông tin bác sĩ cho mỗi cuộc hẹn
                    appointments.mapNotNull { appointment ->
                        try {
                            // Cố gắng lấy thông tin bác sĩ cho mỗi cuộc hẹn
                            val doctor = firebaseApi.getDoctor(appointment.doctorId).getOrNull()
                            if (doctor != null) {
                                // Cập nhật thông tin bác sĩ trong cuộc hẹn
                                appointment.copy(
                                    doctorName = doctor.name,
                                    doctorAvatar = doctor.avatar ?: ""
                                )
                            } else {
                                appointment
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to get doctor for appointment ${appointment.id}")
                            appointment
                        }
                    }
                }
            }
    }

    override suspend fun updateUserAvatar(avatarUrl: String): Result<Unit> {
        val currentUserId = authApi.getCurrentUserId() 
            ?: return Result.failure(Exception("Người dùng chưa đăng nhập"))
        
        return firebaseApi.updateUserField(
            userId = currentUserId,
            fieldName = "avatar",
            fieldValue = avatarUrl
        )
    }

    override suspend fun updateUserProfile(user: User): Result<Unit> = runCatching {
        val currentUserId = authApi.getCurrentUserId() 
            ?: throw Exception("Người dùng chưa đăng nhập")
        
        // Đảm bảo ID người dùng trong object trùng với ID đang đăng nhập
        if (user.id != currentUserId) {
            throw Exception("ID người dùng không hợp lệ")
        }

        // Cập nhật tên
        firebaseApi.updateUserField(
            userId = currentUserId, 
            fieldName = "name", 
            fieldValue = user.name
        ).getOrThrow()
        
        // Cập nhật số điện thoại (nếu có)
        user.phoneNumber?.let {
            firebaseApi.updateUserField(
                userId = currentUserId,
                fieldName = "phoneNumber",
                fieldValue = it
            ).getOrThrow()
        }
        
        // Cập nhật thông tin giới thiệu (about)
        firebaseApi.updateUserField(
            userId = currentUserId,
            fieldName = "about",
            fieldValue = user.about ?: ""
        ).getOrThrow()
        
        // Cập nhật chiều cao (height)
        firebaseApi.updateUserField(
            userId = currentUserId,
            fieldName = "height",
            fieldValue = user.height ?: 0
        ).getOrThrow()
        
        // Cập nhật cân nặng (weight)
        firebaseApi.updateUserField(
            userId = currentUserId,
            fieldName = "weight",
            fieldValue = user.weight ?: 0
        ).getOrThrow()
        
        // Cập nhật tuổi (age)
        user.age?.let {
            firebaseApi.updateUserField(
                userId = currentUserId,
                fieldName = "age",
                fieldValue = it
            ).getOrThrow()
        }
        
        // Cập nhật nhóm máu (bloodType)
        user.bloodType?.let {
            firebaseApi.updateUserField(
                userId = currentUserId,
                fieldName = "bloodType",
                fieldValue = it
            ).getOrThrow()
        }

        // Cập nhật giới tính
        user.gender?.let {
            firebaseApi.updateUserField(
                userId = currentUserId,
                fieldName = "gender",
                fieldValue = it.name
            ).getOrThrow()
        }
        
        // Ghi log thành công
        Timber.d("Cập nhật thông tin người dùng thành công: ${user.name}")
        
        Unit
    }
}