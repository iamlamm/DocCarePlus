package com.healthtech.doccareplus.ui.doctor

import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.common.base.BaseDataViewModel
import com.healthtech.doccareplus.common.state.UiState
import com.healthtech.doccareplus.domain.model.Doctor
import com.healthtech.doccareplus.domain.repository.DoctorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllDoctorsViewModel @Inject constructor(
    private val doctorRepository: DoctorRepository
) : BaseDataViewModel() {
    
    // Chỉ để theo dõi trạng thái đã tải xong chưa
    private val _isInitialDataLoaded = MutableStateFlow(false)
    val isInitialDataLoaded = _isInitialDataLoaded.asStateFlow()
    
    init {
        // Sử dụng lại phương thức từ BaseDataViewModel
        viewModelScope.launch(Dispatchers.IO) {
            observeDoctors(doctorRepository)
            _isInitialDataLoaded.value = true
        }
    }
    
    // Hàm này chỉ gọi khi cần refresh dữ liệu
    fun refreshDoctorsIfNeeded() {
        if (doctors.value !is UiState.Success) {
            viewModelScope.launch(Dispatchers.IO) {
                // Sử dụng phương thức refresh từ BaseDataViewModel
                refreshDoctors(doctorRepository)
            }
        }
    }
}