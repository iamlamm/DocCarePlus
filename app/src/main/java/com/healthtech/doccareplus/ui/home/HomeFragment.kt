package com.healthtech.doccareplus.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.common.base.BaseFragment
import com.healthtech.doccareplus.common.state.UiState
import com.healthtech.doccareplus.databinding.FragmentHomeBinding
import com.healthtech.doccareplus.ui.home.adapter.CategoryAdapter
import com.healthtech.doccareplus.ui.home.adapter.DoctorAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class HomeFragment : BaseFragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    // Khởi tạo adapter một lần trong vòng đời Fragment
    private val categoryAdapter by lazy { CategoryAdapter() }
    private val doctorAdapter by lazy { DoctorAdapter() }

    private val bannerImages = listOf(
        R.drawable.banner,
        R.drawable.banner,
        R.drawable.banner,
        R.drawable.banner,
        R.drawable.banner
    )

    // Biến theo dõi trạng thái đã thiết lập data observers
    private var hasSetupObservers = false

    // Biến lưu trạng thái đã khởi tạo banner
    private var isBannerInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Kiểm tra visibility trước khi setup
        if (binding.bannerSlider.visibility != View.VISIBLE) {
            binding.bannerSlider.visibility = View.VISIBLE
        }
        
        // Ưu tiên thiết lập UI trước
        setupRecyclerView()
        
        // Tách việc thiết lập UI thành các giai đoạn để tránh ANR
        viewLifecycleOwner.lifecycleScope.launch {
            // Ưu tiên thiết lập banner và click listeners
            setupBannerSlider()
            setupClickListeners()
            
            // Chỉ thiết lập observers một lần trong lifecycle của Fragment
            if (!hasSetupObservers) {
                Log.d("HomeFragment", "Setting up data observers for the first time")
                observeData()
                hasSetupObservers = true
            } else {
                Log.d("HomeFragment", "Data observers already set up, skipping")
            }
        }
    }

    private fun setupBannerSlider() {
        Log.d("HomeFragment", "Initializing banner")
        binding.bannerSlider.setImages(bannerImages)
        isBannerInitialized = true
    }

    private fun setupClickListeners() {
        binding.apply {
            tvSeeAllCategory.setOnClickListener {
                findNavController().navigate(R.id.action_home_to_allCategories)
            }

            tvSeeAllDoctor.setOnClickListener {
                findNavController().navigate(R.id.action_home_to_allDoctors)
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rcvCategories.apply {
            adapter = categoryAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        binding.rcvPopularDoctors.apply {
            adapter = doctorAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(), DividerItemDecoration.VERTICAL
                )
            )
        }
    }

    private fun observeData() {
        viewModel.categories.collectWithLifecycle { state ->
            if (state is UiState.Loading) {
                Log.d("HomeFragment", "Categories loading...")
            } else if (state is UiState.Success) {
                Log.d("HomeFragment", "Categories loaded: ${state.data.size} items")
            }
            handleUiState(
                state = state,
                onSuccess = { categories -> categoryAdapter.setCategories(categories) },
                onError = { message -> showError(message) }
            )
        }

        viewModel.doctors.collectWithLifecycle { state ->
            if (state is UiState.Loading) {
                Log.d("HomeFragment", "Doctors loading...")
            } else if (state is UiState.Success) {
                Log.d("HomeFragment", "Doctors loaded: ${state.data.size} items")
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
        Log.d("HomeFragment", "onDestroy called")
        // Chỉ reset trạng thái nếu Fragment thực sự bị hủy, không phải khi view bị destroy
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