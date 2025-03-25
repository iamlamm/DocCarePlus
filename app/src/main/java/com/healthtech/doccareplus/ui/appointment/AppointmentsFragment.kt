package com.healthtech.doccareplus.ui.appointment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.healthtech.doccareplus.databinding.FragmentAppointmentsBinding
import com.healthtech.doccareplus.ui.appointment.adapter.AppointmentsAdapter
import com.healthtech.doccareplus.utils.AnimationUtils
import com.healthtech.doccareplus.utils.AnimationUtils.showWithAnimation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class AppointmentsFragment : Fragment() {

    private var _binding: FragmentAppointmentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AppointmentsViewModel by viewModels()
    private lateinit var appointmentsAdapter: AppointmentsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentUser.collect { user ->
                Timber.d("Current user: " + user?.id + ", name: " + user?.name)
            }
        }

        setupUI()
        setupObservers()

        // Animate header elements và content
        AnimationUtils.fadeInSequentially(
            views = listOf(
                binding.toolbar,
                binding.tabLayout,
                binding.rvAppointments
            ),
            duration = 600,
            delayBetween = 200,
            type = AnimationUtils.AnimationType.SLIDE_RIGHT
        )

        // Thêm animation cho empty state nếu cần
        binding.emptyState.showWithAnimation(
            duration = 600,
            type = AnimationUtils.AnimationType.FADE,
            delay = 300
        )
    }

    private fun setupUI() {
        // Thiết lập adapter và recycler view
        appointmentsAdapter = AppointmentsAdapter()
        binding.rvAppointments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = appointmentsAdapter
        }

        // Thiết lập tabs
        binding.tabLayout.apply {
            addTab(newTab().setText("Tất cả"))
            addTab(newTab().setText("Sắp tới"))
            addTab(newTab().setText("Hoàn thành"))
            addTab(newTab().setText("Đã hủy"))

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    when (tab?.position) {
                        0 -> viewModel.filterAppointments(null) // Tất cả
                        1 -> viewModel.filterAppointments("upcoming")
                        2 -> viewModel.filterAppointments("completed")
                        3 -> viewModel.filterAppointments("cancelled")
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }

        // Nút back
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupObservers() {
        viewModel.appointments.observe(viewLifecycleOwner) { appointments ->
            Timber.d("Observed " + appointments.size + " appointments")

            binding.progressBar.visibility = View.GONE

            if (appointments.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvAppointments.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvAppointments.visibility = View.VISIBLE
                appointmentsAdapter.submitList(appointments)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}