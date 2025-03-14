package com.healthtech.doccareplus.data.remote.api

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.healthtech.doccareplus.domain.model.Appointment
import com.healthtech.doccareplus.domain.model.Category
import com.healthtech.doccareplus.domain.model.Doctor
import com.healthtech.doccareplus.domain.model.User
import com.healthtech.doccareplus.domain.model.TimeSlot
import com.healthtech.doccareplus.domain.model.TimePeriod
import com.healthtech.doccareplus.domain.model.UserRole
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * callbackFlow {}: Mở một luồng để lắng nghe sự kiện.
 * trySend(value): Gửi giá trị vào Flow.
 * awaitClose {}: Đóng Flow khi không còn lắng nghe nữa.
 */

@Singleton
class FirebaseApi @Inject constructor(
    private val database: FirebaseDatabase
) {
    fun getCategories(): Flow<List<Category>> = callbackFlow {
        val categoriesRef = database.getReference("categories")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Khi có dữ liệu mới, convert từ JSON sang List<Category>
                val categoryList = mutableListOf<Category>()
                // Duyệt qua từng child node trong categories
                for (categorySnapshot in snapshot.children) {
                    // Convert từng node thành đối tượng Category
                    categorySnapshot.getValue(Category::class.java)?.let { category ->
                        categoryList.add(category)
                    }
                }
                // Gửi danh sách category mới vào Flow
                trySend(categoryList)
            }

            override fun onCancelled(error: DatabaseError) {
                // Xử lý khi có lỗi xảy ra
                // Có thể log lỗi hoặc thông báo cho user
                Log.e("ERROR LOADING CATEGORY", error.message)
            }
        }
        // Đăng ký listener với Firebase
        categoriesRef.addValueEventListener(listener)

        // Đóng luồng khi xong
        // Khi Flow bị hủy, remove listener để tránh memory leak
        awaitClose {
            categoriesRef.removeEventListener(listener)
            println("Flow getCategories đóng")
        }
    }

    fun getDoctors(): Flow<List<Doctor>> = callbackFlow {
        val doctorsRef = database.getReference("doctors")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val doctorList = snapshot.children.mapNotNull { doctorSnapshot ->
                        val doctorId = doctorSnapshot.key ?: return@mapNotNull null
                        try {
                            Doctor(
                                id = doctorId,
                                code = doctorSnapshot.child("code").getValue(String::class.java) ?: "",
                                name = doctorSnapshot.child("name").getValue(String::class.java) ?: "",
                                specialty = doctorSnapshot.child("specialty").getValue(String::class.java) ?: "",
                                categoryId = doctorSnapshot.child("categoryId").getValue(Long::class.java)?.toInt() ?: 0,
                                rating = doctorSnapshot.child("rating").getValue(Double::class.java)?.toFloat() ?: 0F,
                                reviews = doctorSnapshot.child("reviews").getValue(Long::class.java) ?: 0L,
                                fee = doctorSnapshot.child("fee").getValue(Double::class.java) ?: 0.0,
                                avatar = doctorSnapshot.child("avatar").getValue(String::class.java) ?: "",
                                available = doctorSnapshot.child("available").getValue(Boolean::class.java) ?: true,
                                biography = doctorSnapshot.child("biography").getValue(String::class.java) ?: "",
                                role = try {
                                    val roleString = doctorSnapshot.child("role").getValue(String::class.java) ?: "DOCTOR"
                                    UserRole.valueOf(roleString)
                                } catch (e: Exception) {
                                    UserRole.DOCTOR
                                },
                                email = doctorSnapshot.child("email").getValue(String::class.java) ?: "",
                                phoneNumber = doctorSnapshot.child("phoneNumber").getValue(String::class.java) ?: "",
                                emergencyContact = doctorSnapshot.child("emergencyContact").getValue(String::class.java) ?: "",
                                address = doctorSnapshot.child("address").getValue(String::class.java) ?: ""
                            )
                        } catch (e: Exception) {
                            Log.e("FirebaseApi", "Error parsing doctor $doctorId: ${e.message}")
                            null
                        }
                    }

                    trySend(doctorList)
                } catch (e: Exception) {
                    Log.e("FirebaseApi", "Error processing doctors data: ${e.message}")
                    trySend(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ERROR LOADING DOCTOR", error.message)
                close(error.toException())
            }
        }

        doctorsRef.addValueEventListener(listener)
        awaitClose { doctorsRef.removeEventListener(listener) }
    }

    // Thêm vào FirebaseApi.kt
//    fun getDoctorSchedule(doctorId: String, date: String): Flow<Result<DoctorSchedule>> = callbackFlow {
//        val scheduleRef = database.getReference("schedules/byDoctor/$doctorId/$date")
//        val listener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                if (!snapshot.exists()) {
//                    trySend(Result.failure(Exception("No schedule found")))
//                    return
//                }
//
//                try {
//                    // Parse bookedSlots theo cấu trúc mới (object)
//                    val bookedSlots = mutableListOf<Int>()
//                    snapshot.child("bookedSlots").children.forEach { slotSnapshot ->
//                        val slotId = slotSnapshot.value.toString().toIntOrNull()
//                        if (slotId != null) bookedSlots.add(slotId)
//                    }
//
//                    // Parse unavailableSlots
//                    val unavailableSlots = mutableListOf<Int>()
//                    snapshot.child("unavailableSlots").children.forEach { slotSnapshot ->
//                        val slotId = slotSnapshot.value.toString().toIntOrNull()
//                        if (slotId != null) unavailableSlots.add(slotId)
//                    }
//
//                    val schedule = DoctorSchedule(
//                        doctorId = doctorId,
//                        date = date,
//                        bookedSlots = bookedSlots,
//                        unavailableSlots = unavailableSlots
//                    )
//
//                    trySend(Result.success(schedule))
//                } catch (e: Exception) {
//                    trySend(Result.failure(e))
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                trySend(Result.failure(error.toException()))
//            }
//        }
//
//        scheduleRef.addValueEventListener(listener)
//        awaitClose { scheduleRef.removeEventListener(listener) }
//    }
//
//    // Thêm vào FirebaseApi.kt
//    fun getUserAppointments(userId: String): Flow<List<Appointment>> = callbackFlow {
//        val appointmentsRef = database.getReference("appointments/byUser/$userId")
//        val listener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val appointments = snapshot.children.mapNotNull { appointmentSnapshot ->
//                    try {
//                        // Parse từng field của appointment
//                        val appointment = Appointment(
//                            id = appointmentSnapshot.child("id").value.toString(),
//                            doctorId = appointmentSnapshot.child("doctorId").value.toString(),
//                            userId = appointmentSnapshot.child("userId").value.toString(),
//                            date = appointmentSnapshot.child("date").value.toString(),
//                            slotId = appointmentSnapshot.child("slotId").value.toString().toIntOrNull() ?: 0,
//                            status = appointmentSnapshot.child("status").value.toString(),
//                            symptoms = appointmentSnapshot.child("symptoms").value?.toString(),
//                            notes = appointmentSnapshot.child("notes").value?.toString()
//                        )
//                        appointment
//                    } catch (e: Exception) {
//                        Log.e("FirebaseApi", "Error parsing appointment: ${e.message}")
//                        null
//                    }
//                }
//                trySend(appointments)
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                close(error.toException())
//            }
//        }
//
//        appointmentsRef.addValueEventListener(listener)
//        awaitClose { appointmentsRef.removeEventListener(listener) }
//    }

    fun observeUser(userId: String): Flow<Result<User>> = callbackFlow {
        val userRef = database.getReference("users").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if (user != null) trySend(Result.success(user))
                else trySend(Result.failure(Exception("User not found")))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }
        userRef.addValueEventListener(listener)
        awaitClose {
            userRef.removeEventListener(listener)
            println("Flow getUserById đóng")
        }
    }

    fun getTimeSlots(): Flow<List<TimeSlot>> = callbackFlow {
        val timeSlotsRef = database.getReference("timeSlots")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Thêm log để debug
                Log.d("FirebaseApi", "Connection successful: ${snapshot.exists()}")
                Log.d("FirebaseApi", "Raw data: ${snapshot.value}")

                val timeSlotList = mutableListOf<TimeSlot>()

                if (!snapshot.exists()) {
                    Log.d("FirebaseApi", "No data exists")
                    trySend(timeSlotList)
                    return
                }

                // Parse theo cấu trúc trong database.json
                // timeSlots: { morning: [...], afternoon: [...], evening: [...] }
                val periods = listOf("morning", "afternoon", "evening")

                periods.forEach { periodName ->
                    val periodSnapshot = snapshot.child(periodName)
                    if (!periodSnapshot.exists()) {
                        Log.d("FirebaseApi", "Không có dữ liệu cho period: $periodName")
                        return@forEach
                    }

                    val timePeriod = when (periodName) {
                        "morning" -> TimePeriod.MORNING
                        "afternoon" -> TimePeriod.AFTERNOON
                        "evening" -> TimePeriod.EVENING
                        else -> null
                    } ?: return@forEach

                    periodSnapshot.children.forEach { slotSnapshot ->
                        try {
                            val id = slotSnapshot.child("id").getValue(Int::class.java)
                            val startTime =
                                slotSnapshot.child("startTime")
                                    .getValue(String::class.java)
                            val endTime = slotSnapshot.child("endTime")
                                .getValue(String::class.java)

                            if (id == null || startTime == null || endTime == null) {
                                Log.e(
                                    "FirebaseApi",
                                    "Thiếu thông tin cho time slot trong $periodName"
                                )
                                return@forEach
                            }

                            val timeSlot = TimeSlot(
                                id = id,
                                startTime = startTime,
                                endTime = endTime,
                                period = timePeriod
                            )

                            Log.d("FirebaseApi", "Đã parse được slot: $timeSlot")
                            timeSlotList.add(timeSlot)
                        } catch (e: Exception) {
                            Log.e(
                                "FirebaseApi",
                                "Lỗi khi parse slot trong $periodName: ${e.message}"
                            )
                        }
                    }
                }

                Log.d("FirebaseApi", "Total parsed slots: ${timeSlotList.size}")
                trySend(timeSlotList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseApi", "Firebase error: ${error.message}")
                Log.e("FirebaseApi", "Error details: ${error.details}")
                close(error.toException())
            }
        }

        // Thêm log để xác nhận listener được đăng ký
        Log.d("FirebaseApi", "Registering listener for timeSlots")
        timeSlotsRef.addValueEventListener(listener)

        awaitClose {
            timeSlotsRef.removeEventListener(listener)
            Log.d("FirebaseApi", "Listener removed")
        }
    }

    suspend fun updateUserField(
        userId: String,
        fieldName: String,
        fieldValue: Any
    ): Result<Unit> {
        return try {
            val userRef = database.getReference("users").child(userId)
            val updates = hashMapOf<String, Any>(fieldName to fieldValue)
            userRef.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseApi", "Error updating user field: ${e.message}")
            Result.failure(e)
        }
    }

    // Thêm phương thức để lấy thông tin người dùng
    suspend fun getUser(userId: String): Result<User> = runCatching {
        val snapshot = database.getReference("users").child(userId).get().await()
        if (snapshot.exists()) {
            val user = snapshot.getValue(User::class.java)
            user ?: throw Exception("Failed to parse user data")
        } else {
            throw Exception("User not found")
        }
    }

    // Thêm phương thức để lấy thông tin bác sĩ
    suspend fun getDoctor(doctorId: String): Result<Doctor> = runCatching {
        val snapshot = database.getReference("doctors").child(doctorId).get().await()
        if (snapshot.exists()) {
            try {
                Doctor(
                    id = doctorId,
                    code = snapshot.child("code").getValue(String::class.java) ?: "",
                    name = snapshot.child("name").getValue(String::class.java) ?: "",
                    specialty = snapshot.child("specialty").getValue(String::class.java) ?: "",
                    categoryId = snapshot.child("categoryId").getValue(Long::class.java)?.toInt() ?: 0,
                    rating = snapshot.child("rating").getValue(Double::class.java)?.toFloat() ?: 0F,
                    reviews = snapshot.child("reviews").getValue(Long::class.java) ?: 0L,
                    fee = snapshot.child("fee").getValue(Double::class.java) ?: 0.0,
                    avatar = snapshot.child("avatar").getValue(String::class.java) ?: "",
                    available = snapshot.child("available").getValue(Boolean::class.java) ?: true,
                    biography = snapshot.child("biography").getValue(String::class.java) ?: "",
                    role = try {
                        val roleString = snapshot.child("role").getValue(String::class.java) ?: "DOCTOR"
                        UserRole.valueOf(roleString)
                    } catch (e: Exception) {
                        UserRole.DOCTOR
                    },
                    email = snapshot.child("email").getValue(String::class.java) ?: "",
                    phoneNumber = snapshot.child("phoneNumber").getValue(String::class.java) ?: "",
                    emergencyContact = snapshot.child("emergencyContact").getValue(String::class.java) ?: "",
                    address = snapshot.child("address").getValue(String::class.java) ?: ""
                )
            } catch (e: Exception) {
                Log.e("FirebaseApi", "Error parsing doctor data: ${e.message}")
                throw e
            }
        } else {
            throw Exception("Doctor not found")
        }
    }

    // Thêm phương thức theo dõi các cuộc hẹn của người dùng
    fun observeUserAppointments(userId: String): Flow<Result<List<Appointment>>> = callbackFlow {
        Log.d("FirebaseApi", "Starting to observe appointments for user $userId")
        
        val appointmentsRef = database.getReference("appointments/byUser/$userId")
        val doctorsRef = database.getReference("doctors")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    Log.d("FirebaseApi", "Received appointment data: ${snapshot.exists()}, children count: ${snapshot.childrenCount}")
                    
                    val appointments = mutableListOf<Appointment>()
                    
                    if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                        Log.d("FirebaseApi", "No appointments found")
                        trySend(Result.success(emptyList()))
                        return
                    }
                    
                    // Đếm số lượng cuộc hẹn cần xử lý
                    var pendingAppointments = snapshot.childrenCount
                    
                    for (appointmentSnapshot in snapshot.children) {
                        val appointmentId = appointmentSnapshot.key ?: continue
                        
                        // Truy cập trực tiếp các field thay vì convert toàn bộ thành HashMap
                        val doctorId = appointmentSnapshot.child("doctorId").getValue(String::class.java) ?: ""
                        val date = appointmentSnapshot.child("date").getValue(String::class.java) ?: ""
                        val slotId = appointmentSnapshot.child("slotId").getValue(Long::class.java)?.toInt() ?: 0
                        val status = appointmentSnapshot.child("status").getValue(String::class.java) ?: "upcoming"
                        val createdAt = appointmentSnapshot.child("createdAt").getValue(Long::class.java) ?: 0L
                        val symptoms = appointmentSnapshot.child("symptoms").getValue(String::class.java)
                        val notes = appointmentSnapshot.child("notes").getValue(String::class.java)
                        
                        Log.d("FirebaseApi", "Processing appointment: $appointmentId, doctor: $doctorId")
                        
                        // Lấy thông tin bác sĩ
                        doctorsRef.child(doctorId).get().addOnSuccessListener { doctorSnapshot ->
                            try {
                                // Lấy thông tin của bác sĩ
                                val doctorName = doctorSnapshot.child("name").getValue(String::class.java) ?: "Bác sĩ"
                                val doctorAvatar = doctorSnapshot.child("avatar").getValue(String::class.java) ?: ""
                                
                                // Tạo đối tượng Appointment
                                val appointment = Appointment(
                                    id = appointmentId,
                                    doctorId = doctorId,
                                    userId = userId,
                                    date = date,
                                    slotId = slotId,
                                    doctorName = doctorName,
                                    doctorAvatar = doctorAvatar,
                                    status = status,
                                    createdAt = createdAt,
                                    symptoms = symptoms,
                                    notes = notes
                                )
                                
                                // Thêm vào danh sách
                                appointments.add(appointment)
                                Log.d("FirebaseApi", "Added appointment: $appointmentId, doctor: $doctorName")
                                
                                // Giảm số lượng đang chờ
                                pendingAppointments--
                                
                                // Nếu đã xử lý hết, gửi kết quả
                                if (pendingAppointments <= 0) {
                                    // Sắp xếp theo ngày giảm dần (mới nhất trước)
                                    val sortedAppointments = appointments.sortedByDescending { it.date }
                                    Log.d("FirebaseApi", "All appointments processed, returning ${sortedAppointments.size} items")
                                    trySend(Result.success(sortedAppointments))
                                }
                            } catch (e: Exception) {
                                Log.e("FirebaseApi", "Error processing doctor data: ${e.message}", e)
                                pendingAppointments--
                                
                                if (pendingAppointments <= 0) {
                                    trySend(Result.success(appointments))
                                }
                            }
                        }.addOnFailureListener { e ->
                            Log.e("FirebaseApi", "Error fetching doctor: ${e.message}", e)
                            
                            // Tạo appointment với thông tin mặc định nếu không lấy được thông tin bác sĩ
                            val appointment = Appointment(
                                id = appointmentId,
                                doctorId = doctorId,
                                userId = userId,
                                date = date,
                                slotId = slotId,
                                doctorName = "Bác sĩ",
                                status = status,
                                createdAt = createdAt,
                                symptoms = symptoms,
                                notes = notes
                            )
                            
                            appointments.add(appointment)
                            pendingAppointments--
                            
                            if (pendingAppointments <= 0) {
                                trySend(Result.success(appointments))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseApi", "Error processing appointments: ${e.message}", e)
                    trySend(Result.failure(e))
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseApi", "Database error: ${error.message}")
                trySend(Result.failure(error.toException()))
            }
        }
        
        appointmentsRef.addValueEventListener(listener)
        
        awaitClose {
            appointmentsRef.removeEventListener(listener)
        }
    }
}