package com.healthtech.doccareplus.utils

object DateTimeUtils {

    // Format: 2023-11-22 -> 22/11/2023
    fun formatServerDate(serverDate: String): String {
        try {
            val parts = serverDate.split("-")
            if (parts.size == 3) {
                val (year, month, day) = parts
                return "$day/$month/$year"
            }
        } catch (e: Exception) {
            // Handle exception
        }
        return serverDate
    }

    // Chuyển đổi slotId thành khoảng thời gian
    fun getTimeRangeForSlot(slotId: Int): String {
        return when (slotId) {
            // Morning slots
            0 -> "08:00 - 09:00"
            1 -> "09:00 - 10:00"
            2 -> "10:00 - 11:00"
            3 -> "11:00 - 12:00"

            // Afternoon slots
            4 -> "13:30 - 14:30"
            5 -> "14:30 - 15:30"
            6 -> "15:30 - 16:30"
            7 -> "16:30 - 17:30"

            // Evening slots
            8 -> "18:30 - 19:30"
            9 -> "19:30 - 20:30"
            10 -> "20:30 - 21:30"
            11 -> "21:30 - 22:30"

            else -> "Không xác định"
        }
    }
}