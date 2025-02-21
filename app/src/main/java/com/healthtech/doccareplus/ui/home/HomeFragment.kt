package com.healthtech.doccareplus.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.FragmentHomeBinding
import com.healthtech.doccareplus.ui.adapter.CategoryAdapter
import com.healthtech.doccareplus.ui.adapter.DoctorAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val categoryAdapter = CategoryAdapter()
    private val doctorAdapter = DoctorAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupRecyclerView()
        setupBannerSlider()
        observeCurrentUser()
        observeCategories()
        observeDoctors()
    }

    private fun setupBannerSlider() {
        try {
            lifecycleScope.launch(Dispatchers.Default) {
                val bannerImages = listOf(
                    R.drawable.banner,
                    R.drawable.banner,
                    R.drawable.banner,
                    R.drawable.banner,
                    R.drawable.banner
                )
                
                withContext(Dispatchers.Main) {
                    binding.bannerSlider.setImages(bannerImages)
                }
            }
        } catch (e: Exception) {
            Log.e("BannerSlider", "Error setting up banner slider", e)
            // Hiển thị fallback UI nếu cần
        }
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
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
        }
    }

    private fun observeCurrentUser() {
        // viewLifecycleOwner.lifecycleScope.launch {
        //     viewModel.currentUser.collect { user ->
        //         binding.tvUserName.text =
        //             user?.name ?: requireContext().getString(R.string.placeholder_username)
        //     }
        // }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentUser.collect { user ->
                user?.let {
                    (activity as? HomeActivity)?.updateUserInfo(it.name)
                }
            }
        }
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categories
                .catch { error ->
                    Toast.makeText(
                        requireContext(),
                        "Error loading categories: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .collect { categories ->
                    categoryAdapter.setCategories(categories)
                }
        }
    }

    private fun observeDoctors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.doctors
                .catch { error ->
                    Toast.makeText(
                        requireContext(),
                        "Error loading doctors: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .collect { doctors ->
                    doctorAdapter.setDoctors(doctors)
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}