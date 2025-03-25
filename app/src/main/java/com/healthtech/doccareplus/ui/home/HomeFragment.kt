package com.healthtech.doccareplus.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.common.base.BaseFragment
import com.healthtech.doccareplus.common.state.UiState
import com.healthtech.doccareplus.databinding.FragmentHomeBinding
import com.healthtech.doccareplus.ui.home.adapter.CategoryAdapter
import com.healthtech.doccareplus.ui.home.adapter.DoctorAdapter
import com.healthtech.doccareplus.ui.widgets.decoration.CustomItemDecoration
import com.healthtech.doccareplus.utils.safeNavigate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class HomeFragment : BaseFragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    val viewModel: HomeViewModel by viewModels()
    private val categoryAdapter by lazy { CategoryAdapter() }
    private val doctorAdapter by lazy { DoctorAdapter() }

    private val bannerImages = listOf(
        R.drawable.banner,
        R.drawable.banner_2,
        R.drawable.banner_3,
        R.drawable.banner_4,
        R.drawable.banner_5,
        R.drawable.banner_6,
        R.drawable.banner_7,
        R.drawable.banner_8
    )

    private var hasSetupObservers = false
    private var isBannerInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // // Kiểm tra visibility trước khi setup
        // if (binding.bannerSlider.visibility != View.VISIBLE) {
        //     binding.bannerSlider.visibility = View.VISIBLE
        // }

        // Ưu tiên thiết lập UI trước
        setupRecyclerView()

        // Tách việc thiết lập UI thành các giai đoạn để tránh ANR
        viewLifecycleOwner.lifecycleScope.launch {
            // Ưu tiên thiết lập banner và click listeners
            setupBannerSlider()
            setupClickListeners()

            // Chỉ thiết lập observers một lần trong lifecycle của Fragment
            if (!hasSetupObservers) {
                Timber.d("Setting up data observers for the first time")
                observeData()
                hasSetupObservers = true
            } else {
                Timber.d("Data observers already set up, skipping")
            }
        }
    }

    private fun setupBannerSlider() {
        Timber.d("Initializing banner")
        binding.bannerSlider.setImages(bannerImages)
        isBannerInitialized = true
    }

    private fun setupClickListeners() {
        binding.apply {
            tvSeeAllCategory.setOnClickListener {
                // Preload trước khi chuyển màn hình
                viewModel.preloadCategoriesScreen()

                // Delay nhỏ (không cần thiết nếu bạn đã cài đặt transition animations)
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(50) // Delay rất nhỏ, đủ để kích hoạt preload
                    findNavController().safeNavigate(R.id.action_home_to_allCategories)
                }
            }

            tvSeeAllDoctor.setOnClickListener {
                // Tương tự, preload cho Doctors
                viewModel.preloadDoctorsScreen()

                viewLifecycleOwner.lifecycleScope.launch {
                    delay(50)
                    findNavController().safeNavigate(R.id.action_home_to_allDoctors)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rcvCategories.apply {
            setHasFixedSize(true)
            setItemViewCacheSize(8)
            adapter = categoryAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        binding.rcvPopularDoctors.apply {
            setItemViewCacheSize(4)
            adapter = doctorAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            addItemDecoration(CustomItemDecoration(requireContext()))
        }
    }

    private fun observeData() {
        viewModel.categories.collectWithLifecycle { state ->
            if (state is UiState.Loading) {
                Timber.d("Categories loading...")
            } else if (state is UiState.Success) {
                Timber.d("Categories loaded: " + state.data.size + " items")
            }
            handleUiState(
                state = state,
                onSuccess = { categories -> categoryAdapter.setCategories(categories) },
                onError = { message -> showError(message) }
            )
        }

        viewModel.doctors.collectWithLifecycle { state ->
            if (state is UiState.Loading) {
                Timber.d("Doctors loading...")
            } else if (state is UiState.Success) {
                Timber.d("Doctors loaded: " + state.data.size + " items")
            }
            handleUiState(
                state = state,
                onSuccess = { doctors -> doctorAdapter.setDoctors(doctors) },
                onError = { message -> showError(message) }
            )
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        // Kiểm tra và xử lý banner trong mọi trường hợp
        if (!isBannerInitialized) {
            // Chưa khởi tạo, setup mới
            setupBannerSlider()
        } else {
            // Đã khởi tạo, sử dụng prepareForReuse() thay vì các phương thức khác
            // prepareForReuse() sẽ khôi phục vị trí và khởi động lại auto slide tự động
            binding.bannerSlider.prepareForReuse()
        }
    }

    override fun onPause() {
        super.onPause()
        // Dừng auto slide khi pause
        if (isBannerInitialized) {
            binding.bannerSlider.stopAutoSlide()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hasSetupObservers = false
    }

    override fun cleanupViewReferences() {
        if (_binding != null) {
            binding.rcvCategories.adapter = null
            binding.rcvPopularDoctors.adapter = null
            // CHỈ dừng auto slide, không làm biến mất hay hủy BannerSlider
            binding.bannerSlider.stopAutoSlide()
            _binding = null
        }
        super.cleanupViewReferences()
    }
}