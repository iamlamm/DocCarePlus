package com.healthtech.doccareplus.di

import android.content.Context
import androidx.room.Room
import com.healthtech.doccareplus.data.local.AppDataBase
import com.healthtech.doccareplus.data.local.dao.CategoryDao
import com.healthtech.doccareplus.data.local.dao.DoctorDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDataBase {
        return Room.databaseBuilder(context, AppDataBase::class.java, "app_database")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: AppDataBase): CategoryDao {
        return database.categoryDao()
    }


    @Provides
    @Singleton
    fun provideDoctorDao(database: AppDataBase): DoctorDao {
        return database.doctorDao()
    }
}