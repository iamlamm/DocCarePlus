package com.healthtech.doccareplus.utils

import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator

object AnimationUtils {

    // Thêm các kiểu animation
    enum class AnimationType {
        FADE,
        SCALE,
        SLIDE_UP,
        SLIDE_DOWN,
        SLIDE_LEFT,
        SLIDE_RIGHT
    }

    // Cải tiến hàm showWithAnimation với nhiều options hơn
    fun View.showWithAnimation(
        duration: Long = 400,
        type: AnimationType = AnimationType.FADE,
        delay: Long = 0,
        onStart: () -> Unit = {},
        onEnd: () -> Unit = {}
    ) {
        visibility = View.VISIBLE
        
        when (type) {
            AnimationType.FADE -> {
                alpha = 0f
                animate()
                    .alpha(1f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(DecelerateInterpolator(1.5f))
                    .withStartAction { onStart() }
                    .withEndAction { onEnd() }
                    .start()
            }
            AnimationType.SCALE -> {
                alpha = 0f
                scaleX = 0.85f
                scaleY = 0.85f
                animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(DecelerateInterpolator(1.5f))
                    .withStartAction { onStart() }
                    .withEndAction { onEnd() }
                    .start()
            }
            AnimationType.SLIDE_UP -> {
                alpha = 0f
                translationY = height.toFloat()
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(DecelerateInterpolator(1.5f))
                    .withStartAction { onStart() }
                    .withEndAction { onEnd() }
                    .start()
            }
            AnimationType.SLIDE_DOWN -> {
                alpha = 0f
                translationY = -height.toFloat()
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(DecelerateInterpolator(1.5f))
                    .withStartAction { onStart() }
                    .withEndAction { onEnd() }
                    .start()
            }
            AnimationType.SLIDE_LEFT -> {
                alpha = 0f
                translationX = width.toFloat()
                animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(DecelerateInterpolator(1.5f))
                    .withStartAction { onStart() }
                    .withEndAction { onEnd() }
                    .start()
            }
            AnimationType.SLIDE_RIGHT -> {
                alpha = 0f
                translationX = -width.toFloat()
                animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(DecelerateInterpolator(1.5f))
                    .withStartAction { onStart() }
                    .withEndAction { onEnd() }
                    .start()
            }
        }
    }

    // Cải tiến hàm hideWithAnimation tương tự
    fun View.hideWithAnimation(
        duration: Long = 400,
        type: AnimationType = AnimationType.FADE,
        delay: Long = 0,
        onStart: () -> Unit = {},
        onEnd: () -> Unit = {}
    ) {
        when (type) {
            AnimationType.FADE -> {
                animate()
                    .alpha(0f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withStartAction { onStart() }
                    .withEndAction {
                        visibility = View.GONE
                        onEnd()
                    }
                    .start()
            }
            AnimationType.SCALE -> {
                animate()
                    .alpha(0f)
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withStartAction { onStart() }
                    .withEndAction {
                        visibility = View.GONE
                        onEnd()
                    }
                    .start()
            }
            AnimationType.SLIDE_UP -> {
                animate()
                    .alpha(0f)
                    .translationY(-height.toFloat())
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withStartAction { onStart() }
                    .withEndAction {
                        visibility = View.GONE
                        translationY = 0f // Reset position
                        onEnd()
                    }
                    .start()
            }
            AnimationType.SLIDE_DOWN -> {
                animate()
                    .alpha(0f)
                    .translationY(height.toFloat())
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withStartAction { onStart() }
                    .withEndAction {
                        visibility = View.GONE
                        translationY = 0f // Reset position
                        onEnd()
                    }
                    .start()
            }
            AnimationType.SLIDE_LEFT -> {
                animate()
                    .alpha(0f)
                    .translationX(-width.toFloat())
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withStartAction { onStart() }
                    .withEndAction {
                        visibility = View.GONE
                        translationX = 0f // Reset position
                        onEnd()
                    }
                    .start()
            }
            AnimationType.SLIDE_RIGHT -> {
                animate()
                    .alpha(0f)
                    .translationX(width.toFloat())
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withStartAction { onStart() }
                    .withEndAction {
                        visibility = View.GONE
                        translationX = 0f // Reset position
                        onEnd()
                    }
                    .start()
            }
        }
    }

    // Cải tiến fadeInSequentially với nhiều options hơn
    fun fadeInSequentially(
        views: List<View>,
        duration: Long = 800,
        delayBetween: Long = 300,
        type: AnimationType = AnimationType.FADE,
        onAllComplete: () -> Unit = {}
    ) {
        var totalDelay = 0L
        views.forEachIndexed { index, view ->
            view.showWithAnimation(
                duration = duration,
                type = type,
                delay = totalDelay,
                onEnd = {
                    if (index == views.size - 1) {
                        onAllComplete()
                    }
                }
            )
            totalDelay += delayBetween
        }
    }

    // Thêm hàm mới để animate một group views
    fun animateViewGroup(
        views: List<View>,
        type: AnimationType = AnimationType.FADE,
        duration: Long = 400,
        stagger: Long = 100,
        onAllComplete: () -> Unit = {}
    ) {
        views.forEachIndexed { index, view ->
            view.showWithAnimation(
                duration = duration,
                type = type,
                delay = index * stagger,
                onEnd = {
                    if (index == views.size - 1) {
                        onAllComplete()
                    }
                }
            )
        }
    }

    // Thêm hàm tiện ích để reset trạng thái animation
    fun View.resetAnimation() {
        alpha = 1f
        scaleX = 1f
        scaleY = 1f
        translationX = 0f
        translationY = 0f
        rotation = 0f
        clearAnimation()
    }
}