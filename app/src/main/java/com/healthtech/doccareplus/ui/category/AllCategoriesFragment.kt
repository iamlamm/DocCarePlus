package com.healthtech.doccareplus.ui.category

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.common.base.BaseFragment
import com.healthtech.doccareplus.common.state.UiState
import com.healthtech.doccareplus.databinding.FragmentAllCategoriesBinding
import com.healthtech.doccareplus.ui.category.adapter.AllCategoriesAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
        setupSearchView()
        setupBackPressHandling()

        // Kiểm tra trạng thái đã load dữ liệu chưa
        if (!hasSetupObservers) {
            observeCategories()
            observeSearchResults()
            hasSetupObservers = true
        }
    }

    private fun setupBackPressHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.searchView.visibility == View.VISIBLE) {
                        toggleSearchView()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            })
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            title = "Find Your Category"

            // Xử lý click vào nút search
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_search -> {
                        toggleSearchView()
                        true
                    }

                    else -> false
                }
            }
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
                    categoryId = category.id, categoryName = category.name
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

    private fun setupSearchView() {
        binding.searchView.apply {
            // Đặt queryHint
            queryHint = "Search categories..."

            // Xử lý sự kiện thay đổi text
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    hideKeyboard()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.setSearchQuery(newText ?: "")
                    return true
                }
            })

            // Xử lý nút close
            setOnCloseListener {
                toggleSearchView()
                true
            }
        }
    }

    private fun observeCategories() {
//         viewLifecycleOwner.lifecycleScope.launch {
//             viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                 // Quan sát dữ liệu categories
//                 viewModel.categories.collectLatest { state ->
//                     when (state) {
//                         is UiState.Success -> {
//                             val categories = state.data
//                             // Lazy loading để tăng tốc độ hiển thị ban đầu
//                             if (isFirstLoad && categories.size > 10) {
//                                 // Hiển thị 10 item đầu tiên trước
//                                 allCategoriesAdapter.setCategories(categories.take(10))

//                                 // Sau đó mới hiển thị đầy đủ - trong coroutine scope
//                                 launch {
//                                     delay(150)
//                                     allCategoriesAdapter.setCategories(categories)
//                                     isFirstLoad = false
//                                 }
//                             } else {
//                                 allCategoriesAdapter.setCategories(categories)
//                                 if (isFirstLoad) isFirstLoad = false
//                             }
//                         }
//                         is UiState.Error -> {
//                             // Hiển thị thông báo lỗi
//                             Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG)
//                                 .setAction("Retry") {
//                                     viewModel.refreshCategories()
//                                 }
//                                 .show()
//                         }
//                         else -> {
//                             // Xử lý các trạng thái khác
//                         }
//                     }
//                 }

//                 // Quan sát trạng thái đã tải xong chưa
//                 viewModel.isInitialDataLoaded.collect { isLoaded ->
// //                    binding.progressBarAllCategories.visibility = if (isLoaded) View.GONE else View.VISIBLE
//                 }
//             }
//         }
        viewModel.categories.collectLatestWithLifecycle { state ->
            when (state) {
                is UiState.Success -> {
                    val categories = state.data
                    // Lazy loading để tăng tốc độ hiển thị ban đầu
                    if (isFirstLoad && categories.size > 10) {
                        // Hiển thị 10 item đầu tiên trước
                        allCategoriesAdapter.setCategories(categories.take(10))

                        // Sau đó mới hiển thị đầy đủ - trong coroutine scope
                        viewLifecycleOwner.lifecycleScope.launch {
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
                        }.show()
                }

                else -> {
                    // Xử lý các trạng thái khác
                }
            }
        }

        // Quan sát trạng thái đã tải xong chưa - dùng collectWithLifecycle
        viewModel.isInitialDataLoaded.collectWithLifecycle { isLoaded ->
            // binding.progressBarAllCategories.visibility = if (isLoaded) View.GONE else View.VISIBLE
        }
    }

    private fun observeSearchResults() {
        viewModel.searchResults.collectWithLifecycle { results ->
            allCategoriesAdapter.setCategories(results)
        }
    }

    // Toggle hiển thị SearchView
    private fun toggleSearchView() {
        binding.searchView.apply {
            if (visibility == View.VISIBLE) {
                visibility = View.GONE
                viewModel.clearSearch() // Xóa kết quả tìm kiếm
                binding.toolbar.title = "Find Your Category"
            } else {
                visibility = View.VISIBLE
                binding.toolbar.title = ""
                requestFocus() // Hiển thị bàn phím
            }
        }
    }

    // Helper method để ẩn bàn phím
    private fun hideKeyboard() {
        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.searchView.windowToken, 0)
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