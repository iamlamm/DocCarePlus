package com.healthtech.doccareplus.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.healthtech.doccareplus.common.base.BaseFragment
import com.healthtech.doccareplus.databinding.FragmentAllCategoriesBinding
import com.healthtech.doccareplus.ui.category.adapter.AllCategoriesAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AllCategoriesFragment : BaseFragment() {
    private var _binding: FragmentAllCategoriesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AllCategoriesViewModel by viewModels()
    private lateinit var allCategoriesAdapter: AllCategoriesAdapter
    
    // Để theo dõi xem đã setup observers chưa để tránh setup lại
    private var hasSetupObservers = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllCategoriesBinding.inflate(inflater, container, false)
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
            observeCategories()
//            setupSwipeRefresh()
            hasSetupObservers = true
        }
    }
    
    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
                requireActivity().onBackPressed()
            }
            title = "Find Your Category"
        }
    }
    
    private fun setupAdapter() {
        allCategoriesAdapter = AllCategoriesAdapter().apply {
            setOnCategoryClickListener { category ->
                // TODO: Navigate to category detail
                // Ví dụ: findNavController().navigate(
                //     AllCategoriesFragmentDirections.actionToCategoryDetail(category.id)
                // )
                
                Snackbar.make(
                    binding.root,
                    "Selected category: ${category.name}",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rcvAllCategories.apply {
            adapter = allCategoriesAdapter
            layoutManager = GridLayoutManager(requireContext(), 3)
            // Có thể thêm ItemDecoration nếu cần
        }
    }
    
//    private fun setupSwipeRefresh() {
//        // Nếu layout có SwipeRefreshLayout
//        if (::_binding.isInitialized && _binding != null && _binding!!::swipeRefresh.isInitialized) {
//            binding.swipeRefresh.setOnRefreshListener {
//                viewModel.refreshCategoriesIfNeeded()
//            }
//        }
//    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Quan sát dữ liệu categories
                viewModel.categories.collectLatest { state ->
//                    // Tắt SwipeRefreshLayout nếu đang hiển thị
//                    if (::_binding.isInitialized && _binding != null &&
//                        _binding!!::swipeRefresh.isInitialized) {
//                        binding.swipeRefresh.isRefreshing = false
//                    }
                    
                    handleUiState(
                        state = state,
                        onSuccess = { categories ->
                            // Cập nhật UI với dữ liệu mới
                            allCategoriesAdapter.setCategories(categories)
                        },
                        onError = { message ->
                            // Hiển thị thông báo lỗi
                            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
                                .setAction("Retry") {
                                    viewModel.refreshCategoriesIfNeeded()
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
        viewModel.refreshCategoriesIfNeeded()
    }

    override fun cleanupViewReferences() {
        if (_binding != null) {
            binding.rcvAllCategories.adapter = null
            _binding = null
        }
        super.cleanupViewReferences()
    }
}