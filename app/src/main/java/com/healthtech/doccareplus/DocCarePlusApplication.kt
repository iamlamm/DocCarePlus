package com.healthtech.doccareplus

import android.app.Application
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DocCarePlusApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        Firebase.initialize(this)
//        Firebase.database.setPersistenceEnabled(true)
    }
}