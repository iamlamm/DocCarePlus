package com.healthtech.doccareplus.data.service

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.healthtech.doccareplus.domain.model.Notification
import com.healthtech.doccareplus.domain.model.NotificationType
import com.healthtech.doccareplus.domain.service.NotificationService
import com.healthtech.doccareplus.utils.NotificationHelper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationServiceImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val notificationHelper: NotificationHelper
) : NotificationService {

    override suspend fun createUserNotification(
        notification: Notification,
        userId: String
    ): Result<String> {
        return try {
            val notificationsRef = database.getReference("notifications/users/$userId")
            val newKey = notificationsRef.push().key
                ?: return Result.failure(Exception("Failed to create notification key"))

            val notificationMap = hashMapOf(
                "id" to newKey,
                "title" to notification.title,
                "message" to notification.message,
                "time" to notification.time,
                "read" to notification.read,
                "type" to notification.type.name,
                "date" to notification.date,
                "appointmentId" to notification.appointmentId
            )

            notificationsRef.child(newKey).setValue(notificationMap).await()

            notificationHelper.showNotification(notification)

            Result.success(newKey)
        } catch (e: Exception) {
            Timber.e("Error creating user notification: " + e.message)
            Result.failure(e)
        }
    }

    override suspend fun createDoctorNotification(
        notification: Notification,
        doctorId: String
    ): Result<String> {
        return try {
            val notificationsRef = database.getReference("notifications/doctors/$doctorId")
            val newKey = notificationsRef.push().key
                ?: return Result.failure(Exception("Failed to create notification key"))

            val notificationMap = hashMapOf(
                "id" to newKey,
                "title" to notification.title,
                "message" to notification.message,
                "time" to notification.time,
                "read" to notification.read,
                "type" to notification.type.name,
                "date" to notification.date,
                "appointmentId" to notification.appointmentId
            )

            notificationsRef.child(newKey).setValue(notificationMap).await()
            Result.success(newKey)
        } catch (e: Exception) {
            Timber.e("Error creating doctor notification: " + e.message)
            Result.failure(e)
        }
    }

    override fun observeNotifications(userId: String): Flow<Result<List<Notification>>> =
        callbackFlow {
            val notificationsRef = database.getReference("notifications/users/$userId")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val notifications = snapshot.children.mapNotNull { notificationSnapshot ->
                            try {
                                Notification(
                                    id = notificationSnapshot.child("id")
                                        .getValue(String::class.java) ?: "",
                                    title = notificationSnapshot.child("title")
                                        .getValue(String::class.java) ?: "",
                                    message = notificationSnapshot.child("message")
                                        .getValue(String::class.java) ?: "",
                                    time = notificationSnapshot.child("time")
                                        .getValue(Long::class.java) ?: 0L,
                                    read = notificationSnapshot.child("read")
                                        .getValue(Boolean::class.java) ?: false,
                                    type = try {
                                        val typeString = notificationSnapshot.child("type")
                                            .getValue(String::class.java) ?: "SYSTEM"
                                        NotificationType.valueOf(typeString)
                                    } catch (e: Exception) {
                                        NotificationType.SYSTEM
                                    },
                                    date = notificationSnapshot.child("date")
                                        .getValue(String::class.java) ?: "",
                                    appointmentId = notificationSnapshot.child("appointmentId")
                                        .getValue(String::class.java) ?: ""
                                )
                            } catch (e: Exception) {
                                Timber.e("Error parsing notification: " + e.message)
                                null
                            }
                        }.sortedByDescending { it.time }

                        trySend(Result.success(notifications))
                    } catch (e: Exception) {
                        trySend(Result.failure(e))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(Result.failure(error.toException()))
                }
            }

            notificationsRef.addValueEventListener(listener)
            awaitClose { notificationsRef.removeEventListener(listener) }
        }

    override suspend fun markAsRead(notificationId: String, userId: String): Result<Unit> {
        return try {
            val notificationRef =
                database.getReference("notifications/users/$userId/$notificationId")
            notificationRef.child("read").setValue(true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e("Error marking notification as read: " + e.message)
            Result.failure(e)
        }
    }

    override suspend fun createAdminNotification(notification: Notification) {
        try {
            val notificationsRef = database.getReference("notifications/admin")
            val notificationKey = notificationsRef.push().key ?: return

            val notificationMap = hashMapOf(
                "id" to notificationKey,
                "title" to notification.title,
                "message" to notification.message,
                "time" to notification.time,
                "read" to notification.read,
                "type" to notification.type.name,
                "date" to notification.date,
                "appointmentId" to notification.appointmentId
            )

            notificationsRef.child(notificationKey)
                .setValue(notificationMap)
                .await()

            database.getReference("adminStats/unreadNotifications")
                .setValue(ServerValue.increment(1))
                .await()

        } catch (e: Exception) {
            Timber.tag("NotificationService").e("Error creating admin notification: %s", e.message)
        }
    }
}