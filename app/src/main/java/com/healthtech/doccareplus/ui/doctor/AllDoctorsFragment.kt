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
import com.healthtech.doccareplus.databinding.FragmentAllDoctorsBinding
import com.healthtech.doccareplus.ui.doctor.adapter.AllDoctorsAdapter
import dagger.hilt.android.AndroidEntryPoint
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllDoctorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialization in order of priority
        setupToolbar()
        setupAdapter()
        setupRecyclerView()
        
        // Kiểm tra trạng thái đã load dữ liệu chưa
        if (!hasSetupObservers) {
            observeDoctors()
//            setupSwipeRefresh()
            hasSetupObservers = true
        }
    }
    
    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
                requireActivity().onBackPressed()
            }
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
            // Có thể thêm ItemDecoration nếu cần
        }
    }
    
//    private fun setupSwipeRefresh() {
//        // Nếu layout có SwipeRefreshLayout
//        if (::_binding.isInitialized && _binding != null && _binding!!::swipeRefresh.isInitialized) {
//            binding.swipeRefresh.setOnRefreshListener {
//                viewModel.refreshDoctorsIfNeeded()
//            }
//        }
//    }

    private fun observeDoctors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Quan sát dữ liệu doctors
                viewModel.doctors.collectLatest { state ->
//                    // Tắt SwipeRefreshLayout nếu đang hiển thị
//                    if (::_binding.isInitialized && _binding != null &&
//                        _binding!!::swipeRefresh.isInitialized) {
//                        binding.swipeRefresh.isRefreshing = false
//                    }
                    
                    handleUiState(
                        state = state,
                        onSuccess = { doctors ->
                            // Cập nhật UI với dữ liệu mới
                            allDoctorsAdapter.setDoctors(doctors)
                        },
                        onError = { message ->
                            // Hiển thị thông báo lỗi
                            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                                .setAction("Retry") {
                                    viewModel.refreshDoctorsIfNeeded()
                                }
                                .show()
                        }
                    )
                }
                
                // Quan sát trạng thái đã tải xong chưa
                viewModel.isInitialDataLoaded.collect { isLoaded ->
                    // Bạn có thể sử dụng trạng thái này để ẩn một loading indicator
                    // binding.loadingIndicator.visibility = if (isLoaded) View.GONE else View.VISIBLE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDoctorsIfNeeded()
    }

    override fun cleanupViewReferences() {
        if (_binding != null) {
            binding.rcvAllDoctors.adapter = null
            _binding = null
        }
        super.cleanupViewReferences()
    }
}