package com.healthtech.doccareplus.utils

import android.util.Log
import androidx.navigation.NavController

/**
 * Thực hiện navigation an toàn cho fragment-to-fragment navigation,
 * tránh crash khi destination không hợp lệ hoặc người dùng click quá nhanh
 */
fun NavController.safeNavigate(actionId: Int) {
    try {
        val action = currentDestination?.getAction(actionId)
        if (action != null) {
            navigate(actionId)
        } else {
            Log.w(
                "Navigation",
                "Action $actionId không tồn tại ở destination: ${currentDestination?.label}"
            )
        }
    } catch (e: Exception) {
        Log.e("Navigation", "Không thể navigate: ${e.message}")
    }
}

/**
 * Thực hiện navigation an toàn cho global navigation (như bottom navigation),
 * cho phép navigate trực tiếp đến destination
 */
fun NavController.safeNavigateGlobal(destinationId: Int) {
    try {
        navigate(destinationId)
    } catch (e: Exception) {
        Log.e("Navigation", "Không thể navigate đến destination: ${e.message}")
    }
}