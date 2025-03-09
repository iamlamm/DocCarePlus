package com.healthtech.doccareplus.ui.doctor.profile

import com.healthtech.doccareplus.domain.model.TimeSlot
import java.util.Date

sealed class DoctorProfileState {
    data object Idle : DoctorProfileState()
    data object Loading : DoctorProfileState()
    data object BookingLoading : DoctorProfileState()
    data class BookingSuccess(val appointmentId: String) : DoctorProfileState()

    data class Success(
        val datesInMonth: List<Date>,     // Danh sách các ngày trong tháng được chọn
        val selectedDate: Date? = null     // Ngày được user chọn
    ) : DoctorProfileState()

    data class Error(val message: String) : DoctorProfileState()

    data class InitiateChat(val doctorId: String, val doctorName: String) : DoctorProfileState()
}