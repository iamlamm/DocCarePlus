package com.healthtech.doccareplus.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.FragmentProfileBinding
import com.healthtech.doccareplus.domain.model.Gender
import com.healthtech.doccareplus.domain.model.User
import com.healthtech.doccareplus.ui.auth.AuthActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeProfileState()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnLogout.setOnClickListener { showLogoutDialog() }
    }

    private fun observeProfileState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.profileState.collect { state ->
                when (state) {
                    is ProfileState.Idle -> {
                    }

                    is ProfileState.Success -> {
                        updateUI(state.user)
                    }

                    is ProfileState.Error -> {
                        showErrorDialog(state.message)
                    }
                }
            }
        }
    }

    private fun updateUI(user: User) {
        binding.apply {
            tvUserName.text = user.name
//            tvEmail.text = user.email
//            tvPhone.text = user.phoneNumber
            // Stats
            tvHeight.text =
                getString(R.string.format_profile_height, user.height?.toString() ?: "-")
            tvWeight.text =
                getString(R.string.format_profile_weight, user.weight?.toString() ?: "-")
            tvAge.text = getString(R.string.format_profile_age, user.age?.toString() ?: "-")
            tvBloodType.text = getString(R.string.format_profile_blood, user.bloodType ?: "-")

            // About
            tvAboutContent.text = user.about ?: getString(R.string.no_about_content)

            // Avatar
            if (user.avatar.isNullOrEmpty()) {
                ivProfile.setImageResource(
                    if (user.gender == Gender.MALE) R.mipmap.avatar_male_default
                    else if (user.gender == Gender.FEMALE) R.mipmap.avatar_female_default
                    else R.mipmap.avatar_bear_default
                )
            } else {
                Glide.with(binding.root.context).load(user.avatar)
                    .error(R.mipmap.avatar_bear_default).into(ivProfile)
            }
        }
    }

    private fun showErrorDialog(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.error))
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.logout))
            .setMessage(getString(R.string.logout_confirmation))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.logout)) { _, _ ->
                logout()
            }
            .show()
    }

    private fun logout() {
        viewModel.logout()
        val intent = Intent(requireContext(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}