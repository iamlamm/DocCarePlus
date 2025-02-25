package com.healthtech.doccareplus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doctors")
data class DoctorEntity(
    @PrimaryKey
    val id: Int,
    val code: String,
    val name: String,
    val specialty: String,
    val categoryId: Int,
    val rating: Float,
    val reviews: Long,
    val fee: Double,
    val image: String,
    val available: Boolean,
    val biography: String
)
