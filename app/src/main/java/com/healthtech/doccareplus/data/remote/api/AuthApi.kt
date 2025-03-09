package com.healthtech.doccareplus.data.remote.api

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.database.FirebaseDatabase
import com.healthtech.doccareplus.domain.model.User
import com.healthtech.doccareplus.utils.Constants
import com.healthtech.doccareplus.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.TimeUnit

@Singleton
class AuthApi @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
    private val networkUtils: NetworkUtils
) {
    init {
        setupEmailVerificationListener()
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                return Result.failure(Exception("Không có kết nối internet!"))
            }

            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("Đăng nhập thất bại"))

            // Kiểm tra xem Email đã được xác thực hay chưa
            if (!firebaseUser.isEmailVerified) {
                // Tự động gửi lại email xác thực nếu chưa xác thực
                firebaseUser.sendEmailVerification().await()
                return Result.failure(Exception("Email chưa được xác thực. Vui lòng kiểm tra email của bạn."))
            }

            val userSnapshot = database.getReference("users").child(firebaseUser.uid).get().await()
            val userData = userSnapshot.getValue(User::class.java) ?: return Result.failure(
                Exception("Không tìm thấy thông tin người dùng")
            )

            Result.success(userData)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("Tài khoản không tồn tại"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Email hoặc mật khẩu không chính xác"))
        } catch (e: Exception) {
            Result.failure(Exception("Đã có lỗi xảy ra, vui lòng thử lại sau"))
        }
    }

    suspend fun register(
        name: String, email: String, password: String, phoneNumber: String, avatar: String
    ): Result<User> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                return Result.failure(Exception("Không có kết nối internet!"))
            }

            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser =
                    result.user ?: return Result.failure(Exception("Đăng ký thất bại"))

                firebaseUser.sendEmailVerification().await()

                val user = User(
                    id = firebaseUser.uid,
                    name = name,
                    email = email,
                    phoneNumber = phoneNumber,
                    avatar = avatar
                )
                database.getReference("users").child(user.id).setValue(user).await()
                Result.success(user)

            } catch (e: FirebaseAuthUserCollisionException) {
                // Xử lý trường hợp email đã tồn tại
                try {
                    // Kiểm tra trong database
                    val users =
                        database.getReference("users").orderByChild("email").equalTo(email).get()
                            .await()

                    if (users.exists()) {
                        try {
                            // Thử đăng nhập để kiểm tra trạng thái xác thực
                            val signInResult =
                                auth.signInWithEmailAndPassword(email, password).await()

                            if (signInResult.user?.isEmailVerified == true) {
                                Result.failure(Exception("Email này đã được đăng ký và xác thực. Vui lòng sử dụng email khác hoặc đăng nhập với email này."))
                            } else {
                                // Email chưa xác thực, xóa tài khoản cũ
                                signInResult.user?.delete()?.await()
                                users.children.first().ref.removeValue().await()

                                // Đệ quy để tạo tài khoản mới
                                register(name, email, password, phoneNumber, avatar)
                            }
                        } catch (e: FirebaseAuthInvalidCredentialsException) {
                            Result.failure(Exception("Email này đã được đăng ký với mật khẩu khác. Vui lòng sử dụng email khác hoặc đăng nhập với mật khẩu đúng."))
                        } catch (e: Exception) {
                            Result.failure(Exception("Email này đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập."))
                        }
                    } else {
                        Result.failure(Exception("Email này đã được đăng ký nhưng không tìm thấy thông tin trong hệ thống. Vui lòng liên hệ hỗ trợ."))
                    }
                } catch (e: Exception) {
                    Result.failure(Exception("Lỗi khi kiểm tra thông tin tài khoản: ${e.message}"))
                }
            } catch (e: FirebaseAuthWeakPasswordException) {
                Result.failure(Exception("Mật khẩu quá yếu. Vui lòng chọn mật khẩu mạnh hơn."))
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Result.failure(Exception("Email không hợp lệ. Vui lòng kiểm tra lại."))
            } catch (e: Exception) {
                Result.failure(Exception("Lỗi đăng ký: ${e.message}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi không xác định: ${e.message}"))
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!networkUtils.isNetworkAvailable()) {
                return@withContext Result.failure(Exception("Không có kết nối internet"))
            }

            // Kiểm tra email có tồn tại trong hệ thống không
            val usersRef = database.getReference("users")
            val snapshot = usersRef.orderByChild("email").equalTo(email).get().await()

            if (!snapshot.exists()) {
                return@withContext Result.failure(Exception("Email này chưa được đăng ký trong hệ thống"))
            }

            try {
                auth.sendPasswordResetEmail(email).await()
                Result.success(Unit)
            } catch (e: FirebaseAuthInvalidUserException) {
                Result.failure(Exception("Email không tồn tại trong hệ thống"))
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Result.failure(Exception("Email không hợp lệ"))
            } catch (e: Exception) {
                Result.failure(Exception("Không thể gửi email khôi phục: ${e.message}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi không xác định: ${e.message}"))
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun updateEmail(currentPassword: String, newEmail: String): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                if (!networkUtils.isNetworkAvailable()) {
                    return@withContext Result.failure(Exception("Không có kết nối internet!"))
                }

                val user = auth.currentUser ?: return@withContext Result.failure(
                    Exception("Người dùng chưa đăng nhập")
                )

                // 1. Xác thực lại người dùng
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                user.reauthenticate(credential).await()

                // 2. Kiểm tra email mới có tồn tại trong hệ thống không
                val usersRef = database.getReference("users")
                val snapshot = usersRef.orderByChild("email").equalTo(newEmail).get().await()
                if (snapshot.exists()) {
                    return@withContext Result.failure(
                        Exception("Email này đã được sử dụng bởi tài khoản khác")
                    )
                }

                // 3. Gửi email xác minh đến địa chỉ mới
                user.verifyBeforeUpdateEmail(newEmail).await()

                // 4. Lưu thông tin email mới vào một node tạm thời
                val pendingEmailRef = database.getReference("pending_email_changes").child(user.uid)
                pendingEmailRef.setValue(mapOf(
                    "newEmail" to newEmail,
                    "timestamp" to System.currentTimeMillis(),
                    "currentEmail" to user.email
                )).await()

                // 5. Trả về thành công, chờ người dùng xác thực
                Result.success(Unit)

            } catch (e: FirebaseAuthInvalidCredentialsException) {
                when (e.errorCode) {
                    "ERROR_WRONG_PASSWORD" -> 
                        Result.failure(Exception("Mật khẩu không chính xác"))
                    "ERROR_INVALID_EMAIL" -> 
                        Result.failure(Exception("Email mới không hợp lệ"))
                    else -> 
                        Result.failure(Exception("Thông tin xác thực không hợp lệ"))
                }
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                Result.failure(Exception("Vui lòng đăng nhập lại để thực hiện thao tác này"))
            } catch (e: Exception) {
                Result.failure(Exception("Lỗi cập nhật email: ${e.message}"))
            }
        }
    
    suspend fun cancelEmailChange(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = auth.currentUser ?: return@withContext Result.failure(
                Exception("Người dùng chưa đăng nhập")
            )
            
            // Xóa pending change
            database.getReference("pending_email_changes")
                .child(user.uid)
                .removeValue()
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi khi hủy thay đổi email: ${e.message}"))
        }
    }
    
    suspend fun checkPendingEmailChange(): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val user = auth.currentUser ?: return@withContext Result.failure(
                Exception("Người dùng chưa đăng nhập")
            )
            
            val snapshot = database.getReference("pending_email_changes")
                .child(user.uid)
                .get()
                .await()
                
            val pendingEmail = if (snapshot.exists()) {
                snapshot.child("newEmail").getValue(String::class.java)
            } else null
            
            Result.success(pendingEmail)
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi khi kiểm tra thay đổi email: ${e.message}"))
        }
    }

    private fun setupEmailVerificationListener() {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null && user.isEmailVerified) {
                // Kiểm tra xem có pending email change không
                database.getReference("pending_email_changes")
                    .child(user.uid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            val newEmail = snapshot.child("newEmail").getValue(String::class.java)
                            if (newEmail != null) {
                                // Cập nhật email trong database chính
                                database.getReference("users")
                                    .child(user.uid)
                                    .child("email")
                                    .setValue(newEmail)
                                    .addOnSuccessListener {
                                        // Xóa pending email change
                                        snapshot.ref.removeValue()
                                    }
                            }
                        }
                    }
            }
        }
        
        // Tự động xóa pending changes quá hạn (24 giờ)
        cleanupExpiredPendingChanges()
    }
    
    private fun cleanupExpiredPendingChanges() {
        val expirationTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1) // 24 giờ
        database.getReference("pending_email_changes")
            .orderByChild("timestamp")
            .endAt(expirationTime.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { it.ref.removeValue() }
                }
                override fun onCancelled(error: DatabaseError) {
                    // Xử lý lỗi
                }
            })
    }
}