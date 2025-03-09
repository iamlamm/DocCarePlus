package com.healthtech.doccareplus.data.local.preferences

import android.content.Context
import com.healthtech.doccareplus.domain.model.User
import com.healthtech.doccareplus.domain.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun saveUser(user: User) {
        sharedPreferences.edit().apply {
            putString("user_id", user.id)
            putString("user_name", user.name)
            putString("user_email", user.email)
            putString("user_phone", user.phoneNumber)
            putString("user_role", user.role.name)
            putString("user_avatar", user.avatar)
            putLong("user_created_at", user.createdAt)
            putBoolean("is_logged_in", true)
            apply()
        }
    }

    fun getUser(): User? {
        val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)
        val rememberMe = sharedPreferences.getBoolean("remember_me", false)

        if (!isLoggedIn || !rememberMe) return null

        return User(
            id = sharedPreferences.getString("user_id", "") ?: "",
            name = sharedPreferences.getString("user_name", "") ?: "",
            email = sharedPreferences.getString("user_email", "") ?: "",
            phoneNumber = sharedPreferences.getString("user_phone", "") ?: "",
            role = UserRole.valueOf(
                sharedPreferences.getString("user_role", UserRole.PATIENT.name)
                    ?: UserRole.PATIENT.name
            ),
            avatar = sharedPreferences.getString("user_avatar", "") ?: "",
            createdAt = sharedPreferences.getLong("user_created_at", System.currentTimeMillis())
        )
    }

    fun clearUser() {
        sharedPreferences.edit().apply {
            remove("user_id")
            remove("user_name")
            remove("user_email")
            remove("user_phone")
            remove("user_role")
            remove("user_avatar")
            remove("user_created_at")
            remove("is_logged_in")
            apply()
        }
    }

    fun saveRememberMe(isChecked: Boolean) {
        sharedPreferences.edit().putBoolean("remember_me", isChecked).apply()
    }

    fun isRememberMeChecked(): Boolean {
        return sharedPreferences.getBoolean("remember_me", false)
    }

    fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean("is_logged_in", false) &&
                sharedPreferences.getBoolean("remember_me", false)
    }

}