package com.healthtech.doccareplus.ui.chat

sealed class ChatUiState {
    // Trạng thái cho các hành động cụ thể
    object Idle : ChatUiState()
    object Loading : ChatUiState()
    object Success : ChatUiState()

    data class Error(val message: String) : ChatUiState()
    data class GroupCreated(val groupId: String) : ChatUiState()
    data class GroupJoined(val groupId: String) : ChatUiState()
    data class ChatStarted(val userId: String) : ChatUiState()
}

