package com.healthtech.doccareplus.domain.service

import com.healthtech.doccareplus.data.remote.api.PaymentApiClient
import kotlinx.coroutines.flow.Flow

interface PaymentService {
    fun initiatePayment(
        amount: Double,
        currency: String = "usd"
    ): Flow<Result<PaymentApiClient.PaymentSheetParams>>
}