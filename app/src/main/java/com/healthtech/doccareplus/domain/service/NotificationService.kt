package com.healthtech.doccareplus.domain.service

import com.healthtech.doccareplus.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationService {
    // Tạo thông báo cho user và doctor
    suspend fun createUserNotification(notification: Notification, userId: String): Result<String>
    suspend fun createDoctorNotification(notification: Notification, doctorId: String): Result<String>
    
    // Lắng nghe thông báo của user
    fun observeNotifications(userId: String): Flow<Result<List<Notification>>>
    
    // Đánh dấu đã đọc
    suspend fun markAsRead(notificationId: String, userId: String): Result<Unit>
}