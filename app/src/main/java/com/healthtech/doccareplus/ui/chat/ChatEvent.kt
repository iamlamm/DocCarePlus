package com.healthtech.doccareplus.ui.chat

// Các sự kiện từ người dùng
sealed class ChatEvent {
    data class StartChat(val userId: String) : ChatEvent()
    data class CreateGroup(val groupName: String, val groupId: String, val userIds: List<String>) :
        ChatEvent()

    data class JoinGroup(val groupId: String) : ChatEvent()
    object ResetState : ChatEvent()
}