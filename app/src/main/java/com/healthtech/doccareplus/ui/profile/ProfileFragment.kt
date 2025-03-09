package com.healthtech.doccareplus.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.FragmentProfileBinding
import com.healthtech.doccareplus.domain.model.Gender
import com.healthtech.doccareplus.domain.model.User
import com.healthtech.doccareplus.ui.auth.AuthActivity
import com.healthtech.doccareplus.ui.profile.editprofile.EmailChangeState
import com.healthtech.doccareplus.utils.SnackbarUtils
import com.healthtech.doccareplus.utils.showErrorDialog
import com.healthtech.doccareplus.utils.showWarningDialog
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
        observePendingEmailChange()

        // Observe email change state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.emailChangeState.collect { state ->
                when (state) {
                    is EmailChangeState.Loading -> {
                        SnackbarUtils.showInfoSnackbar(binding.root, "Đang xử lý...")
                    }

                    is EmailChangeState.Success -> {
                        SnackbarUtils.showSuccessSnackbar(binding.root, state.message)
                    }

                    is EmailChangeState.Error -> {
                        showErrorDialog(
                            title = getString(R.string.error),
                            message = state.message,
                            positiveText = getString(R.string.ok)
                        )
                    }

                    else -> {}
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            btnBack.setOnClickListener { findNavController().navigateUp() }

            btnEditProfile.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_editProfile)
            }

            btnCopyUid.setOnClickListener {
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("UID", binding.tvUid.text)
                clipboard.setPrimaryClip(clip)

                binding.btnCopyUid.animate()
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(100)
                    .withEndAction {
                        binding.btnCopyUid.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                    }
                    .start()

                SnackbarUtils.showSuccessSnackbar(binding.root, getString(R.string.uid_copied))
            }
        }
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
                        showErrorDialog(
                            title = getString(R.string.error),
                            message = state.message,
                            positiveText = getString(R.string.ok)
                        )
                    }
                }
            }
        }
    }

    private fun observePendingEmailChange() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pendingEmail.collect { pendingEmail ->
                if (pendingEmail != null) {
                    binding.pendingEmailBanner.visibility = View.VISIBLE
                    binding.pendingEmailText.text =
                        "Đang chờ xác thực email mới: $pendingEmail"
                    binding.cancelEmailChange.setOnClickListener {
                        viewModel.cancelEmailChange()
                    }
                } else {
                    binding.pendingEmailBanner.visibility = View.GONE
                }
            }
        }
    }

    private fun updateUI(user: User) {
        binding.apply {
            tvUid.text = user.id


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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}