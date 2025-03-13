package com.healthtech.doccareplus.data.service

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.healthtech.doccareplus.domain.model.BookingRequest
import com.healthtech.doccareplus.domain.model.Notification
import com.healthtech.doccareplus.domain.model.NotificationType
import com.healthtech.doccareplus.domain.model.SlotAvailabilityResult
import com.healthtech.doccareplus.domain.service.BookingService
import com.healthtech.doccareplus.domain.service.NotificationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingServiceImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val notificationService: NotificationService
) : BookingService {

    override suspend fun bookAppointment(request: BookingRequest): Flow<Result<String>> = flow {
        try {
            // 1. Kiểm tra lịch làm việc của bác sĩ
            val isWorkingDay = checkDoctorWorkingDay(request.doctorId, request.date)
            if (!isWorkingDay) {
                emit(Result.failure(Exception("Bác sĩ không làm việc vào ngày này")))
                return@flow
            }

            // 2. Kiểm tra slot có khả dụng không
            when (val availability = checkSlotAvailabilityInternal(
                request.doctorId,
                request.date,
                request.slotId,
                request.userId
            )) {
                is SlotAvailabilityResult.Available -> {
                    // Tiếp tục quá trình đặt lịch
                    val appointmentId = createAppointment(request)
                    updateBookedSlots(request.doctorId, request.date, request.slotId)
                    
                    // Lấy thông tin tên bác sĩ
                    val doctorRef = database.getReference("doctors/${request.doctorId}")
                    val doctorSnapshot = doctorRef.get().await()
                    val doctorName = doctorSnapshot.child("name").getValue(String::class.java) ?: "Bác sĩ"
                    
                    // Tạo thông báo cho user
                    val userNotification = Notification(
                        title = "Đặt lịch thành công",
                        message = "Bạn đã đặt lịch khám thành công với $doctorName. Mã cuộc hẹn: $appointmentId",
                        time = System.currentTimeMillis(),
                        type = NotificationType.APPOINTMENT_BOOKED,
                        date = request.date,
                        appointmentId = appointmentId
                    )
                    notificationService.createUserNotification(userNotification, request.userId)
                    
                    // Tạo thông báo cho doctor
                    val doctorNotification = Notification(
                        title = "Lịch hẹn mới",
                        message = "Bạn có lịch hẹn mới với bệnh nhân ${request.userName}. Mã cuộc hẹn: $appointmentId",
                        time = System.currentTimeMillis(),
                        type = NotificationType.NEW_APPOINTMENT,
                        date = request.date,
                        appointmentId = appointmentId
                    )
                    notificationService.createDoctorNotification(doctorNotification, request.doctorId)
                    
                    emit(Result.success(appointmentId))
                }

                is SlotAvailabilityResult.AlreadyBookedByCurrentUser -> {
                    emit(Result.failure(Exception("Bạn đã đặt lịch khám vào khung giờ này")))
                }

                is SlotAvailabilityResult.AlreadyBookedByOther -> {
                    emit(Result.failure(Exception("Khung giờ này đã có người đặt lịch")))
                }

                is SlotAvailabilityResult.Unavailable -> {
                    emit(Result.failure(Exception("Bác sĩ không thể khám vào khung giờ này")))
                }
            }
        } catch (e: Exception) {
            Log.e("BookingService", "Error during booking: ${e.message}")
            emit(Result.failure(e))
        }
    }

    override suspend fun checkSlotAvailability(
        doctorId: String, 
        date: String, 
        slotId: Int, 
        userId: String
    ): Flow<Result<SlotAvailabilityResult>> = flow {
        try {
            // 1. Kiểm tra lịch làm việc của bác sĩ
            val isWorkingDay = checkDoctorWorkingDay(doctorId, date)
            if (!isWorkingDay) {
                emit(Result.failure(Exception("Bác sĩ không làm việc vào ngày này")))
                return@flow
            }

            // 2. Kiểm tra slot có khả dụng không
            val availability = checkSlotAvailabilityInternal(doctorId, date, slotId, userId)
            when (availability) {
                is SlotAvailabilityResult.Available -> {
                    emit(Result.success(availability))
                }
                is SlotAvailabilityResult.AlreadyBookedByCurrentUser -> {
                    emit(Result.failure(Exception("Bạn đã đặt lịch khám vào khung giờ này")))
                }
                is SlotAvailabilityResult.AlreadyBookedByOther -> {
                    emit(Result.failure(Exception("Khung giờ này đã có người đặt lịch")))
                }
                is SlotAvailabilityResult.Unavailable -> {
                    emit(Result.failure(Exception("Bác sĩ không thể khám vào khung giờ này")))
                }
            }
        } catch (e: Exception) {
            Log.e("BookingService", "Error checking slot availability: ${e.message}")
            emit(Result.failure(e))
        }
    }

    private suspend fun checkDoctorWorkingDay(doctorId: String, date: String): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)!!
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK).toLowerCaseDayName()
        val scheduleRef = database.getReference("doctorSchedules").child(doctorId)
        val snapshot = scheduleRef.get().await()
        return snapshot.child("workingDays")
            .children.any { it.value.toString() == dayOfWeek }
    }

    private suspend fun checkSlotAvailabilityInternal(
        doctorId: String,
        date: String,
        slotId: Int,
        currentUserId: String
    ): SlotAvailabilityResult {
        val scheduleRef = database.getReference("schedules/byDoctor/$doctorId/$date")

        val schedule = scheduleRef.get().await()

        if (!schedule.exists()) return SlotAvailabilityResult.Available

        // Kiểm tra unavailableSlots
        val unavailableSlots = schedule.child("unavailableSlots")
            .children.mapNotNull { it.value.toString().toIntOrNull() }

        if (slotId in unavailableSlots) {
            return SlotAvailabilityResult.Unavailable
        }

        // Kiểm tra cuộc hẹn theo cấu trúc mới
        val appointmentsRef = database.getReference("appointments/byDoctor/$doctorId")
            .orderByChild("date")
            .equalTo(date)

        val appointmentsSnapshot = appointmentsRef.get().await()
        val existingAppointment = appointmentsSnapshot.children.firstOrNull {
            it.child("slotId").value.toString().toInt() == slotId
        }

        return when {
            existingAppointment == null -> SlotAvailabilityResult.Available
            existingAppointment.child("userId").value.toString() == currentUserId ->
                SlotAvailabilityResult.AlreadyBookedByCurrentUser

            else -> SlotAvailabilityResult.AlreadyBookedByOther
        }
    }

    private suspend fun createAppointment(request: BookingRequest): String {
        val appointmentKey = database.getReference("appointments/details").push().key!!

        val appointment = hashMapOf(
            "id" to appointmentKey,
            "doctorId" to request.doctorId,
            "userId" to request.userId,
            "date" to request.date,
            "slotId" to request.slotId,
            "status" to "upcoming",
            "createdAt" to ServerValue.TIMESTAMP
        )

        // Lưu vào 3 nơi khác nhau theo cấu trúc mới
        val updates = hashMapOf<String, Any>(
            // Chi tiết cuộc hẹn
            "appointments/details/$appointmentKey" to appointment,

            // Theo bác sĩ
            "appointments/byDoctor/${request.doctorId}/$appointmentKey" to appointment,

            // Theo người dùng
            "appointments/byUser/${request.userId}/$appointmentKey" to appointment,

            // Theo ngày
            "appointments/byDate/${request.date}/$appointmentKey" to true
        )

        database.reference.updateChildren(updates).await()
        return appointmentKey
    }

    private suspend fun updateBookedSlots(doctorId: String, date: String, slotId: Int) {
        val scheduleRef = database.getReference("schedules/byDoctor/$doctorId/$date")

        val schedule = scheduleRef.get().await()

        if (schedule.exists()) {
            // Thêm slot vào bookedSlots theo cấu trúc mới
            scheduleRef.child("bookedSlots").push().setValue(slotId).await()
        } else {
            // Tạo schedule mới nếu chưa có
            val newSchedule = hashMapOf(
                "doctorId" to doctorId,
                "date" to date,
                "unavailableSlots" to listOf<Int>(),
                "bookedSlots" to hashMapOf<String, Int>()
            )

            // Đặt schedule theo cấu trúc mới
            database.getReference("schedules/byDoctor/$doctorId/$date")
                .setValue(newSchedule)
                .await()

            // Thêm vào chỉ mục byDate
            database.getReference("schedules/byDate/$date/$doctorId")
                .setValue(true)
                .await()

            // Sau đó cập nhật bookedSlots
            database.getReference("schedules/byDoctor/$doctorId/$date/bookedSlots")
                .push().setValue(slotId).await()
        }
    }

    private fun Int.toLowerCaseDayName(): String {
        return when (this) {
            Calendar.MONDAY -> "monday"
            Calendar.TUESDAY -> "tuesday"
            Calendar.WEDNESDAY -> "wednesday"
            Calendar.THURSDAY -> "thursday"
            Calendar.FRIDAY -> "friday"
            Calendar.SATURDAY -> "saturday"
            Calendar.SUNDAY -> "sunday"
            else -> throw IllegalArgumentException("Invalid day of week")
        }
    }
}

