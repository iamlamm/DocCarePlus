package com.healthtech.doccareplus.utils

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator

object AnimationUtils {

    // Xử lý animation mượt mà khi show một view
    fun View.showWithAnimation(duration: Long = 400) {
        alpha = 0f
        scaleX = 0.92f
        scaleY = 0.92f
        visibility = View.VISIBLE

        animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()
    }

    // Xử lý animation mượt mà khi hide một view
    fun View.hideWithAnimation(duration: Long = 400, onEnd: () -> Unit = {}) {
        animate()
            .alpha(0f)
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                visibility = View.GONE
                onEnd()
            }
            .start()
    }
}