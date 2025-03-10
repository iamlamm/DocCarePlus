package com.healthtech.doccareplus.domain.service

import com.healthtech.doccareplus.domain.model.BookingRequest
import kotlinx.coroutines.flow.Flow

interface BookingService {
    /**
     * String là appointmentId
     */
    suspend fun bookAppointment(request: BookingRequest): Flow<Result<String>>
}