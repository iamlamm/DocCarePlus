package com.healthtech.doccareplus.data.remote.api

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.healthtech.doccareplus.domain.model.Category
import com.healthtech.doccareplus.domain.model.Doctor
import com.healthtech.doccareplus.domain.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseApi @Inject constructor(
    private val database: FirebaseDatabase
) {
    //    callbackFlow {}: Mở một luồng để lắng nghe sự kiện.
    //    trySend(value): Gửi giá trị vào Flow.
    //    awaitClose {}: Đóng Flow khi không còn lắng nghe nữa.

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
}