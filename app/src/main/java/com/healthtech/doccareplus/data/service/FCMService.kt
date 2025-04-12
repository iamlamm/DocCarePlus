package com.healthtech.doccareplus.data.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.healthtech.doccareplus.domain.model.Notification
import com.healthtech.doccareplus.domain.model.NotificationType
import com.healthtech.doccareplus.domain.repository.UserRepository
import com.healthtech.doccareplus.utils.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FCMService : FirebaseMessagingService() {
    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var userRepository: UserRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("New FCM token: $token")
        serviceScope.launch {
            try {
                val result = userRepository.updateFCMToken(token)
                if (result.isSuccess) {
                    Timber.d("FCM token updated successfully")
                } else {
                    Timber.e("Failed to update FCM token: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating FCM token")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        try {
            val data = message.data
            val notification = Notification(
                id = data["notification_id"] ?: System.currentTimeMillis().toString(),
                title = data["title"] ?: message.notification?.title ?: "",
                message = data["message"] ?: message.notification?.body ?: "",
                time = System.currentTimeMillis(),
                type = try {
                    NotificationType.valueOf(data["type"] ?: "SYSTEM")
                } catch (e: Exception) {
                    NotificationType.SYSTEM
                },
                appointmentId = data["appointment_id"] ?: "",
                date = data["date"] ?: ""
            )

            notificationHelper.showNotification(notification)
            Timber.d("Received FCM notification: ${notification.title}")
        } catch (e: Exception) {
            Timber.e(e, "Error processing FCM message")
        }
    }
}