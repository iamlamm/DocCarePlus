package com.healthtech.doccareplus.ui.widgets.slider

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager2.widget.ViewPager2
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.ViewBannerSliderBinding
import com.healthtech.doccareplus.utils.AnimationUtils
import com.healthtech.doccareplus.utils.AnimationUtils.showWithAnimation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Custom ViewGroup để hiển thị slider với hiệu ứng transition và auto-sliding.
 * - Sử dụng ViewPager2 và RecyclerView cho hiệu suất tốt
 * - Tự động scroll với Coroutines
 * - Hiển thị infinite scroll
 */

class BannerSlider @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewBannerSliderBinding.inflate(LayoutInflater.from(context), this)

    private var currentPosition = 0

    private val sliderAdapter by lazy { SliderAdapter(emptyList()) }
    private var imageList = emptyList<Int>()
    private var isInitialized = false

    private var coroutineScope: CoroutineScope? = null
    private var autoSlideJob: Job? = null

    companion object {
        private const val AUTO_SLIDE_DELAY = 3000L
        private const val INFINITE_SCROLL_MULTIPLIER = 1000
        private const val SCALE_FACTOR = 0.25f
        private const val MIN_ALPHA = 0.25f
    }

    init {
        initializeViewPager()
        coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    }

    private fun initializeViewPager() {
        binding.viewPager.apply {
            offscreenPageLimit = 1
            adapter = sliderAdapter
            setupPageTransformer()
        }
    }

    // private fun ViewPager2.setupPageTransformer() {
    //     val nextItemVisiblePx = resources.getDimensionPixelOffset(R.dimen.page_offset)
    //     val currentItemHorizontalMarginPx = resources.getDimensionPixelOffset(R.dimen.page_margin)
    //     val pageTranslationX = nextItemVisiblePx + currentItemHorizontalMarginPx

    //     setPageTransformer { page, position ->
    //         page.apply {
    //             translationX = -pageTranslationX * position
    //             scaleY = 1 - (SCALE_FACTOR * kotlin.math.abs(position))
    //             alpha = MIN_ALPHA + (1 - kotlin.math.abs(position))
    //         }
    //     }
    // }

    private fun ViewPager2.setupPageTransformer() {
        val nextItemVisiblePx = resources.getDimensionPixelOffset(R.dimen.page_offset)
        val currentItemHorizontalMarginPx = resources.getDimensionPixelOffset(R.dimen.page_margin)
        val pageTranslationX = nextItemVisiblePx + currentItemHorizontalMarginPx

        setPageTransformer { page, position ->
            page.apply {
                val positionOffset = position * 0.8f
                translationX = -pageTranslationX * positionOffset

                val absPosition = kotlin.math.abs(position)
                val scaleFactor = 1 - (SCALE_FACTOR * absPosition * 0.7f)
                scaleY = scaleFactor
                scaleX = 1f - (SCALE_FACTOR * absPosition * 0.3f)
                alpha = MIN_ALPHA + (1 - absPosition) * (1 - MIN_ALPHA)
                rotation = position * -1.5f
            }
        }
    }

    fun saveCurrentPosition() {
        if (imageList.isNotEmpty()) {
            currentPosition = binding.viewPager.currentItem
        }
    }

    fun restorePosition() {
        if (imageList.isNotEmpty()) {
            binding.viewPager.setCurrentItem(currentPosition, false)
            binding.indicator.animatePageSelected(currentPosition % imageList.size)
        }
    }

    fun reinitialize() {
        if (imageList.isEmpty()) return

        if (coroutineScope == null) {
            coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        }

        binding.viewPager.adapter = sliderAdapter
        sliderAdapter.updateImages(imageList)

        setupInitialPosition(true)
        setupIndicator()
        startAutoSlide()

        isInitialized = true
        visibility = View.VISIBLE
    }

    fun setImages(images: List<Int>, restoreLastPosition: Boolean = false) {
        if (images.isEmpty()) return

        imageList = images

        if (binding.viewPager.adapter == null) {
            binding.viewPager.adapter = sliderAdapter
        }

        sliderAdapter.updateImages(images)
        setupInitialPosition(restoreLastPosition)
        setupIndicator()

        visibility = View.VISIBLE
        isInitialized = true

        startAutoSlide()
    }

    private fun setupInitialPosition(restoreLastPosition: Boolean) {
        val startPosition = if (restoreLastPosition && currentPosition > 0) {
            currentPosition
        } else {
            (imageList.size * INFINITE_SCROLL_MULTIPLIER) / 2
        }
        binding.viewPager.setCurrentItem(startPosition, false)
    }

    private fun setupIndicator() {
        binding.indicator.apply {
            setViewPager(binding.viewPager)
            createIndicators(imageList.size, binding.viewPager.currentItem % imageList.size)
        }

        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback)
    }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            if (imageList.isNotEmpty()) {
                binding.indicator.animatePageSelected(position % imageList.size)
                currentPosition = position
            }
        }
    }

    fun prepareForReuse() {
        visibility = View.VISIBLE

        if (imageList.isNotEmpty()) {
            restorePosition()
            startAutoSlide()
        }
    }

    fun startAutoSlide() {
        stopAutoSlide()
        coroutineScope?.launch {
            while (isActive) {
                delay(AUTO_SLIDE_DELAY)
                binding.viewPager.let { viewPager ->
                    if (!viewPager.isFakeDragging) {
                        viewPager.setCurrentItem(viewPager.currentItem + 1, true)
                    }
                }
            }
        }?.also { autoSlideJob = it }
    }

    fun restartSlider() {
        if (imageList.isEmpty()) return
        visibility = View.VISIBLE
        if (binding.viewPager.adapter == null) {
            binding.viewPager.adapter = sliderAdapter
            sliderAdapter.updateImages(imageList)
            setupInitialPosition(true)
            setupIndicator()
        }
        startAutoSlide()
        isInitialized = true
    }

    fun stopAutoSlide() {
        autoSlideJob?.cancel()
        autoSlideJob = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (coroutineScope == null) {
            coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        }
        if (imageList.isNotEmpty()) {
            startAutoSlide()
        }
    }

    override fun onDetachedFromWindow() {
        stopAutoSlide()
        super.onDetachedFromWindow()
    }

    fun cleanupResources(fullCleanup: Boolean = false) {
        stopAutoSlide()
        if (fullCleanup) {
            coroutineScope?.cancel()
            coroutineScope = null
            isInitialized = false
        }
    }

    private fun setupSlideAnimation() {
        binding.viewPager.apply {
            showWithAnimation(
                duration = 800,
                type = AnimationUtils.AnimationType.FADE
            )
        }
    }
}

/**
 * 15h 2502
 */
// class BannerSlider @JvmOverloads constructor(
//     context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
// ) : ConstraintLayout(context, attrs, defStyleAttr) {
//
//     private val binding = ViewBannerSliderBinding.inflate(LayoutInflater.from(context), this)
//     private lateinit var sliderAdapter: SliderAdapter
//     private var imageList = listOf<Int>()
//
//     private var coroutineScope: CoroutineScope? = null
//     private var autoSlideJob: Job? = null
//
//     // Constants
//     companion object {
//         private const val AUTO_SLIDE_DELAY = 3000L
//         private const val INFINITE_SCROLL_MULTIPLIER = 500
//         private const val SCALE_FACTOR = 0.25f
//         private const val MIN_ALPHA = 0.25f
//     }
//
//     init {
//         coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
//         initializeViewPager()
//     }
//
//     private fun initializeViewPager() {
//         binding.viewPager.apply {
//             offscreenPageLimit = 1
//             setupPageTransformer()
//         }
//     }
//
//     private fun ViewPager2.setupPageTransformer() {
//         val nextItemVisiblePx = resources.getDimensionPixelOffset(R.dimen.page_offset)
//         val currentItemHorizontalMarginPx = resources.getDimensionPixelOffset(R.dimen.page_margin)
//         val pageTranslationX = nextItemVisiblePx + currentItemHorizontalMarginPx
//
//         setPageTransformer { page, position ->
//             page.apply {
//                 translationX = -pageTranslationX * position
//                 scaleY = 1 - (SCALE_FACTOR * kotlin.math.abs(position))
//                 alpha = MIN_ALPHA + (1 - kotlin.math.abs(position))
//             }
//         }
//     }
//
//     fun setImages(images: List<Int>) {
//         if (images.isEmpty()) return
//
//         imageList = images
//         setupAdapter()
//         setupInitialPosition()
//         setupIndicator()
//         startAutoSlide()
//     }
//
//     private fun setupAdapter() {
//         sliderAdapter = SliderAdapter(imageList)
//         binding.viewPager.adapter = sliderAdapter
//     }
//
//     private fun setupInitialPosition() {
//         val startPosition = (imageList.size * INFINITE_SCROLL_MULTIPLIER)
//         binding.viewPager.setCurrentItem(startPosition, false)
//     }
//
//     private fun setupIndicator() {
//         binding.indicator.apply {
//             setViewPager(binding.viewPager)
//             createIndicators(imageList.size, binding.viewPager.currentItem % imageList.size)
//         }
//
//         binding.viewPager.registerOnPageChangeCallback(createPageChangeCallback())
//     }
//
//     private fun createPageChangeCallback() = object : ViewPager2.OnPageChangeCallback() {
//         override fun onPageSelected(position: Int) {
//             super.onPageSelected(position)
//             binding.indicator.animatePageSelected(position % imageList.size)
//         }
//     }
//
//     private fun startAutoSlide() {
//         stopAutoSlide()
//         coroutineScope?.launch {
//             while (isActive) {
//                 delay(AUTO_SLIDE_DELAY)
//                 binding.viewPager.currentItem += 1
//             }
//         }?.also { autoSlideJob = it }
//     }
//
//     private fun stopAutoSlide() {
//         autoSlideJob?.cancel()
//         autoSlideJob = null
//     }
//
//     override fun onAttachedToWindow() {
//         super.onAttachedToWindow()
//         if (::sliderAdapter.isInitialized) {
//             startAutoSlide()
//         }
//     }
//
//     override fun onDetachedFromWindow() {
//         cleanupResources()
//         super.onDetachedFromWindow()
//     }
//
//     private fun cleanupResources() {
//         stopAutoSlide()
//         coroutineScope?.cancel()
//         coroutineScope = null
//     }
// }

/**
 * 15h 2402
 */

//class BannerSlider @JvmOverloads constructor(
//    context: Context,
//    attrs: AttributeSet? = null,
//    defStyleAttr: Int = 0
//) : ConstraintLayout(context, attrs, defStyleAttr) {
//
//    private val binding = ViewBannerSliderBinding.inflate(LayoutInflater.from(context), this)
//    private lateinit var sliderAdapter: SliderAdapter
//    private var imageList = listOf<Int>()

//    private var coroutineScope: CoroutineScope? = null
//    private var autoSlideJob: Job? = null

//    init {
//        setupViewPager()
//        // Tạo scope mới khi view được khởi tạo
//        coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
//    }

//    private fun setupViewPager() {
//        binding.viewPager.apply {
//            offscreenPageLimit = 1
//
//            val nextItemVisiblePx = resources.getDimensionPixelOffset(R.dimen.page_offset)
//            val currentItemHorizontalMarginPx = resources.getDimensionPixelOffset(R.dimen.page_margin)
//            val pageTranslationX = nextItemVisiblePx + currentItemHorizontalMarginPx
//
//            setPageTransformer { page, position ->
//                page.translationX = -pageTranslationX * position
//                page.scaleY = 1 - (0.25f * kotlin.math.abs(position))
//                page.alpha = 0.25f + (1 - kotlin.math.abs(position))
//            }
//
//            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
//                override fun onPageSelected(position: Int) {
//                    super.onPageSelected(position)
//                    binding.indicator.animatePageSelected(position % imageList.size)
//                }
//            })
//        }
//    }
//
//    fun setImages(images: List<Int>) {
//        imageList = images
//        sliderAdapter = SliderAdapter(images)
//        binding.viewPager.adapter = sliderAdapter
//
//        // Thiết lập hiệu ứng chuyển trang
//        val nextItemVisiblePx = resources.getDimensionPixelOffset(R.dimen.page_offset)
//        val currentItemHorizontalMarginPx = resources.getDimensionPixelOffset(R.dimen.page_margin)
//        val pageTranslationX = nextItemVisiblePx + currentItemHorizontalMarginPx
//
//        binding.viewPager.apply {
//            offscreenPageLimit = 1
//            setPageTransformer { page, position ->
//                page.translationX = -pageTranslationX * position
//                page.scaleY = 1 - (0.25f * kotlin.math.abs(position))
//                page.alpha = 0.25f + (1 - kotlin.math.abs(position))
//            }
//        }
//
//        // Thiết lập vị trí bắt đầu và indicator
//        val startPosition = (images.size * 500)
//        binding.viewPager.setCurrentItem(startPosition, false)
//
//        binding.indicator.setViewPager(binding.viewPager)
//        binding.indicator.createIndicators(images.size, startPosition % images.size)
//
//        // Cập nhật indicator khi page thay đổi
//        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
//            override fun onPageSelected(position: Int) {
//                super.onPageSelected(position)
//                binding.indicator.animatePageSelected(position % images.size)
//            }
//        })
//
//        startAutoSlide()
//    }
//
//    private fun startAutoSlide() {
//        stopAutoSlide()
//        coroutineScope?.let { scope ->
//            autoSlideJob = scope.launch {
//                while(isActive) {
//                    delay(3000)
//                    binding.viewPager.currentItem = binding.viewPager.currentItem + 1
//                }
//            }
//        }
//    }
//
//    private fun stopAutoSlide() {
//        autoSlideJob?.cancel()
//        autoSlideJob = null
//    }
//
//    override fun onAttachedToWindow() {
//        super.onAttachedToWindow()
//        startAutoSlide()
//    }
//
//    override fun onDetachedFromWindow() {
//        super.onDetachedFromWindow()
//        // Hủy tất cả coroutines khi view bị detach
//        stopAutoSlide()
//        coroutineScope?.cancel()
//        coroutineScope = null
//    }
//}