package com.healthtech.doccareplus.data.service

import com.healthtech.doccareplus.data.remote.api.PaymentApiClient
import com.healthtech.doccareplus.domain.service.PaymentService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentServiceImpl @Inject constructor(
    private val paymentApiClient: PaymentApiClient
) : PaymentService {

    override fun initiatePayment(
        amount: Double,
        currency: String
    ): Flow<Result<PaymentApiClient.PaymentSheetParams>> = callbackFlow {
        paymentApiClient.createPayment(
            amount = amount,
            currency = currency,
            callback = object : PaymentApiClient.PaymentSheetParamsCallback {
                override fun onSuccess(params: PaymentApiClient.PaymentSheetParams) {
                    trySend(Result.success(params))
                }

                override fun onError(error: String) {
                    trySend(Result.failure(Exception(error)))
                }
            }
        )

        awaitClose()
    }
}