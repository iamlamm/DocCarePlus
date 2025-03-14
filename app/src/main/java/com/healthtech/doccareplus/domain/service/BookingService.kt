package com.healthtech.doccareplus.domain.service

import com.healthtech.doccareplus.domain.model.BookingRequest
import com.healthtech.doccareplus.domain.model.SlotAvailabilityResult
import kotlinx.coroutines.flow.Flow

interface BookingService {
    /**
     * String là appointmentId
     */
    suspend fun bookAppointment(request: BookingRequest): Flow<Result<String>>
    
    /**
     * Kiểm tra slot có khả dụng không trước khi bắt đầu quá trình thanh toán
     */
    suspend fun checkSlotAvailability(doctorId: String, date: String, slotId: Int, userId: String): Flow<Result<SlotAvailabilityResult>>

    suspend fun updateAppointmentStatus(appointmentId: String, newStatus: String): Flow<Result<Unit>>
}