package com.healthtech.doccareplus.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplus.domain.repository.UserRepository
import com.zegocloud.zimkit.common.enums.ZIMKitConversationType
import com.zegocloud.zimkit.services.ZIMKit
import dagger.hilt.android.lifecycle.HiltViewModel
import im.zego.zim.enums.ZIMErrorCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun handleEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.StartChat -> startChat(event.userId)
            is ChatEvent.CreateGroup -> createGroup(event.groupName, event.groupId, event.userIds)
            is ChatEvent.JoinGroup -> joinGroup(event.groupId)
            is ChatEvent.ResetState -> _uiState.value = ChatUiState.Idle
        }
    }

    private fun startChat(userId: String) {
        _uiState.value = ChatUiState.Loading
        
        // Kiểm tra userId hợp lệ
        if (userId.isBlank()) {
            _uiState.value = ChatUiState.Error("ID người dùng không được để trống")
            return
        }
        
        // Log để kiểm tra
        Log.d("ChatViewModel", "Starting chat with user ID: $userId")
        
        viewModelScope.launch {
            try {
                // Đảm bảo userId là string, không phải int
                val conversationId = userId.trim()
                
                // Kiểm tra định dạng đúng cho ZIMKit
                if (!conversationId.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
                    Log.w("ChatViewModel", "User ID format may not be compatible: $conversationId")
                }
                
                _uiState.value = ChatUiState.ChatStarted(conversationId)
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error("Không thể bắt đầu trò chuyện: ${e.message}")
            }
        }
    }

    private fun createGroup(groupName: String, groupId: String, userIds: List<String>) {
        _uiState.value = ChatUiState.Loading
        
        if (groupName.isBlank() || groupId.isBlank()) {
            _uiState.value = ChatUiState.Error("Tên nhóm và ID nhóm không được để trống")
            return
        }
        
        ZIMKit.createGroup(groupName, groupId, userIds) { groupInfo, inviteUserErrors, error ->
            _uiState.value = if (error.code == ZIMErrorCode.SUCCESS) {
                ChatUiState.GroupCreated(groupInfo.id)
            } else {
                ChatUiState.Error("Lỗi tạo nhóm: ${error.message}")
            }
        }
    }

    private fun joinGroup(groupId: String) {
        _uiState.value = ChatUiState.Loading

        if (groupId.isBlank()) {
            _uiState.value = ChatUiState.Error("ID nhóm không được để trống")
            return
        }
        
        ZIMKit.joinGroup(groupId) { groupInfo, error ->
            _uiState.value = if (error.code == ZIMErrorCode.SUCCESS || 
                               error.code == ZIMErrorCode.MEMBER_IS_ALREADY_IN_THE_GROUP) {
                ChatUiState.GroupJoined(groupId)
            } else {
                ChatUiState.Error("Lỗi tham gia nhóm: ${error.message}")
            }
        }
    }
}