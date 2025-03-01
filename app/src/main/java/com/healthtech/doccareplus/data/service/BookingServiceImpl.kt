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
            when (val availability = checkSlotAvailability(
                request.doctorId,
                request.date,
                request.slotId,
                request.userId
            )) {
                is SlotAvailabilityResult.Available -> {
                    // Tiếp tục quá trình đặt lịch
                    val appointmentId = createAppointment(request)
                    updateBookedSlots(request.doctorId, request.date, request.slotId)
                    emit(Result.success(appointmentId))

                    val notification = Notification(
                        title = "Đặt lịch thành công",
                        message = "Bạn đã đặt lịch khám thành công. Mã cuộc hẹn: $appointmentId",
                        time = System.currentTimeMillis(),
                        type = NotificationType.APPOINTMENT,
                        userId = request.userId
                    )
                    notificationService.createNotification(notification)
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

    private suspend fun checkDoctorWorkingDay(doctorId: Int, date: String): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)!!
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK).toLowerCaseDayName()

        val scheduleRef = database.getReference("doctorSchedules")
            .orderByChild("doctorId")
            .equalTo(doctorId.toDouble())

        val snapshot = scheduleRef.get().await()
        return snapshot.children.firstOrNull()?.child("workingDays")
            ?.children?.any { it.value.toString() == dayOfWeek } ?: false
    }

    private suspend fun checkSlotAvailability(
        doctorId: Int,
        date: String,
        slotId: Int,
        currentUserId: String
    ): SlotAvailabilityResult {
        val scheduleRef = database.getReference("schedules")
            .orderByChild("doctorId")
            .equalTo(doctorId.toDouble())

        val snapshot = scheduleRef.get().await()
        val schedule = snapshot.children.firstOrNull {
            it.child("date").value.toString() == date
        }

        if (schedule == null) return SlotAvailabilityResult.Available

        val unavailableSlots = schedule.child("unavailableSlots")
            .children.mapNotNull { it.value.toString().toIntOrNull() }

        if (slotId in unavailableSlots) {
            return SlotAvailabilityResult.Unavailable
        }

        // Kiểm tra trong appointments để biết ai đã đặt slot này
        val appointmentsRef = database.getReference("appointments")
            .orderByChild("doctorId")
            .equalTo(doctorId.toDouble())

        val appointmentsSnapshot = appointmentsRef.get().await()
        val existingAppointment = appointmentsSnapshot.children.firstOrNull {
            it.child("date").value.toString() == date &&
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
        val appointmentsRef = database.getReference("appointments")

        // Lấy key từ Firebase
        val newKey = appointmentsRef.push().key!!

        val appointment = hashMapOf(
            "id" to newKey,
            "doctorId" to request.doctorId,
            "userId" to request.userId,
            "date" to request.date,
            "slotId" to request.slotId,
            "status" to "upcoming",
            "createdAt" to ServerValue.TIMESTAMP
        )

        appointmentsRef.child(newKey).setValue(appointment).await()
        return newKey
    }

    private suspend fun updateBookedSlots(doctorId: Int, date: String, slotId: Int) {
        val scheduleRef = database.getReference("schedules")
            .orderByChild("doctorId")
            .equalTo(doctorId.toDouble())

        val snapshot = scheduleRef.get().await()
        val schedule = snapshot.children.firstOrNull {
            it.child("date").value.toString() == date
        }

        if (schedule != null) {
            // Thêm slot vào bookedSlots
            schedule.ref.child("bookedSlots").push().setValue(slotId).await()
        } else {
            // Tạo schedule mới nếu chưa có
            val newSchedule = hashMapOf(
                "doctorId" to doctorId,
                "date" to date,
                "unavailableSlots" to listOf<Int>(),
                "bookedSlots" to listOf(slotId)
            )
            // Sửa lại cách push và set giá trị
            database.getReference("schedules")
                .push()
                .setValue(newSchedule)
                .await()
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

