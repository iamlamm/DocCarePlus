package com.healthtech.doccareplus.domain.repository

import com.healthtech.doccareplus.domain.model.Doctor
import kotlinx.coroutines.flow.Flow

interface DoctorRepository {
    fun observeDoctors(): Flow<Result<List<Doctor>>>
    
    // Thêm phương thức lọc bác sĩ theo category
    fun getDoctorsByCategory(categoryId: Int): Flow<Result<List<Doctor>>>
}