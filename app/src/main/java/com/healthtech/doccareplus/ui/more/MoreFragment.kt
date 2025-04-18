package com.healthtech.doccareplus.ui.more

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.FragmentMoreBinding
import com.healthtech.doccareplus.domain.repository.AuthRepository
import com.healthtech.doccareplus.ui.auth.AuthActivity
import com.healthtech.doccareplus.utils.showWarningDialog
import com.healthtech.doccareplus.data.local.preferences.UserPreferences
import com.healthtech.doccareplus.DocCarePlusApplication
import com.healthtech.doccareplus.common.dialogs.LanguageSelectionDialog
import com.healthtech.doccareplus.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MoreFragment : Fragment() {
    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var userPreferences: UserPreferences

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        updateLanguageButtonText()
    }

    private fun setupClickListeners() {
        binding.apply {
            btnProfile.setOnClickListener {
                findNavController().navigate(R.id.action_more_to_profile)
            }

            btnLogout.setOnClickListener {
                showWarningDialog(
                    title = getString(R.string.logout),
                    message = getString(R.string.logout_confirmation),
                    positiveText = getString(R.string.logout),
                    negativeText = getString(R.string.cancel),
                    onPositive = {
                        logout()
                    })
            }

            btnLanguage.setOnClickListener {
                showLanguageSelectionDialog()
            }
        }
    }

    private fun logout() {
        authRepository.logout()
        val intent = Intent(requireContext(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showLanguageSelectionDialog() {
        val currentLanguage = userPreferences.getLanguage()
        LanguageSelectionDialog(requireContext(), currentLanguage) { selectedLanguage ->
            userPreferences.saveLanguage(selectedLanguage)
            (requireActivity().application as DocCarePlusApplication)
                .updateLanguage(requireContext(), selectedLanguage)
            
            // Restart activity to apply changes
            requireActivity().recreate()
        }.show()
    }

    private fun updateLanguageButtonText() {
        val currentLanguage = userPreferences.getLanguage()
        val languageText = when (currentLanguage) {
            Constants.LANGUAGE_ENGLISH -> "English"
            Constants.LANGUAGE_VIETNAMESE -> "Tiếng Việt"
            else -> "English"
        }
        binding.currentLanguageText.text = languageText
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}