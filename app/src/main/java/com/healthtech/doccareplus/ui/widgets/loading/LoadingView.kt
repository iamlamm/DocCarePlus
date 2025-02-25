package com.healthtech.doccareplus.ui.widgets.loading

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.Animation
import androidx.constraintlayout.widget.ConstraintLayout
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.ViewLoadingBinding
import com.healthtech.doccareplus.utils.AnimationUtils.hideWithAnimation
import com.healthtech.doccareplus.utils.AnimationUtils.showWithAnimation

class LoadingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewLoadingBinding.inflate(LayoutInflater.from(context), this, true)

    // Sử dụng property animator thay vì animation thông thường
    private var currentAnimation: Animation? = null

    init {
        // Thiết lập nền để hiệu ứng mượt mà hơn
//        setBackgroundColor(context.getColor(R.color.black_transparent))
        // Đảm bảo Lottie được cấu hình tối ưu
        binding.lottieAnimation.apply {
            repeatCount = -1
            speed = 1.0f
            imageAssetsFolder = "images/"
            enableMergePathsForKitKatAndAbove(true)
        }
    }

    fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            showWithAnimation()
            binding.lottieAnimation.playAnimation()
        } else {
            hideWithAnimation {
                binding.lottieAnimation.pauseAnimation()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        binding.lottieAnimation.pauseAnimation()
        // Đảm bảo hủy tất cả animation đang chạy
        animate().cancel()
        currentAnimation?.cancel()
    }
}