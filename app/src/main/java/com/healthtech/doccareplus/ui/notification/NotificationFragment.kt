package com.healthtech.doccareplus.ui.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.FragmentNotificationBinding
import com.healthtech.doccareplus.ui.notification.adapter.NotificationAdapter
import com.healthtech.doccareplus.ui.notification.viewmodel.NotificationViewModel
import com.healthtech.doccareplus.ui.success.SuccessFragmentDirections
import com.healthtech.doccareplus.ui.widgets.decoration.CustomItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationFragment : Fragment() {
    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationViewModel by viewModels()
    private val adapter = NotificationAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Xử lý nút back
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateBack()
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeNotifications()
        setupToolbar()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener { navigateBack() }
        }
    }

    private fun setupRecyclerView() {
        adapter.setOnNotificationClickListener { notificationId ->
            viewModel.markAsRead(notificationId)
        }

        binding.rcvNotification.apply {
            adapter = this@NotificationFragment.adapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(CustomItemDecoration(context))
        }
    }

    private fun observeNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notifications.collect { notifications ->
                adapter.submitList(notifications)
            }
        }
    }

    private fun navigateBack() {
        findNavController().navigate(R.id.action_notification_to_previous)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}