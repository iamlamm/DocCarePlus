package com.healthtech.doccareplus.domain.mapper

import com.healthtech.doccareplus.data.local.entity.DoctorEntity
import com.healthtech.doccareplus.domain.model.Doctor

fun DoctorEntity.toDoctor(): Doctor {
    return Doctor(
        id = id,
        code = code,
        name = name,
        specialty = specialty,
        rating = rating,
        reviews = reviews,
        fee = fee,
        image = image,
        available = available
    )
}

fun Doctor.toDoctorEntity(): DoctorEntity {
    return DoctorEntity(
        id = id,
        code = code,
        name = name,
        specialty = specialty,
        rating = rating,
        reviews = reviews,
        fee = fee,
        image = image,
        available = available
    )
}