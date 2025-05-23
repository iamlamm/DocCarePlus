package com.healthtech.doccareplus.ui.auth.register

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.FragmentRegisterBinding
import com.healthtech.doccareplus.utils.ValidationUtils
import com.healthtech.doccareplus.utils.showErrorDialog
import com.healthtech.doccareplus.utils.showSuccessDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegisterViewModel by viewModels()


    private var hasNameFocused = false
    private var hasEmailFocused = false
    private var hasPhoneFocused = false
    private var hasPasswordFocused = false
    private var hasConfirmPasswordFocused = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFocusListeners()
        setupTextWatchers()
        setupClickListeners()
        observeRegisterState()
    }

    private fun createTextWatcher(validateFunction: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                validateFunction()
                updateButtonState()
            }

        }
    }

    private fun setupTextWatchers() {
        binding.apply {
            etRegisterName.addTextChangedListener(createTextWatcher { validateName() })
            etRegisterEmail.addTextChangedListener(createTextWatcher { validateEmail() })
            etRegisterPhone.addTextChangedListener(createTextWatcher { validatePhone() })
            etRegisterPassword.addTextChangedListener(createTextWatcher {
                validatePassword()
                validateConfirmPassword() // Kiểm tra lại confirm password khi password thay đổi
            })
            etRegisterConfirmPassword.addTextChangedListener(createTextWatcher { validateConfirmPassword() })
        }
    }

    private fun setupFocusListeners() {
        binding.apply {
            binding.apply {
                etRegisterName.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) hasNameFocused = true
                }
                etRegisterEmail.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) hasEmailFocused = true
                }
                etRegisterPhone.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) hasPhoneFocused = true
                }
                etRegisterPassword.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) hasPasswordFocused = true
                }
                etRegisterConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) hasConfirmPasswordFocused = true
                }
            }
        }
    }

    private fun validateName(): Boolean {
        val name = binding.etRegisterName.text.toString()
        return when {
            !hasNameFocused && name.isEmpty() -> {
                binding.tilRegisterName.error = null
                false
            }

            hasNameFocused && name.isEmpty() -> {
                binding.tilRegisterName.error = getString(R.string.name_required)
                false
            }

            !ValidationUtils.isValidName(name) -> {
                binding.tilRegisterName.error = getString(R.string.invalid_name)
                false
            }

            else -> {
                binding.tilRegisterName.error = null
                true
            }
        }
    }

    private fun validateEmail(): Boolean {
        val email = binding.etRegisterEmail.text.toString()
        return when {
            !hasEmailFocused && email.isEmpty() -> {
                binding.tilRegisterEmail.error = null
                false
            }

            hasEmailFocused && email.isEmpty() -> {
                binding.tilRegisterEmail.error = getString(R.string.email_required)
                false
            }

            !ValidationUtils.isValidEmail(email) -> {
                binding.tilRegisterEmail.error = getString(R.string.invalid_email)
                false
            }

            else -> {
                binding.tilRegisterEmail.error = null
                true
            }
        }
    }

    private fun validatePhone(): Boolean {
        val phone = binding.etRegisterPhone.text.toString()
        return when {
            !hasPhoneFocused && phone.isEmpty() -> {
                binding.tilRegisterPhone.error = null
                false
            }

            hasPhoneFocused && phone.isEmpty() -> {
                binding.tilRegisterPhone.error = getString(R.string.phone_required)
                false
            }

            !ValidationUtils.isValidPhoneNumber(phone) -> {
                binding.tilRegisterPhone.error = getString(R.string.invalid_phone)
                false
            }

            else -> {
                binding.tilRegisterPhone.error = null
                true
            }
        }
    }

    private fun validatePassword(): Boolean {
        val password = binding.etRegisterPassword.text.toString()
        return when {
            !hasPasswordFocused && password.isEmpty() -> {
//                binding.tilRegisterPassword.error = null
//                false
                binding.tilRegisterPassword.helperText = null
                binding.tilRegisterPassword.isHelperTextEnabled = false
                false
            }

            hasPasswordFocused && password.isEmpty() -> {
//                binding.tilRegisterPassword.error = getString(R.string.password_required)
//                false
                binding.tilRegisterPassword.helperText = getString(R.string.password_required)
                binding.tilRegisterPassword.isHelperTextEnabled = true
                false
            }

            !ValidationUtils.isValidPassword(password) -> {
//                binding.tilRegisterPassword.error = getString(R.string.invalid_password)
//                false
                binding.tilRegisterPassword.helperText = getString(R.string.invalid_password)
                binding.tilRegisterPassword.isHelperTextEnabled = true
                false
            }

            else -> {
//                binding.tilRegisterPassword.error = null
//                true
                binding.tilRegisterPassword.helperText = null
                binding.tilRegisterPassword.isHelperTextEnabled = false
                true
            }
        }
    }

    private fun validateConfirmPassword(): Boolean {
        val password = binding.etRegisterPassword.text.toString()
        val confirmPassword = binding.etRegisterConfirmPassword.text.toString()
        return when {
            !hasConfirmPasswordFocused && confirmPassword.isEmpty() -> {
//                binding.tilRegisterConfirmPassword.error = null
//                false
                binding.tilRegisterConfirmPassword.helperText = null
                binding.tilRegisterConfirmPassword.isHelperTextEnabled = false
                false
            }

            hasConfirmPasswordFocused && confirmPassword.isEmpty() -> {
//                binding.tilRegisterConfirmPassword.error =
//                    getString(R.string.confirm_password_required)
//                false
                binding.tilRegisterConfirmPassword.helperText =
                    getString(R.string.confirm_password_required)
                binding.tilRegisterConfirmPassword.isHelperTextEnabled = true
                false
            }

            password != confirmPassword -> {
//                binding.tilRegisterConfirmPassword.error = getString(R.string.passwords_not_match)
//                false
                binding.tilRegisterConfirmPassword.helperText =
                    getString(R.string.passwords_not_match)
                binding.tilRegisterConfirmPassword.isHelperTextEnabled = true
                false
            }

            else -> {
//                binding.tilRegisterConfirmPassword.error = null
//                true
                binding.tilRegisterConfirmPassword.helperText = null
                binding.tilRegisterConfirmPassword.isHelperTextEnabled = false
                true
            }
        }
    }

    private fun updateButtonState() {
        binding.btnRegisterSubmit.isEnabled =
            validateName() && validateEmail() && validatePhone() && validatePassword() && validateConfirmPassword()
    }

    private fun setupClickListeners() {
        binding.btnRegisterSubmit.setOnClickListener {

            val name = binding.etRegisterName.text.toString()
            val email = binding.etRegisterEmail.text.toString()
            val password = binding.etRegisterPassword.text.toString()
            val phoneNumber = binding.etRegisterPhone.text.toString()

            viewModel.register(name, email, password, phoneNumber)
        }

        binding.tvRegisterToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    private fun observeRegisterState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.registerState.collect { state ->
                when (state) {
                    is RegisterState.Loading -> {
                        binding.progressBarRegister.setLoading(true)
                        binding.btnRegisterSubmit.isEnabled = false
                    }

                    is RegisterState.EmailVerificationSent -> {
                        binding.progressBarRegister.setLoading(false)
                        binding.btnRegisterSubmit.isEnabled = true

//                        MaterialAlertDialogBuilder(requireContext())
//                            .setTitle("Xác thực Email")
//                            .setMessage("Chúng tôi đã gửi email xác thực đến địa chỉ email của bạn. Vui lòng kiểm tra và xác thực tài khoản trước khi đăng nhập.")
//                            .setPositiveButton("Đến đăng nhập") { _, _ ->
//                                findNavController().navigate(R.id.action_register_to_login)
//                            }
//                            .setCancelable(false)
//                            .show()
                        showSuccessDialog(
                            title = "Xác thực Email",
                            message = "Chúng tôi đã gửi email xác thực đến địa chỉ email của bạn. Vui lòng kiểm tra và xác thực tài khoản trước khi đăng nhập.",
                            positiveText = "Đến đăng nhập",
                            cancelable = false,
                            onPositive = {
                                findNavController().navigate(R.id.action_register_to_login)
                            })
                    }

                    is RegisterState.Error -> {
                        binding.progressBarRegister.setLoading(false)
                        binding.btnRegisterSubmit.isEnabled = true
                        showErrorDialog(
                            title = "Thông báo",
                            message = state.message,
                            positiveText = "Đã hiểu",
                            onPositive = {
                                viewModel.resetRegisterState()
                            })

//                        MaterialAlertDialogBuilder(requireContext()).setTitle("Thông báo")
//                            .setMessage(state.message).setPositiveButton("Đã hiểu") { _, _ ->
//                                viewModel.resetRegisterState()
//                            }
////                                // Nếu lỗi liên quan đến email đã xác thực, chuyển về màn đăng nhập
////                                if (state.message.contains("đã được đăng ký và xác thực")) {
////                                    findNavController().navigate(R.id.action_register_to_login)
////                                }
//
//                            .show()
                    }

                    else -> {
                        binding.progressBarRegister.setLoading(false)
                        binding.btnRegisterSubmit.isEnabled = true
                    }
                }
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