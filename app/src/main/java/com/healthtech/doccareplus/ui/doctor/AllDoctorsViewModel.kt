package com.healthtech.doccareplus.ui.doctor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.Query
import com.healthtech.doccareplus.common.base.BaseDataViewModel
import com.healthtech.doccareplus.common.state.UiState
import com.healthtech.doccareplus.domain.model.Doctor
import com.healthtech.doccareplus.domain.repository.DoctorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class AllDoctorsViewModel @Inject constructor(
    private val doctorRepository: DoctorRepository, savedStateHandle: SavedStateHandle
) : BaseDataViewModel() {

    // Chỉ để theo dõi trạng thái đã tải xong chưa
    private val _isInitialDataLoaded = MutableStateFlow(false)
    val isInitialDataLoaded = _isInitialDataLoaded.asStateFlow()

    // Lưu trữ thông tin category
    private val _categoryId = MutableStateFlow<Int?>(null)
    val categoryId = _categoryId.asStateFlow()

    private val _categoryName = MutableStateFlow<String?>(null)
    val categoryName = _categoryName.asStateFlow()

    // Cache doctors để tối ưu hiệu suất (thêm vào để khắc phục lỗi)
    private var cachedDoctorsByCategory: List<Doctor>? = null

    // Theo dõi thời gian cập nhật cuối
    private var lastRefreshTime = 0L

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Doctor>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    // Lưu trữ danh sách gốc
    private var originalDoctors = listOf<Doctor>()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive = _isSearchActive.asStateFlow()

    init {
        // Lấy categoryId và categoryName từ navigation arguments
        savedStateHandle.get<Int>("categoryId")?.let { catId ->
            if (catId > -1) { // Kiểm tra giá trị mặc định -1
                _categoryId.value = catId
            }
        }

        savedStateHandle.get<String>("categoryName")?.let { catName ->
            _categoryName.value = catName
        }

        // Bắt đầu load dữ liệu phù hợp
        loadInitialData()

        // Quan sát doctors để cập nhật original list
        viewModelScope.launch {
            doctors.collect { state ->
                if (state is UiState.Success) {
                    if (!_isSearchActive.value) {
                        _searchResults.value = state.data
                        originalDoctors = state.data
                    } else {
                        originalDoctors = state.data
                        filterDoctors(_searchQuery.value)
                    }
                }
            }
        }
    }

    // Hàm đặt query tìm kiếm
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterDoctors(query)
    }

    // Hàm lọc danh sách doctors
    private fun filterDoctors(query: String) {
        if (query.isEmpty()) {
            _searchResults.value = originalDoctors
            return
        }

        val filteredList = originalDoctors.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.specialty.contains(query, ignoreCase = true)
        }

        _searchResults.value = filteredList
    }

    // Hàm xóa query tìm kiếm
    fun clearSearch() {
        _searchQuery.value = ""
        doctors.value.let { state ->
            if (state is UiState.Success) {
                _searchResults.value = state.data
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val catId = _categoryId.value

            if (catId != null) {
                // Nếu có categoryId, load doctors theo category
                loadDoctorsByCategory(catId)
            } else {
                // Nếu không có categoryId, load tất cả doctors
                observeDoctors(doctorRepository)
            }

            _isInitialDataLoaded.value = true
        }
    }

    private fun loadDoctorsByCategory(categoryId: Int) {
        viewModelScope.launch {
            try {
                _doctors.value = UiState.Loading

                doctorRepository.getDoctorsByCategory(categoryId).collect { result ->
                    result.onSuccess { doctors ->
                        // Lưu vào cache local thay vì sử dụng cachedDoctors từ BaseDataViewModel
                        cachedDoctorsByCategory = doctors
                        originalDoctors = doctors  // Lưu danh sách gốc
                        _doctors.value = UiState.Success(doctors)
                    }.onFailure { error ->
                        if (error !is CancellationException) {
                            _doctors.value = UiState.Error(error.message ?: "Unknown error")
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _doctors.value = UiState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    // Hàm này kiểm tra và refresh chỉ khi cần thiết
    fun checkAndRefreshIfNeeded() {
        val currentTime = System.currentTimeMillis()
        // Chỉ refresh nếu đã trôi qua ít nhất 1 phút từ lần cuối
        if (currentTime - lastRefreshTime > 60000) {
            refreshDoctorsByCategory()
        }
    }

    // Refresh dữ liệu theo category hoặc tất cả
    private fun refreshDoctorsByCategory() {
        val catId = _categoryId.value

        if (catId != null) {
            // Nếu có categoryId, load lại doctors theo category
            loadDoctorsByCategory(catId)
        } else {
            // Nếu không có categoryId, refresh tất cả doctors
            refreshDoctors(doctorRepository)
        }

        lastRefreshTime = System.currentTimeMillis()
    }

    // Hàm này gọi khi người dùng chủ động refresh (pull-to-refresh)
    fun refreshDoctors() {
        viewModelScope.launch {
            try {
                lastRefreshTime = System.currentTimeMillis()
                _doctors.value = UiState.Loading
                
                val catId = _categoryId.value
                if (catId != null) {
                    // Buộc tải lại dữ liệu bác sĩ theo category
                    loadDoctorsByCategory(catId)
                } else {
                    // Tải lại tất cả bác sĩ
                    observeDoctors(doctorRepository)
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _doctors.value = UiState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            clearSearch()
        }
    }
}