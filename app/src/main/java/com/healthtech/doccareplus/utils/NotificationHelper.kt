package com.healthtech.doccareplus.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.domain.model.Notification
import com.healthtech.doccareplus.domain.model.NotificationType
import com.healthtech.doccareplus.ui.home.HomeActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val CHANNEL_APPOINTMENTS = "channel_appointments"
        private const val CHANNEL_SYSTEM = "channel_system"
    }

    init {
        createNotificationChannels()
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Kênh thông báo lịch hẹn
            val appointmentChannel = NotificationChannel(
                CHANNEL_APPOINTMENTS,
                "Thông báo lịch hẹn",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo cho các lịch hẹn, nhắc nhở và thay đổi"
            }

            // Kênh thông báo hệ thống
            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM,
                "Thông báo hệ thống",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Thông báo hệ thống và cập nhật"
            }

            // Đăng ký các kênh thông báo
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(
                listOf(
                    appointmentChannel,
                    systemChannel
                )
            )
        }
    }

    fun showNotification(notification: Notification) {
        // Kiểm tra quyền
        if (!PermissionManager.hasNotificationPermission(context)) {
            Timber.e("No notification permission")
            return
        }

        // Chọn kênh thông báo dựa vào loại
        val channelId = when (notification.type) {
            NotificationType.APPOINTMENT_BOOKED,
            NotificationType.NEW_APPOINTMENT,
            NotificationType.APPOINTMENT_CANCELLED,
            NotificationType.APPOINTMENT_REMINDER -> CHANNEL_APPOINTMENTS

            NotificationType.SYSTEM -> CHANNEL_SYSTEM
            NotificationType.APPOINTMENT_COMPLETED -> TODO()
            NotificationType.ADMIN_NEW_APPOINTMENT -> TODO()
        }

        // Tạo intent để mở màn hình chính
        val intent = Intent(context, HomeActivity::class.java).apply {
            putExtra("OPEN_NOTIFICATIONS", true)
            putExtra("NOTIFICATION_ID", notification.id)
            putExtra("APPOINTMENT_ID", notification.appointmentId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Tạo intent cho nút "Xem chi tiết"
        val detailIntent = Intent(context, HomeActivity::class.java).apply {
            putExtra("OPEN_NOTIFICATIONS", true)
            putExtra("NOTIFICATION_ID", notification.id)
            putExtra("APPOINTMENT_ID", notification.appointmentId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val detailPendingIntent = PendingIntent.getActivity(
            context,
            (notification.id + "_detail").hashCode(),
            detailIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Xây dựng thông báo với style mở rộng
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            // Thêm style mở rộng
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notification.message)
            )
            // Thêm nút xem chi tiết
            .addAction(R.drawable.calendar_menu, "Xem chi tiết", detailPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(
                notification.id.hashCode(),
                builder.build()
            )
            Timber.d("Notification displayed successfully")
        } catch (e: SecurityException) {
            Timber.e("Error showing notification", e)
            e.printStackTrace()
        }
    }
}