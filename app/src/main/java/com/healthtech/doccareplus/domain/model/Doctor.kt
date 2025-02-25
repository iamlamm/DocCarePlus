package com.healthtech.doccareplus.domain.model

data class Doctor(
    val id: Int = 0,
    val code: String = "",
    val name: String = "",
    val specialty: String = "",
    val categoryId: Int = 0,
    val rating: Float = 0F,
    val reviews: Long = 0L,
    val fee: Double = 0.0,
    val image: String = "",
    val available: Boolean = true,
    val biography: String = ""
)
