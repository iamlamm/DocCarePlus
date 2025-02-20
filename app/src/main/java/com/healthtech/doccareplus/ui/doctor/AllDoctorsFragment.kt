package com.healthtech.doccareplus.ui.doctor

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.healthtech.doccareplus.databinding.FragmentAllDoctorsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AllDoctorsFragment : Fragment() {
    private var _binding: FragmentAllDoctorsBinding? = null
    val binding get() = _binding!!
    private val viewModel: AllDoctorsViewModel by viewModels()
    private val allDoctorsAdapter = AllDoctorsAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAllDoctorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupToolbar()
        observeDoctors()
    }

    @SuppressLint("ShowToast")
    private fun observeDoctors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.doctors.catch { error ->
                Snackbar.make(binding.root, error.message!!, Snackbar.LENGTH_LONG)
            }
                .collect { doctors ->
                    allDoctorsAdapter.setDoctors(doctors)
                }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener {
                requireActivity().onBackPressed()
            }
            title = "Find your Doctor"
        }
    }

    private fun setupRecyclerView() {
        binding.rcvAllDoctors.apply {
            adapter = allDoctorsAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}