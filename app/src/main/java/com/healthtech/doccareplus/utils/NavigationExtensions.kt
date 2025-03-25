package com.healthtech.doccareplus.utils

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.healthtech.doccareplus.utils.AnimationUtils.hideWithAnimation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

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
            Timber.w("Action $actionId không tồn tại ở destination: ${currentDestination?.label}")
        }
    } catch (e: Exception) {
        Timber.e("Không thể navigate: ${e.message}")
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
        Timber.e("Không thể navigate đến destination: ${e.message}")
    }
}

// Extension function mới, đơn giản hóa
fun Fragment.animateThenNavigate(
    actionId: Int,
    exitViews: List<View>,
    duration: Long = 300,
    preloadAction: (suspend () -> Unit)? = null
) {
    viewLifecycleOwner.lifecycleScope.launch {
        // 1. Chạy preload nếu có
        preloadAction?.invoke()
        
        // 2. Animate các view ra
        exitViews.forEach { view ->
            view.hideWithAnimation(
                duration = duration,
                type = AnimationUtils.AnimationType.FADE
            )
        }
        
        // 3. Đợi animation hoàn thành
        delay(duration)
        
        // 4. Navigate
        findNavController().safeNavigate(actionId)
    }
}