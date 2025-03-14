package com.healthtech.doccareplus.ui.doctor.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.domain.model.BookingRequest
import com.healthtech.doccareplus.domain.model.TimeSlot
import com.healthtech.doccareplus.domain.repository.TimeSlotRepository
import com.healthtech.doccareplus.domain.repository.UserRepository
import com.healthtech.doccareplus.domain.service.BookingService
import com.healthtech.doccareplus.domain.service.PaymentService
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
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult

@HiltViewModel
class DoctorProfileViewModel @Inject constructor(
    private val timeSlotRepository: TimeSlotRepository,
    private val bookingService: BookingService,
    private val userRepository: UserRepository,
    private val paymentService: PaymentService
) : ViewModel() {
    private val _state = MutableStateFlow<DoctorProfileState>(DoctorProfileState.Idle)
    val state: StateFlow<DoctorProfileState> = _state.asStateFlow()

    private val _months = MutableStateFlow<List<String>>(emptyList())
    val months: StateFlow<List<String>> = _months.asStateFlow()

    private val _timeSlots = MutableStateFlow<List<TimeSlot>>(emptyList())
    val timeSlots: StateFlow<List<TimeSlot>> = _timeSlots.asStateFlow()

    private val _selectedTimeSlot = MutableStateFlow<TimeSlot?>(null)
    val selectedTimeSlot: StateFlow<TimeSlot?> = _selectedTimeSlot.asStateFlow()

    private val _selectedDate = MutableStateFlow<Date?>(null)
    val selectedDate: StateFlow<Date?> = _selectedDate.asStateFlow()

    private val _doctorId = MutableStateFlow<String?>(null)
    val doctorId: StateFlow<String?> = _doctorId.asStateFlow()

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
                    Log.e(
                        "TimeSlots",
                        "Error loading time slots: ${result.exceptionOrNull()?.message}"
                    )
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
        _selectedDate.value = date
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

    fun bookAppointment(doctorId: String) {
        _doctorId.value = doctorId

        viewModelScope.launch {
            val selectedDate = _selectedDate.value
            val selectedSlot = _selectedTimeSlot.value

            if (selectedDate == null) {
                _state.value = DoctorProfileState.Error("Vui lòng chọn ngày")
                return@launch
            }

            if (selectedSlot == null) {
                _state.value = DoctorProfileState.Error("Vui lòng chọn giờ khám")
                return@launch
            }

            try {
                _state.value = DoctorProfileState.BookingLoading

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate)

                val userId = userRepository.getCurrentUserId()
                val request = BookingRequest(
                    doctorId = doctorId,
                    userId = userId!!,
                    date = formattedDate,
                    slotId = selectedSlot.id
                )

                bookingService.bookAppointment(request)
                    .collect { result ->
                        result.fold(
                            onSuccess = { appointmentId ->
                                _state.value = DoctorProfileState.BookingSuccess(appointmentId)
                            },
                            onFailure = { e ->
                                _state.value =
                                    DoctorProfileState.Error(e.message ?: "Đặt lịch thất bại")
                            }
                        )
                    }
            } catch (e: Exception) {
                _state.value = DoctorProfileState.Error(e.message ?: "Đã xảy ra lỗi")
            }
        }
    }

    fun initiateChatWithDoctor(doctorId: Int, doctorName: String) {
        val doctorIdString = doctorId.toString()

        Log.d("DoctorProfileViewModel", "Starting chat with doctor: $doctorIdString, $doctorName")

        _state.value = DoctorProfileState.InitiateChat(doctorIdString, doctorName)
    }

    fun setupBooking(doctorId: String, doctorFee: Double) {
        viewModelScope.launch {
            val selectedDate = _selectedDate.value
            val selectedSlot = _selectedTimeSlot.value

            if (selectedDate == null) {
                _state.value = DoctorProfileState.Error("Vui lòng chọn ngày")
                return@launch
            }

            if (selectedSlot == null) {
                _state.value = DoctorProfileState.Error("Vui lòng chọn giờ khám")
                return@launch
            }

            try {
                _state.value = DoctorProfileState.BookingLoading

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate)

                val userId = userRepository.getCurrentUserId()
                if (userId == null) {
                    _state.value = DoctorProfileState.Error("Không tìm thấy thông tin người dùng")
                    return@launch
                }

                bookingService.checkSlotAvailability(
                    doctorId,
                    formattedDate,
                    selectedSlot.id,
                    userId
                )
                    .collect { result ->
                        result.fold(
                            onSuccess = {
                                initiatePayment(doctorFee)
                            },
                            onFailure = { e ->
                                _state.value =
                                    DoctorProfileState.Error(e.message ?: "Không thể đặt lịch")
                            }
                        )
                    }
            } catch (e: Exception) {
                _state.value = DoctorProfileState.Error(e.message ?: "Đã xảy ra lỗi")
            }
        }
    }

    private fun initiatePayment(doctorFee: Double) {
        viewModelScope.launch {
            _state.value = DoctorProfileState.PaymentLoading

            paymentService.initiatePayment(amount = doctorFee)
                .collect { result ->
                    result.fold(
                        onSuccess = { params ->
                            _state.value = DoctorProfileState.PaymentReady(
                                paymentIntentClientSecret = params.paymentIntentClientSecret,
                                customerConfig = params.customerConfig
                            )
                        },
                        onFailure = { e ->
                            _state.value =
                                DoctorProfileState.PaymentFailed(e.message ?: "Lỗi thanh toán")
                        }
                    )
                }
        }
    }

    fun handlePaymentResult(paymentResult: PaymentSheetResult) {
        when (paymentResult) {
            is PaymentSheetResult.Completed -> {
                bookAppointmentAfterPayment()
            }

            is PaymentSheetResult.Canceled -> {
                _state.value = DoctorProfileState.PaymentCancelled
            }

            is PaymentSheetResult.Failed -> {
                _state.value = DoctorProfileState.PaymentFailed(
                    paymentResult.error.localizedMessage ?: "Thanh toán thất bại"
                )
            }
        }
    }

    private fun bookAppointmentAfterPayment() {
        viewModelScope.launch {
            val selectedDate = _selectedDate.value
            val selectedSlot = _selectedTimeSlot.value

            if (selectedDate == null) {
                _state.value = DoctorProfileState.Error("Vui lòng chọn ngày")
                return@launch
            }

            if (selectedSlot == null) {
                _state.value = DoctorProfileState.Error("Vui lòng chọn giờ khám")
                return@launch
            }

            try {
                _state.value = DoctorProfileState.BookingLoading

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDate = dateFormat.format(selectedDate)

                val userId = userRepository.getCurrentUserId()

                val doctorId = _doctorId.value
                if (doctorId == null) {
                    _state.value = DoctorProfileState.Error("Không tìm thấy thông tin bác sĩ")
                    return@launch
                }

                val request = BookingRequest(
                    doctorId = doctorId,
                    userId = userId!!,
                    date = formattedDate,
                    slotId = selectedSlot.id
                )

                bookingService.bookAppointment(request)
                    .collect { result ->
                        result.fold(
                            onSuccess = { appointmentId ->
                                _state.value = DoctorProfileState.PaymentComplete(appointmentId)
                            },
                            onFailure = { e ->
                                _state.value =
                                    DoctorProfileState.Error(e.message ?: "Đặt lịch thất bại")
                            }
                        )
                    }
            } catch (e: Exception) {
                _state.value = DoctorProfileState.Error(e.message ?: "Đã xảy ra lỗi")
            }
        }
    }

    fun setDoctorId(id: String) {
        _doctorId.value = id
    }
}