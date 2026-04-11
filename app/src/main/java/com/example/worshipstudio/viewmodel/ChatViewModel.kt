package com.example.worshipstudio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.worshipstudio.data.model.ChatMessage
import com.example.worshipstudio.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatState(
    val messages:    List<ChatMessage> = emptyList(),
    val unreadCount: Int               = 0
)

class ChatViewModel : ViewModel() {
    private val repo = ChatRepository()

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state

    private var lastReadTimestamp = 0L
    private var currentUserId    = ""
    private var observing        = false

    fun init(churchId: String, userId: String) {
        if (observing || churchId.isEmpty()) return
        observing     = true
        currentUserId = userId
        // Delete messages older than 24h from Firebase on start
        repo.pruneOldMessages(churchId)
        viewModelScope.launch {
            repo.observeMessages(churchId).collect { messages ->
                val unread = messages.count {
                    it.timestamp > lastReadTimestamp && it.senderId != currentUserId
                }
                _state.value = ChatState(messages = messages, unreadCount = unread)
            }
        }
    }

    fun markAllRead() {
        lastReadTimestamp = System.currentTimeMillis()
        _state.value = _state.value.copy(unreadCount = 0)
    }

    fun sendMessage(text: String, churchId: String, senderId: String, senderName: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        repo.sendMessage(
            churchId,
            ChatMessage(
                senderId   = senderId,
                senderName = senderName,
                text       = trimmed,
                timestamp  = System.currentTimeMillis()
            )
        )
    }
}
