package com.healthtech.doccareplus.ui.doctor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.healthtech.doccareplus.common.base.BaseFragment
import com.healthtech.doccareplus.common.state.UiState
import com.healthtech.doccareplus.databinding.FragmentAllDoctorsBinding
import com.healthtech.doccareplus.ui.doctor.adapter.AllDoctorsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AllDoctorsFragment : BaseFragment() {
    private var _binding: FragmentAllDoctorsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AllDoctorsViewModel by viewModels()
    private lateinit var allDoctorsAdapter: AllDoctorsAdapter
    
    // Để theo dõi xem đã setup observers chưa để tránh setup lại
    private var hasSetupObservers = false
    // Theo dõi lần load đầu tiên để tối ưu hiệu suất
    private var isFirstLoad = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllDoctorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Hiển thị loading indicator ngay lập tức nếu có
        // binding.progressBarAllDoctors.visibility = View.VISIBLE
        
        // Initialization in order of priority
        setupToolbar()
        setupAdapter()
        setupRecyclerView()
        
        // Kiểm tra trạng thái đã load dữ liệu chưa
        if (!hasSetupObservers) {
            observeDoctors()
            observeCategoryInfo()
            hasSetupObservers = true
        }
    }
    
    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
                requireActivity().onBackPressed()
            }
            
            // Title mặc định
            title = "Find Your Doctor"
        }
    }
    
    private fun setupAdapter() {
        allDoctorsAdapter = AllDoctorsAdapter().apply {
            setOnDoctorClickListener { doctor ->
                // TODO: Navigate to doctor detail
                Snackbar.make(
                    binding.root,
                    "Selected doctor: ${doctor.name}",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            
            setOnBookClickListener { doctor ->
                // TODO: Navigate to booking screen
                Snackbar.make(
                    binding.root,
                    "Book appointment with: ${doctor.name}",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rcvAllDoctors.apply {
            adapter = allDoctorsAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            // Tắt animation để tăng hiệu suất
            itemAnimator = null
            // Tối ưu performance
            setHasFixedSize(true)
        }
    }

    private fun observeDoctors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Quan sát dữ liệu doctors
                viewModel.doctors.collectLatest { state ->
                    when (state) {
                        is UiState.Success -> {
                            val doctors = state.data
                            // Lazy loading để tăng tốc độ hiển thị ban đầu
                            if (isFirstLoad && doctors.size > 8) {
                                // Hiển thị 8 bác sĩ đầu tiên trước (đủ để điền màn hình)
                                allDoctorsAdapter.setDoctors(doctors.take(8))
                                
                                // Sau đó mới hiển thị đầy đủ - trong coroutine scope
                                launch {
                                    delay(150)
                                    allDoctorsAdapter.setDoctors(doctors)
                                    isFirstLoad = false
                                }
                            } else {
                                allDoctorsAdapter.setDoctors(doctors)
                                if (isFirstLoad) isFirstLoad = false
                            }
                        }
                        is UiState.Error -> {
                            // Hiển thị thông báo lỗi
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG)
                                .setAction("Retry") {
                                    viewModel.refreshDoctors()
                                }
                                .show()
                        }
                        else -> {
                            // Xử lý các trạng thái khác
                        }
                    }
                }
                
                // Quan sát trạng thái đã tải xong chưa
                viewModel.isInitialDataLoaded.collect { isLoaded ->
                    // Có thể cập nhật UI dựa trên trạng thái isLoaded
                    // binding.progressBarAllDoctors.visibility = if (isLoaded) View.GONE else View.VISIBLE
                }
            }
        }
    }

    // Thêm hàm để observe thông tin category
    private fun observeCategoryInfo() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categoryName.collect { categoryName ->
                if (!categoryName.isNullOrEmpty()) {
                    // Cập nhật title nếu có category name
                    binding.toolbar.title = categoryName
                }
            }
        }
    }

    // Thay đổi onResume để tránh refresh ngay lập tức
    override fun onResume() {
        super.onResume()
        
        // Chỉ refresh sau khi đã hoàn tất animation chuyển màn hình
        if (!isFirstLoad) {
            viewLifecycleOwner.lifecycleScope.launch {
                delay(300) // Đợi animation chuyển màn hình hoàn tất
                viewModel.checkAndRefreshIfNeeded()
            }
        }
    }

    override fun cleanupViewReferences() {
        if (_binding != null) {
            binding.rcvAllDoctors.adapter = null
            _binding = null
        }
        super.cleanupViewReferences()
    }
}