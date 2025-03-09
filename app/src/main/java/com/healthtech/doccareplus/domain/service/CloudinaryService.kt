package com.healthtech.doccareplus.domain.service

import android.net.Uri
import com.healthtech.doccareplus.utils.Constants
import kotlinx.coroutines.flow.Flow

interface CloudinaryService {
    fun uploadImage(
        imageUri: Uri,
        folder: String = Constants.CLOUDINARY_FOLDER_STORE_AVATAR,
        fileName: String? = null,
        overwrite: Boolean = true
    ): Flow<CloudinaryUploadState>
}