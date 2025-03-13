package com.healthtech.doccareplus.domain.mapper

import com.healthtech.doccareplus.data.local.entity.DoctorEntity
import com.healthtech.doccareplus.domain.model.Doctor
import com.healthtech.doccareplus.domain.model.UserRole

fun DoctorEntity.toDoctor(): Doctor {
    return Doctor(
        id = id,
        code = code,
        name = name,
        specialty = specialty,
        categoryId = categoryId,
        rating = rating,
        reviews = reviews,
        fee = fee,
        avatar = avatar,
        available = available,
        biography = biography,
        role = try {
            UserRole.valueOf(role)
        } catch (e: Exception) {
            UserRole.DOCTOR // Default value
        },
        email = email,
        phoneNumber = phoneNumber,
        emergencyContact = emergencyContact,
        address = address
    )
}

fun Doctor.toDoctorEntity(): DoctorEntity {
    return DoctorEntity(
        id = id,
        code = code,
        name = name,
        specialty = specialty,
        categoryId = categoryId,
        rating = rating,
        reviews = reviews,
        fee = fee,
        avatar = avatar,
        available = available,
        biography = biography,
        role = role.name,
        email = email,
        phoneNumber = phoneNumber,
        emergencyContact = emergencyContact,
        address = address
    )
}