package com.healthtech.doccareplus.domain.model


enum class NotificationType {
    APPOINTMENT_BOOKED,     // Thông báo cho user khi đặt lịch thành công
    NEW_APPOINTMENT,        // Thông báo cho bác sĩ khi có lịch hẹn mới
    APPOINTMENT_CANCELLED,  // Khi hủy lịch hẹn
    APPOINTMENT_REMINDER,   // Nhắc lịch hẹn
    SYSTEM                  // Thông báo hệ thống
}