package com.healthtech.doccareplus.common.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.common.state.UiState
import com.healthtech.doccareplus.domain.model.Category
import com.healthtech.doccareplus.domain.model.Doctor
import com.healthtech.doccareplus.domain.repository.CategoryRepository
import com.healthtech.doccareplus.domain.repository.DoctorRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

abstract class BaseDataViewModel : ViewModel() {
    protected val _categories = MutableStateFlow<UiState<List<Category>>>(UiState.Idle)
    val categories = _categories.asStateFlow()

    protected val _doctors = MutableStateFlow<UiState<List<Doctor>>>(UiState.Idle)
    val doctors = _doctors.asStateFlow()

    // Cache data
    private var cachedCategories: List<Category>? = null
    private var cachedDoctors: List<Doctor>? = null

    // For tracking active network calls
    private var categoriesJob: Job? = null
    private var doctorsJob: Job? = null

    /**
     * Observe categories with cache support and controlled subscription management
     * @param forceRefresh Set to true to ignore cache and fetch fresh data
     */
    protected fun observeCategories(repository: CategoryRepository, forceRefresh: Boolean = false) {
        // Return cached data immediately if available and not forcing refresh
        if (!forceRefresh && cachedCategories != null) {
            _categories.value = UiState.Success(cachedCategories!!)
            return // Don't trigger network call if we have cache and not forcing refresh
        }

        // We'll only reach here if we need to fetch new data, so set loading state
        _categories.value = UiState.Loading

        // Cancel existing job if any before starting a new one
        categoriesJob?.cancel()

        categoriesJob = viewModelScope.launch {
            try {
                repository.observeCategories().collect { result ->
                    result.onSuccess { categories ->
                        cachedCategories = categories
                        _categories.value = UiState.Success(categories)
                    }.onFailure { error ->
                        if (error !is CancellationException) {
                            _categories.value = UiState.Error(error.message ?: "Unknown error")
                        }
                    }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _categories.value = UiState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Observe doctors with cache support and controlled subscription management
     * @param forceRefresh Set to true to ignore cache and fetch fresh data
     */
    protected fun observeDoctors(repository: DoctorRepository, forceRefresh: Boolean = false) {
        // Return cached data immediately if available and not forcing refresh
        if (!forceRefresh && cachedDoctors != null) {
            _doctors.value = UiState.Success(cachedDoctors!!)
            return // Don't trigger network call if we have cache and not forcing refresh
        }

        // We'll only reach here if we need to fetch new data, so set loading state
        _doctors.value = UiState.Loading

        // Cancel existing job if any before starting a new one
        doctorsJob?.cancel()

        doctorsJob = viewModelScope.launch {
            try {
                repository.observeDoctors().collect { result ->
                    result.onSuccess { doctors ->
                        cachedDoctors = doctors
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

    /**
     * Force refresh categories data
     */
    protected fun refreshCategories(categoryRepository: CategoryRepository) {
        observeCategories(categoryRepository, forceRefresh = true)
    }

    /**
     * Force refresh doctors data
     */
    protected fun refreshDoctors(doctorRepository: DoctorRepository) {
        observeDoctors(doctorRepository, forceRefresh = true)
    }

    /**
     * Force refresh all data
     */
    protected fun refreshAllData(
        categoryRepository: CategoryRepository, doctorRepository: DoctorRepository
    ) {
        refreshCategories(categoryRepository)
        refreshDoctors(doctorRepository)
    }

    override fun onCleared() {
        super.onCleared()
        cachedCategories = null
        cachedDoctors = null
        categoriesJob?.cancel()
        doctorsJob?.cancel()
    }
}

/**
 * 15h 2502
 */
//abstract class BaseDataViewModel : ViewModel() {
//    protected val _categories = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
//    val categories = _categories.asStateFlow()
//
//    protected val _doctors = MutableStateFlow<UiState<List<Doctor>>>(UiState.Loading)
//    val doctors = _doctors.asStateFlow()
//
//    // Cache data
//    private var cachedCategories: List<Category>? = null
//    private var cachedDoctors: List<Doctor>? = null
//
//    protected fun observeCategories(repository: CategoryRepository) {
//        if (cachedCategories != null) {
//            _categories.value = UiState.Success(cachedCategories!!)
//        }
//
//        viewModelScope.launch {
//            try {
//                repository.observeCategories()
//                    .collect { result ->
//                        result.onSuccess { categories ->
//                            cachedCategories = categories
//                            _categories.value = UiState.Success(categories)
//                        }.onFailure { error ->
//                            if (error !is CancellationException) {
//                                _categories.value = UiState.Error(error.message ?: "Unknown error")
//                            }
//                        }
//                    }
//            } catch (e: Exception) {
//                if (e !is CancellationException) {
//                    _categories.value = UiState.Error(e.message ?: "Unknown error")
//                }
//            }
//        }
//    }
//
//    protected fun observeDoctors(repository: DoctorRepository) {
//        if (cachedDoctors != null) {
//            _doctors.value = UiState.Success(cachedDoctors!!)
//        }
//
//        viewModelScope.launch {
//            try {
//                repository.observeDoctors()
//                    .collect { result ->
//                        result.onSuccess { doctors ->
//                            cachedDoctors = doctors
//                            _doctors.value = UiState.Success(doctors)
//                        }.onFailure { error ->
//                            if (error !is CancellationException) {
//                                _doctors.value = UiState.Error(error.message ?: "Unknown error")
//                            }
//                        }
//                    }
//            } catch (e: Exception) {
//                if (e !is CancellationException) {
//                    _doctors.value = UiState.Error(e.message ?: "Unknown error")
//                }
//            }
//        }
//    }
//
////    protected fun observeCategories(repository: CategoryRepository) {
////        viewModelScope.launch {
////            try {
////                repository.observeCategories()
////                    .collect { result ->
////                        result.onSuccess { categories ->
////                            _categories.value = UiState.Success(categories)
////                        }.onFailure { error ->
////                            // Không set Error state khi CancellationException
////                            if (error !is CancellationException) {
////                                _categories.value = UiState.Error(error.message ?: "Unknown error")
////                            }
////                        }
////                    }
////            } catch (e: Exception) {
////                // Không set Error state khi CancellationException
////                if (e !is CancellationException) {
////                    _categories.value = UiState.Error(e.message ?: "Unknown error")
////                }
////            }
////        }
////    }
////
////    protected fun observeDoctors(repository: DoctorRepository) {
////        viewModelScope.launch {
////            try {
////                repository.observeDoctors()
////                    .collect { result ->
////                        result.onSuccess { doctors ->
////                            _doctors.value = UiState.Success(doctors)
////                        }.onFailure { error ->
////                            // Không set Error state khi CancellationException
////                            if (error !is CancellationException) {
////                                _doctors.value = UiState.Error(error.message ?: "Unknown error")
////                            }
////                        }
////                    }
////            } catch (e: Exception) {
////                // Không set Error state khi CancellationException
////                if (e !is CancellationException) {
////                    _doctors.value = UiState.Error(e.message ?: "Unknown error")
////                }
////            }
////        }
////    }
//
//    override fun onCleared() {
//        super.onCleared()
//        cachedCategories = null
//        cachedDoctors = null
//    }
//}