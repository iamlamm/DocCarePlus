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
import com.healthtech.doccareplus.common.state.UiState
import com.healthtech.doccareplus.databinding.FragmentAllCategoriesBinding
import com.healthtech.doccareplus.ui.category.adapter.AllCategoriesAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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
    private var isFirstLoad = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Hiển thị loading indicator ngay lập tức
//        binding.progressBarAllCategories.visibility = View.VISIBLE
        
        // Initialization in order of priority
        setupToolbar()
        setupAdapter()
        setupRecyclerView()
        
        // Kiểm tra trạng thái đã load dữ liệu chưa
        if (!hasSetupObservers) {
            observeCategories()
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
//                // Xử lý click
//                Snackbar.make(
//                    binding.root,
//                    "Selected category: ${category.name}",
//                    Snackbar.LENGTH_SHORT
//                ).show()
                val action = AllCategoriesFragmentDirections.actionAllCategoriesToAllDoctors(
                    categoryId = category.id,
                    categoryName = category.name
                )
                findNavController().navigate(action)
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rcvAllCategories.apply {
            adapter = allCategoriesAdapter
            layoutManager = GridLayoutManager(requireContext(), 3)
            // Tắt animation để tăng hiệu suất
            itemAnimator = null
            // Tối ưu performance
            setHasFixedSize(true)
        }
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Quan sát dữ liệu categories
                viewModel.categories.collectLatest { state ->
                    when (state) {
                        is UiState.Success -> {
                            val categories = state.data
                            // Lazy loading để tăng tốc độ hiển thị ban đầu
                            if (isFirstLoad && categories.size > 10) {
                                // Hiển thị 10 item đầu tiên trước
                                allCategoriesAdapter.setCategories(categories.take(10))
                                
                                // Sau đó mới hiển thị đầy đủ - trong coroutine scope
                                launch {
                                    delay(150)
                                    allCategoriesAdapter.setCategories(categories)
                                    isFirstLoad = false
                                }
                            } else {
                                allCategoriesAdapter.setCategories(categories)
                                if (isFirstLoad) isFirstLoad = false
                            }
                        }
                        is UiState.Error -> {
                            // Hiển thị thông báo lỗi
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG)
                                .setAction("Retry") {
                                    viewModel.refreshCategories()
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
//                    binding.progressBarAllCategories.visibility = if (isLoaded) View.GONE else View.VISIBLE
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
            binding.rcvAllCategories.adapter = null
            _binding = null
        }
        super.cleanupViewReferences()
    }
}