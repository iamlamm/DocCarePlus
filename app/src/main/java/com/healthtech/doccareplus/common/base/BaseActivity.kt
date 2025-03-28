package com.healthtech.doccareplus.common.base

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.healthtech.doccareplus.data.local.preferences.UserPreferences
import java.util.Locale

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val userPrefs = UserPreferences(newBase)
        val languageCode = userPrefs.getLanguage()
        val locale = Locale(languageCode)
        val config = newBase.resources.configuration
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }
}