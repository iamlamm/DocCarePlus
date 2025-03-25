package com.healthtech.doccareplus.domain.service

sealed class CloudinaryUploadState {
    data object Idle : CloudinaryUploadState()
    data class Loading(val progress: Int = 0) : CloudinaryUploadState()
    data class Success(val imageUrl: String) : CloudinaryUploadState()
    data class Error(val message: String) : CloudinaryUploadState()
}