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

@Singleton
class AuthApi @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
    private val networkUtils: NetworkUtils
) {
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
        name: String, email: String, password: String, phoneNumber: String
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
                    id = firebaseUser.uid, name = name, email = email, phoneNumber = phoneNumber
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
                                register(name, email, password, phoneNumber)
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

    suspend fun fetchCurrentUser(): Result<User> {
        return try {
            if (!networkUtils.isNetworkAvailable()) {
                return Result.failure(Exception("Không có kết nối internet!"))
            }

            val firebaseUser = auth.currentUser
                ?: return Result.failure(Exception("Chưa đăng nhập"))

            val userSnapshot = database.getReference("users")
                .child(firebaseUser.uid)
                .get()
                .await()

            val userData = userSnapshot.getValue(User::class.java)
                ?: return Result.failure(Exception("Không tìm thấy thông tin người dùng"))

            Result.success(userData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}