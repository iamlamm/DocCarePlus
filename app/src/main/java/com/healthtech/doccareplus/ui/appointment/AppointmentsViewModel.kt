package com.healthtech.doccareplus.ui.appointment

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.domain.model.Appointment
import com.healthtech.doccareplus.domain.model.User
import com.healthtech.doccareplus.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppointmentsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _appointments = MutableLiveData<List<Appointment>>()
    val appointments: LiveData<List<Appointment>> = _appointments
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser
    
    private var allAppointments: List<Appointment> = emptyList()
    
    init {
        loadCurrentUser()
        loadAppointments()
    }
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                _currentUser.value = user
                Log.d("AppointmentsViewModel", "Current user loaded: ${user?.id}")
            } catch (e: Exception) {
                Log.e("AppointmentsViewModel", "Error loading current user", e)
            }
        }
    }
    
    private fun loadAppointments() {
        viewModelScope.launch {
            _isLoading.value = true
            
            userRepository.getCurrentUser()?.let { user ->
                Log.d("AppointmentsViewModel", "Loading appointments for user: ${user.id}")
                
                userRepository.getUserAppointments(user.id)
                    .catch { e -> 
                        Log.e("AppointmentsViewModel", "Error in appointments flow", e)
                        _isLoading.value = false
                        _appointments.value = emptyList()
                    }
                    .collect { result ->
                        result.onSuccess { appointmentsList ->
                            Log.d("AppointmentsViewModel", "Received ${appointmentsList.size} appointments")
                            allAppointments = appointmentsList.sortedByDescending { it.date }
                            _appointments.value = allAppointments
                        }.onFailure { error ->
                            Log.e("AppointmentsViewModel", "Failed to load appointments", error)
                            _appointments.value = emptyList()
                        }
                        _isLoading.value = false
                    }
            } ?: run {
                Log.d("AppointmentsViewModel", "No current user found")
                _isLoading.value = false
                _appointments.value = emptyList()
            }
        }
    }
    
    fun filterAppointments(status: String?) {
        Log.d("AppointmentsViewModel", "Filtering appointments by status: $status")
        _appointments.value = if (status == null) {
            allAppointments
        } else {
            allAppointments.filter { it.status == status }
        }
    }

    // Ánh xạ từ slotId sang thời gian kết thúc theo database
    private fun getSlotEndTime(slotId: Int): Pair<Int, Int> {
        return when (slotId) {
            // Morning slots
            0 -> Pair(9, 0)   // 08:00 - 09:00
            1 -> Pair(10, 0)  // 09:00 - 10:00
            2 -> Pair(11, 0)  // 10:00 - 11:00
            3 -> Pair(12, 0)  // 11:00 - 12:00
            
            // Afternoon slots
            4 -> Pair(14, 30) // 13:30 - 14:30
            5 -> Pair(15, 30) // 14:30 - 15:30
            6 -> Pair(16, 30) // 15:30 - 16:30
            7 -> Pair(17, 30) // 16:30 - 17:30
            
            // Evening slots
            8 -> Pair(19, 30) // 18:30 - 19:30
            9 -> Pair(20, 30) // 19:30 - 20:30
            10 -> Pair(21, 30) // 20:30 - 21:30
            11 -> Pair(22, 30) // 21:30 - 22:30
            
            else -> Pair(23, 59) // Mặc định nếu không tìm thấy
        }
    }
}