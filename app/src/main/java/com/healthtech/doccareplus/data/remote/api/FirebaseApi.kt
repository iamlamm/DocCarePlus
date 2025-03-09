package com.healthtech.doccareplus.data.remote.api

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.healthtech.doccareplus.domain.model.Category
import com.healthtech.doccareplus.domain.model.Doctor
import com.healthtech.doccareplus.domain.model.User
import com.healthtech.doccareplus.domain.model.TimeSlot
import com.healthtech.doccareplus.domain.model.TimePeriod
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
                val doctorList = mutableListOf<Doctor>()
                for (doctorSnapshot in snapshot.children) {
                    doctorSnapshot.getValue(Doctor::class.java)?.let { doctor ->
                        doctorList.add(doctor)
                    }
                }
                // Gửi danh sách doctor mới vào Flow
                trySend(doctorList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ERROR LOADING DOCTOR", error.message)
            }
        }
        // Đăng ký listener với Firebase
        doctorsRef.addValueEventListener(listener)
        // Khi Flow bị hủy, remove listener để tránh memory leak
        awaitClose {
            doctorsRef.removeEventListener(listener)
            println("Flow getDoctors đóng")
        }
    }

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
                                slotSnapshot.child("startTime").getValue(String::class.java)
                            val endTime = slotSnapshot.child("endTime").getValue(String::class.java)

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

    suspend fun updateUserField(userId: String, fieldName: String, fieldValue: Any): Result<Unit> {
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
}