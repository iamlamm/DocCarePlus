package com.healthtech.doccareplus.ui.profile.editprofile

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.databinding.FragmentEditProfileBinding
import com.healthtech.doccareplus.domain.model.Gender
import com.healthtech.doccareplus.domain.model.User
import com.healthtech.doccareplus.domain.service.CloudinaryUploadState
import com.healthtech.doccareplus.ui.profile.ProfileState
import com.healthtech.doccareplus.ui.profile.ProfileViewModel
import com.healthtech.doccareplus.ui.profile.UpdateProfileState
import com.healthtech.doccareplus.utils.SnackbarUtils
import com.healthtech.doccareplus.utils.showErrorDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private var progressDialog: ProgressDialog? = null
    private var currentUser: User? = null
    
    private val getImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadAvatar(uri)
            }
        }
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            SnackbarUtils.showErrorSnackbar(
                binding.root,
                "Cần quyền truy cập để chọn ảnh"
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupClickListeners()
        setupBloodTypeDropdown()
        observeEmailChangeState()
        observePendingEmailChange()
        observeAvatarUploadState()
        observeUpdateProfileState()
        populateUserData()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { navigateBack() }
    }

    private fun setupClickListeners() {
        binding.apply {
            btnBack.setOnClickListener { navigateBack() }
            
            btnSave.setOnClickListener { 
                saveUserProfile()
            }
            
            btnChangeEmail.setOnClickListener {
                showChangeEmailDialog()
            }
            
            btnChangeAvatar.setOnClickListener {
                checkAndRequestPermission()
            }
        }
    }
    
    private fun checkAndRequestPermission() {
        when {
            // Android 13+ uses more specific permissions
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        openImagePicker()
                    }
                    else -> {
                        requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                }
            }
            // Older Android versions
            else -> {
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        openImagePicker()
                    }
                    else -> {
                        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            }
        }
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getImageLauncher.launch(intent)
    }
    
    private fun uploadAvatar(imageUri: Uri) {
        viewModel.uploadAvatar(imageUri)
    }
    
    private fun observeAvatarUploadState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.avatarUploadState.collect { state ->
                when (state) {
                    is CloudinaryUploadState.Idle -> {
                        // Không làm gì
                    }
                    
                    is CloudinaryUploadState.Loading -> {
                        if (progressDialog == null) {
                            progressDialog = ProgressDialog(requireContext()).apply {
                                setMessage("Đang tải lên ảnh đại diện (${state.progress}%)...")
                                setCancelable(false)
                                show()
                            }
                        } else {
                            progressDialog?.setMessage("Đang tải lên ảnh đại diện (${state.progress}%)...")
                        }
                    }
                    
                    is CloudinaryUploadState.Success -> {
                        progressDialog?.dismiss()
                        progressDialog = null
                        SnackbarUtils.showSuccessSnackbar(
                            binding.root,
                            "Đã cập nhật ảnh đại diện thành công"
                        )
                        viewModel.resetAvatarUploadState()
                    }
                    
                    is CloudinaryUploadState.Error -> {
                        progressDialog?.dismiss()
                        progressDialog = null
                        showErrorDialog(
                            title = "Lỗi",
                            message = state.message,
                            positiveText = "OK"
                        )
                        viewModel.resetAvatarUploadState()
                    }
                }
            }
        }
    }

    private fun observeEmailChangeState() {
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

    private fun observePendingEmailChange() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pendingEmail.collect { pendingEmail ->
                if (pendingEmail != null) {
                    binding.etEmail.isEnabled = false
                    binding.etEmail.hint = "Đang chờ xác thực: $pendingEmail"
                    binding.btnChangeEmail.text = "Hủy thay đổi"
                    binding.btnChangeEmail.setOnClickListener {
                        viewModel.cancelEmailChange()
                    }
                } else {
                    binding.etEmail.isEnabled = true
                    binding.etEmail.hint = null
                    binding.btnChangeEmail.text = "Thay đổi email"
                    binding.btnChangeEmail.setOnClickListener {
                        showChangeEmailDialog()
                    }
                }
            }
        }
    }

    private fun showChangeEmailDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_email, null)
        val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.password_input)
        val newEmailInput = dialogView.findViewById<TextInputEditText>(R.id.new_email_input)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Thay đổi email")
            .setView(dialogView)
            .setPositiveButton("Xác nhận") { _, _ ->
                val password = passwordInput.text.toString()
                val newEmail = newEmailInput.text.toString()

                if (password.isNotEmpty() && newEmail.isNotEmpty()) {
                    viewModel.updateEmail(password, newEmail)
                } else {
                    SnackbarUtils.showErrorSnackbar(
                        binding.root,
                        "Vui lòng nhập đầy đủ thông tin"
                    )
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun saveUserProfile() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val about = binding.etAbout.text.toString().trim()
        val heightStr = binding.etHeight.text.toString().trim()
        val weightStr = binding.etWeight.text.toString().trim()
        val ageStr = binding.etAge.text.toString().trim()
        val bloodType = binding.etBloodType.text.toString().trim()
        
        // Xác định giới tính được chọn
        val gender = when {
            binding.rbMale.isChecked -> Gender.MALE
            binding.rbFemale.isChecked -> Gender.FEMALE
            else -> Gender.OTHER
        }
        
        // Validate
        if (name.isEmpty()) {
            binding.tilName.error = "Vui lòng nhập tên của bạn"
            return
        }
        
        // Create updated user object
        currentUser?.let { user ->
            val updatedUser = user.copy(
                name = name,
                phoneNumber = if (phone.isNotEmpty()) phone else user.phoneNumber,
                about = about,
                height = if (heightStr.isNotEmpty()) heightStr.toInt() else user.height,
                weight = if (weightStr.isNotEmpty()) weightStr.toInt() else user.weight,
                age = if (ageStr.isNotEmpty()) ageStr.toInt() else user.age,
                bloodType = if (bloodType.isNotEmpty()) bloodType else user.bloodType,
                gender = gender
            )
            
            viewModel.updateProfile(updatedUser)
        }
    }
    
    private fun observeUpdateProfileState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updateProfileState.collect { state ->
                when (state) {
                    is UpdateProfileState.Idle -> {
                        // Do nothing
                    }
                    is UpdateProfileState.Loading -> {
                        // Show loading
                        SnackbarUtils.showInfoSnackbar(binding.root, "Đang cập nhật thông tin...")
                    }
                    is UpdateProfileState.Success -> {
                        SnackbarUtils.showSuccessSnackbar(binding.root, state.message)
                        viewModel.resetUpdateProfileState()
                        // Delay trước khi quay về màn hình profile
                        lifecycleScope.launch {
                            kotlinx.coroutines.delay(1000)
                            navigateBack()
                        }
                    }
                    is UpdateProfileState.Error -> {
                        showErrorDialog(
                            title = "Lỗi",
                            message = state.message,
                            positiveText = "OK"
                        )
                        viewModel.resetUpdateProfileState()
                    }
                }
            }
        }
    }

    private fun populateUserData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.profileState.collect { state ->
                if (state is ProfileState.Success) {
                    currentUser = state.user  // Lưu user hiện tại
                    val user = state.user
                    
                    // Các trường hiện có
                    binding.etName.setText(user.name)
                    binding.etPhone.setText(user.phoneNumber)
                    binding.etEmail.setText(user.email)
                    binding.etAbout.setText(user.about)
                    binding.etHeight.setText(user.height?.toString() ?: "")
                    binding.etWeight.setText(user.weight?.toString() ?: "")
                    binding.etAge.setText(user.age?.toString() ?: "")
                    binding.etBloodType.setText(user.bloodType ?: "")
                    
                    // Thiết lập giới tính
                    when (user.gender) {
                        Gender.MALE -> binding.rbMale.isChecked = true
                        Gender.FEMALE -> binding.rbFemale.isChecked = true
                        Gender.OTHER -> binding.rbOther.isChecked = true
                        else -> Unit
                    }
                    
                    // Avatar
                    if (user.avatar.isNullOrEmpty()) {
                        binding.ivProfile.setImageResource(
                            if (user.gender == Gender.MALE) R.mipmap.avatar_male_default
                            else if (user.gender == Gender.FEMALE) R.mipmap.avatar_female_default
                            else R.mipmap.avatar_bear_default
                        )
                    } else {
                        Glide.with(requireContext())
                            .load(user.avatar)
                            .error(R.mipmap.avatar_bear_default)
                            .into(binding.ivProfile)
                    }
                }
            }
        }
    }

    private fun setupBloodTypeDropdown() {
        val bloodTypes = arrayOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, bloodTypes)
        (binding.etBloodType as? AutoCompleteTextView)?.setAdapter(adapter)
    }

    private fun navigateBack() {
        findNavController().navigate(R.id.action_editProfile_to_previous)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressDialog?.dismiss()
        progressDialog = null
        _binding = null
    }
}