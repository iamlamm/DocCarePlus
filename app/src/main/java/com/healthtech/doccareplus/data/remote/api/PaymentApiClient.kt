package com.healthtech.doccareplus.data.remote.api

import android.content.Context
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.healthtech.doccareplus.utils.Constants
import com.stripe.android.paymentsheet.PaymentSheet
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentApiClient @Inject constructor(
    private val context: Context
) {
    /**
     * Khởi tạo thanh toán với số tiền tùy chỉnh
     */
    fun createPayment(
        amount: Double,
        currency: String = "usd",
        callback: PaymentSheetParamsCallback
    ) {
        val queue = Volley.newRequestQueue(context)
        val url = "${Constants.BACKEND_URL}/create-custom-payment"

        // Chuyển đổi amount từ Double sang Long cents (nhân với 100)
        val amountInCents = (amount * 100).toLong()

        val requestBody = JSONObject().apply {
            put("amount", amountInCents)
            put("currency", currency)
        }

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, requestBody,
            { response ->
                try {
                    val paymentIntentClientSecret = response.getString("paymentIntent")
                    val customerId = response.getString("customer")
                    val ephemeralKey = response.getString("ephemeralKey")

                    val customerConfig = PaymentSheet.CustomerConfiguration(
                        id = customerId,
                        ephemeralKeySecret = ephemeralKey
                    )

                    callback.onSuccess(
                        PaymentSheetParams(
                            paymentIntentClientSecret = paymentIntentClientSecret,
                            customerConfig = customerConfig
                        )
                    )
                } catch (e: Exception) {
                    callback.onError("Lỗi xử lý dữ liệu: ${e.localizedMessage}")
                }
            },
            { error ->
                callback.onError("Lỗi mạng: ${error.localizedMessage ?: "Không xác định"}")
            }
        )

        queue.add(jsonObjectRequest)
    }

    /**
     * Lớp chứa các tham số cần thiết cho PaymentSheet
     */
    data class PaymentSheetParams(
        val paymentIntentClientSecret: String,
        val customerConfig: PaymentSheet.CustomerConfiguration
    )

    /**
     * Interface callback cho kết quả API
     */
    interface PaymentSheetParamsCallback {
        fun onSuccess(params: PaymentSheetParams)
        fun onError(error: String)
    }
}