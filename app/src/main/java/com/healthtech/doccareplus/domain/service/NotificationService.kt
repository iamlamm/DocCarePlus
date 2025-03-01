package com.healthtech.doccareplus.domain.service

import com.healthtech.doccareplus.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationService {
    suspend fun createNotification(notification: Notification)

    fun observeNotifications(userId: String): Flow<Result<List<Notification>>>

    suspend fun markAsRead(notificationId: String)
}