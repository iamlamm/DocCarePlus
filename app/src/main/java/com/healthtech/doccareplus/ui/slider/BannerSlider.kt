package com.healthtech.doccareplus.ui.slider

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager2.widget.ViewPager2
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.ViewBannerSliderBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BannerSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewBannerSliderBinding.inflate(LayoutInflater.from(context), this)
    private lateinit var sliderAdapter: SliderAdapter
    private var imageList = listOf<Int>()
    
    private var coroutineScope: CoroutineScope? = null
    private var autoSlideJob: Job? = null

    init {
        setupViewPager()
        // Tạo scope mới khi view được khởi tạo
        coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    private fun setupViewPager() {
        binding.viewPager.apply {
            offscreenPageLimit = 1
            
            val nextItemVisiblePx = resources.getDimensionPixelOffset(R.dimen.page_offset)
            val currentItemHorizontalMarginPx = resources.getDimensionPixelOffset(R.dimen.page_margin)
            val pageTranslationX = nextItemVisiblePx + currentItemHorizontalMarginPx

            setPageTransformer { page, position ->
                page.translationX = -pageTranslationX * position
                page.scaleY = 1 - (0.25f * kotlin.math.abs(position))
                page.alpha = 0.25f + (1 - kotlin.math.abs(position))
            }

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    binding.indicator.animatePageSelected(position % imageList.size)
                }
            })
        }
    }

    fun setImages(images: List<Int>) {
        imageList = images
        sliderAdapter = SliderAdapter(images)
        binding.viewPager.adapter = sliderAdapter
        
        // Thiết lập hiệu ứng chuyển trang
        val nextItemVisiblePx = resources.getDimensionPixelOffset(R.dimen.page_offset)
        val currentItemHorizontalMarginPx = resources.getDimensionPixelOffset(R.dimen.page_margin)
        val pageTranslationX = nextItemVisiblePx + currentItemHorizontalMarginPx
        
        binding.viewPager.apply {
            offscreenPageLimit = 1
            setPageTransformer { page, position ->
                page.translationX = -pageTranslationX * position
                page.scaleY = 1 - (0.25f * kotlin.math.abs(position))
                page.alpha = 0.25f + (1 - kotlin.math.abs(position))
            }
        }
        
        // Thiết lập vị trí bắt đầu và indicator
        val startPosition = (images.size * 500)
        binding.viewPager.setCurrentItem(startPosition, false)
        
        binding.indicator.setViewPager(binding.viewPager)
        binding.indicator.createIndicators(images.size, startPosition % images.size)
        
        // Cập nhật indicator khi page thay đổi
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.indicator.animatePageSelected(position % images.size)
            }
        })
        
        startAutoSlide()
    }

    private fun startAutoSlide() {
        stopAutoSlide()
        coroutineScope?.let { scope ->
            autoSlideJob = scope.launch {
                while(isActive) {
                    delay(3000)
                    binding.viewPager.currentItem = binding.viewPager.currentItem + 1
                }
            }
        }
    }

    private fun stopAutoSlide() {
        autoSlideJob?.cancel()
        autoSlideJob = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAutoSlide()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Hủy tất cả coroutines khi view bị detach
        stopAutoSlide()
        coroutineScope?.cancel()
        coroutineScope = null
    }
}