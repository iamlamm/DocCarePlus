package com.healthtech.doccareplus.ui.doctor.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.domain.model.TimePeriod
import com.healthtech.doccareplus.domain.model.TimeSlot
import com.healthtech.doccareplus.domain.repository.TimeSlotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DoctorProfileViewModel @Inject constructor(
    private val timeSlotRepository: TimeSlotRepository
) : ViewModel() {
    private val _state = MutableStateFlow<DoctorProfileState>(DoctorProfileState.Idle)
    val state: StateFlow<DoctorProfileState> = _state.asStateFlow()

    private val _months = MutableStateFlow<List<String>>(emptyList())
    val months: StateFlow<List<String>> = _months.asStateFlow()

    private val _timeSlots = MutableStateFlow<List<TimeSlot>>(emptyList())
    val timeSlots: StateFlow<List<TimeSlot>> = _timeSlots.asStateFlow()

    private val _selectedTimeSlot = MutableStateFlow<TimeSlot?>(null)
    val selectedTimeSlot: StateFlow<TimeSlot?> = _selectedTimeSlot.asStateFlow()

    init {
        loadInitialData()
        observeTimeSlots()
    }

    private fun observeTimeSlots() {
        viewModelScope.launch {
            timeSlotRepository.observeTimeSlots().collect { result ->
                if (result.isSuccess) {
                    val slots = result.getOrNull() ?: emptyList()
                    _timeSlots.value = slots
                    Log.d("TimeSlots", "Received ${slots.size} time slots")
                } else {
                    Log.e("TimeSlots", "Error loading time slots: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }


    private fun loadInitialData() {
        viewModelScope.launch {
            _months.value = getNext6Months()
            updateCalendarForMonth(_months.value.firstOrNull() ?: return@launch)
        }
    }

    fun updateCalendarForMonth(monthYear: String) {
        viewModelScope.launch {
            try {
                _state.value = DoctorProfileState.Loading
                val dates = generateDatesForMonth(monthYear)
                _state.value = DoctorProfileState.Success(datesInMonth = dates)
            } catch (e: Exception) {
                _state.value = DoctorProfileState.Error("Failed to load calendar: ${e.message}")
            }
        }
    }

    fun onDateSelected(date: Date) {
        val currentState = _state.value
        if (currentState is DoctorProfileState.Success) {
            _state.value = currentState.copy(selectedDate = date)
        }
    }

    private fun getNext6Months(): List<String> {
        val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        return (0..5).map { month ->
            formatter.format(calendar.time).also {
                calendar.add(Calendar.MONTH, 1)
            }
        }
    }

    private fun generateDatesForMonth(monthYear: String): List<Date> {
        val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        calendar.apply {
            time = formatter.parse(monthYear) ?: Date()
        }

        val selectedMonth = calendar.get(Calendar.MONTH)
        val selectedYear = calendar.get(Calendar.YEAR)

        val datesInMonth = mutableListOf<Date>()

        if (selectedMonth == currentMonth && selectedYear == currentYear) {
            calendar.set(Calendar.DAY_OF_MONTH, currentDay)
        } else {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
        }

        val month = calendar.get(Calendar.MONTH)
        while (calendar.get(Calendar.MONTH) == month) {
            datesInMonth.add(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return datesInMonth
    }


    fun setSelectedTimeSlot(timeSlot: TimeSlot) {
        _selectedTimeSlot.value = timeSlot
    }

    fun resetSelectedTimeSlot() {
        _selectedTimeSlot.value = null
    }
}