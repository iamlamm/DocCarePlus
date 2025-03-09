package com.healthtech.doccareplus.utils

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.Gravity
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.CustomSnackbarBinding

enum class SnackbarType(val lottieRes: Int, val backgroundRes: Int) {
    SUCCESS(R.raw.success_1, R.drawable.bg_snackbar_success),
    ERROR(R.raw.success_2, R.drawable.bg_snackbar_error),
    WARNING(R.raw.success_3, R.drawable.bg_snackbar_warning),
    INFO(R.raw.success_1, R.drawable.bg_snackbar_info)
}

object SnackbarUtils {
    @SuppressLint("RestrictedApi")
    fun showSnackbar(
        view: View,
        message: String,
        type: SnackbarType = SnackbarType.SUCCESS,
        duration: Int = Snackbar.LENGTH_SHORT
    ) {
        // Create basic Snackbar
        val snackbar = Snackbar.make(view, "", duration)
        val snackbarLayout = snackbar.view as Snackbar.SnackbarLayout
        
        // Center the Snackbar horizontally
        val params = snackbarLayout.layoutParams
        when (params) {
            is CoordinatorLayout.LayoutParams -> {
                params.width = FrameLayout.LayoutParams.WRAP_CONTENT
                params.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            }
            is FrameLayout.LayoutParams -> {
                params.width = FrameLayout.LayoutParams.WRAP_CONTENT
                params.gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            }
        }
        snackbarLayout.layoutParams = params
        
        // Remove all default views
        snackbarLayout.removeAllViews()
        
        // Set background directly on the Snackbar view
        snackbarLayout.setBackgroundResource(type.backgroundRes)
        
        // Remove default padding
        snackbarLayout.setPadding(0, 0, 0, 0)
        
        // Inflate our custom layout
        val binding = CustomSnackbarBinding.inflate(LayoutInflater.from(view.context))
        
        // Configure the custom layout
        binding.lottieIcon.setAnimation(type.lottieRes)
        binding.tvMessage.text = message
        
        // Add our custom layout to the Snackbar
        snackbarLayout.addView(binding.root)
        
        snackbar.show()
    }

    // Convenience methods
    fun showSuccessSnackbar(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT
    ) = showSnackbar(view, message, SnackbarType.SUCCESS, duration)

    fun showErrorSnackbar(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT
    ) = showSnackbar(view, message, SnackbarType.ERROR, duration)

    fun showWarningSnackbar(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT
    ) = showSnackbar(view, message, SnackbarType.WARNING, duration)

    fun showInfoSnackbar(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT
    ) = showSnackbar(view, message, SnackbarType.INFO, duration)
}