package com.healthtech.doccareplus.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.R
import com.healthtech.doccareplus.data.local.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {
    private val _startDestination = MutableStateFlow<Int>(0)
    val startDestination = _startDestination.asStateFlow()

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            _startDestination.value = if (userPreferences.isUserLoggedIn()) {
                R.id.homeFragment
            } else {
                R.id.loginFragment
            }
        }
    }
}