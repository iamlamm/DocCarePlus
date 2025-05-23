package com.healthtech.doccareplus.ui.auth.forgotpassword

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.FragmentForgotPasswordBinding
import com.healthtech.doccareplus.utils.ValidationUtils
import com.healthtech.doccareplus.utils.showErrorDialog
import com.healthtech.doccareplus.utils.showSuccessDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {
    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ForgotPasswordViewModel by viewModels()
    private var hasEmailFocused = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupEmailValidation()
        setupClickListeners()
        observeForgotPasswordState()
    }


    private fun setupClickListeners() {
        binding.apply {
            btnForgotPasswordSubmit.setOnClickListener {
                hideKeyboard()
                val email = etForgotPasswordEmail.text.toString()
                viewModel.resetPassword(email)
            }
            tvForgotPasswordToLogin.setOnClickListener {
                findNavController().navigate(R.id.action_forgot_password_to_login)
            }
        }
    }

    private fun observeForgotPasswordState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.forgotPasswordState.collect { state ->
                when (state) {
                    is ForgotPasswordState.Loading -> {
                        binding.progressBarForgotPassword.visibility = View.VISIBLE
                        binding.btnForgotPasswordSubmit.isEnabled = false
                    }

                    is ForgotPasswordState.Success -> {
                        binding.progressBarForgotPassword.visibility = View.GONE
                        binding.btnForgotPasswordSubmit.isEnabled = true
//                        showSuccessDialog()
                        showSuccessDialog(title = "Thành công",
                            message = "Email khôi phục mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư của bạn.",
                            positiveText = "Đến đăng nhập",
                            cancelable = false,
                            onPositive = {
                                viewModel.resetForgotPasswordState()
                                findNavController().navigate(R.id.action_forgot_password_to_login)
                            })
                    }

                    is ForgotPasswordState.Error -> {
                        binding.progressBarForgotPassword.visibility = View.GONE
                        binding.btnForgotPasswordSubmit.isEnabled = true
//                        showErrorDialog(state.message)
                        showErrorDialog(title = "Lỗi",
                            message = state.message,
                            positiveText = "Đã hiểu",
                            onPositive = {
                                viewModel.resetForgotPasswordState()
                            })
                    }

                    else -> {
                        binding.progressBarForgotPassword.visibility = View.GONE
                        binding.btnForgotPasswordSubmit.isEnabled = true
                    }
                }
            }
        }
    }

//    private fun showErrorDialog(message: String) {
//        MaterialAlertDialogBuilder(requireContext())
//            .setTitle("Lỗi")
//            .setMessage(message)
//            .setPositiveButton("Đã hiểu", null)
//            .show()
//    }
//
//    private fun showSuccessDialog() {
//        MaterialAlertDialogBuilder(requireContext())
//            .setTitle("Thành công")
//            .setMessage("Email khôi phục mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư của bạn.")
//            .setPositiveButton("Đến đăng nhập") { _, _ ->
//                findNavController().navigate(R.id.action_forgot_password_to_login)
//            }
//            .setCancelable(false)
//            .show()
//    }

    private fun setupEmailValidation() {
        binding.etForgotPasswordEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) hasEmailFocused = true
            if (!hasFocus) validateEmail()
        }

        binding.etForgotPasswordEmail.doOnTextChanged { _, _, _, _ ->
            if (hasEmailFocused) validateEmail()
        }
    }

    private fun validateEmail(): Boolean {
        val email = binding.etForgotPasswordEmail.text.toString()
        return when {
            !hasEmailFocused && email.isEmpty() -> {
                binding.tilForgotPasswordEmail.error = null
                false
            }

            hasEmailFocused && email.isEmpty() -> {
                binding.tilForgotPasswordEmail.error = getString(R.string.email_required)
                false
            }

            !ValidationUtils.isValidEmail(email) -> {
                binding.tilForgotPasswordEmail.error = getString(R.string.invalid_email)
                false
            }

            else -> {
                binding.tilForgotPasswordEmail.error = null
                true
            }
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocusedView = requireActivity().currentFocus
        if (currentFocusedView != null) {
            inputMethodManager.hideSoftInputFromWindow(currentFocusedView.windowToken, 0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}