package com.healthtech.doccareplus.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.data.local.preferences.UserPreferences
import com.healthtech.doccareplus.databinding.ActivityHomeBinding
import com.healthtech.doccareplus.domain.model.Category
import com.healthtech.doccareplus.domain.model.Doctor
import com.healthtech.doccareplus.domain.model.Gender
import com.healthtech.doccareplus.domain.model.User
import com.healthtech.doccareplus.domain.repository.AuthRepository
import com.healthtech.doccareplus.domain.repository.CategoryRepository
import com.healthtech.doccareplus.domain.repository.DoctorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val doctorRepository: DoctorRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    val categories: Flow<List<Category>> = categoryRepository.getCategories()
    val doctors: Flow<List<Doctor>> = doctorRepository.getDoctors()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val result = authRepository.getCurrentUser()
                result.onSuccess { user ->
                    _currentUser.value = user
                }.onFailure { error ->
                    // Handle error if needed
                }
            } catch (e: Exception) {
                // Handle exception if needed
            }
        }
    }

    fun updateUserUI(binding: ActivityHomeBinding, user: User) {
        binding.tvUserName.text = user.name

        binding.ivUserAvatar.apply {
            if (user.avatar.isNullOrEmpty()) {
                setImageResource(
                    when (user.gender) {
                        Gender.MALE -> R.mipmap.avatar_male_default
                        Gender.FEMALE -> R.mipmap.avatar_female_default
                        else -> R.mipmap.avatar_bear_default
                    }
                )
            } else {
                Glide.with(this)
                    .load(user.avatar)
                    .error(R.mipmap.avatar_bear_default)
                    .into(this)
            }
        }
    }
}