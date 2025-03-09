package com.healthtech.doccareplus.di

import com.healthtech.doccareplus.data.service.BookingServiceImpl
import com.healthtech.doccareplus.data.service.CloudinaryServiceImpl
import com.healthtech.doccareplus.data.service.NotificationServiceImpl
import com.healthtech.doccareplus.domain.service.BookingService
import com.healthtech.doccareplus.domain.service.CloudinaryService
import com.healthtech.doccareplus.domain.service.NotificationService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    @Binds
    @Singleton
    abstract fun bindBookingService(
        bookingServiceImpl: BookingServiceImpl
    ): BookingService

    @Binds
    @Singleton
    abstract fun bindNotificationService(
        notificationServiceImpl: NotificationServiceImpl
    ): NotificationService

    @Binds
    @Singleton
    abstract fun bindCloudinaryService(
        cloudinaryServiceImpl: CloudinaryServiceImpl
    ): CloudinaryService
}