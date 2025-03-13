package com.healthtech.doccareplus.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 101
        private const val CALL_PERMISSION_REQUEST_CODE = 102

        val CALL_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )

        val VIDEO_CALL_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )

        val VOICE_CALL_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )

        val PHONE_CALL_PERMISSIONS = arrayOf(
            Manifest.permission.CALL_PHONE
        )
        
        val NOTIFICATION_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf()
        }

        fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(context, permission) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
            return true
        }

        fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int = PERMISSION_REQUEST_CODE) {
            ActivityCompat.requestPermissions(
                activity,
                permissions,
                requestCode
            )
        }

        fun requestNotificationPermissions(activity: Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }

        fun requestPhoneCallPermissions(activity: Activity) {
            requestPermissions(activity, PHONE_CALL_PERMISSIONS, CALL_PERMISSION_REQUEST_CODE)
        }

        fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Với Android < 13, không cần xin quyền rõ ràng
            }
        }
    }
}