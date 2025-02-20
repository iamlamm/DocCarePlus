package com.healthtech.doccareplus.ui.doctor

import androidx.lifecycle.ViewModel
import com.healthtech.doccareplus.domain.model.Doctor
import com.healthtech.doccareplus.domain.repository.DoctorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class AllDoctorsViewModel @Inject constructor(
    private val doctorRepository: DoctorRepository
) : ViewModel() {
    val doctors: Flow<List<Doctor>> = doctorRepository.getDoctors()
}