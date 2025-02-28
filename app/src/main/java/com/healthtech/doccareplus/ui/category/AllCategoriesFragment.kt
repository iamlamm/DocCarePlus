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

        setupToolbar()
        setupAdapter()
        setupRecyclerView()
        setupSearchView()
        setupBackPressHandling()
        observeCategories()
        observeSearchResults()
    }

    private fun setupBackPressHandling() {
//        requireActivity().onBackPressedDispatcher.addCallback(
//            viewLifecycleOwner,
//            object : OnBackPressedCallback(true) {
//                override fun handleOnBackPressed() {
//                    if (binding.searchView.visibility == View.VISIBLE) {
//                        toggleSearchView()
//                    } else {
//                        isEnabled = false
//                        requireActivity().onBackPressedDispatcher.onBackPressed()
//                    }
//                }
//            })
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.searchView.visibility == View.VISIBLE) {
                        binding.searchView.setQuery("", false)
                        hideKeyboard()
                        binding.searchView.clearFocus()
                        binding.searchView.visibility = View.GONE
                        viewModel.setSearchActive(false)
                        binding.toolbar.title = "Find Your Category"
                    } else {
                        // Cho phép hành vi back mặc định
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            })
    }


    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
//                requireActivity().onBackPressedDispatcher.onBackPressed()
                closeSearchAndNavigateBack()
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
            itemAnimator = null
            setHasFixedSize(true)
        }
    }

    private fun setupSearchView() {
        binding.searchView.apply {
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
                viewModel.setSearchActive(false)
                binding.toolbar.title = "Find Your Category"
            } else {
                visibility = View.VISIBLE
                viewModel.setSearchActive(true)
                binding.toolbar.title = ""
                requestFocus()
            }
        }
    }

    // Helper method để ẩn bàn phím
    private fun hideKeyboard() {
        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.searchView.windowToken, 0)
    }

    private fun closeSearchAndNavigateBack() {
        if (binding.searchView.visibility == View.VISIBLE) {
//            binding.searchView.setQuery("", false)
//            hideKeyboard()
//            binding.searchView.clearFocus()
            binding.searchView.visibility = View.GONE
//            viewModel.setSearchActive(false)
        }
//        requireActivity().onBackPressedDispatcher.onBackPressed()
        findNavController().navigateUp()

    }

    // Thay đổi onResume để tránh refresh ngay lập tức
    override fun onResume() {
        super.onResume()

        // Khôi phục trạng thái SearchView dựa trên isSearchActive từ ViewModel
        viewModel.isSearchActive.value.let { isActive ->
            if (isActive) {
                if (binding.searchView.visibility != View.VISIBLE) {
                    binding.searchView.visibility = View.VISIBLE
                    binding.toolbar.title = ""
                }
            } else {
                binding.searchView.visibility = View.GONE
                binding.toolbar.title = "Find Your Category"
            }
        }

        if (::allCategoriesAdapter.isInitialized &&
            allCategoriesAdapter.itemCount == 0 &&
            binding.rcvAllCategories != null
        ) {

            // Làm mới dữ liệu khi quay lại fragment
            viewModel.refreshCategories()

            // Delay nhỏ để animation hoàn tất trước khi hiển thị dữ liệu
            viewLifecycleOwner.lifecycleScope.launch {
                delay(100)
                // Hiển thị RecyclerView nếu nó đang bị ẩn
                if (binding.rcvAllCategories.visibility != View.VISIBLE) {
                    binding.rcvAllCategories.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun cleanupViewReferences() {
        if (_binding != null) {
            binding.rcvAllCategories.adapter = null
            binding.searchView.setQuery("", false)
            binding.searchView.clearFocus()
            _binding = null
        }
        super.cleanupViewReferences()
    }
}