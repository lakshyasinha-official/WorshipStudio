package com.example.worshipstudio.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ChatMessage(
    val id:         String = "",
    val senderId:   String = "",
    val senderName: String = "",
    val text:       String = "",
    val timestamp:  Long   = 0L
)
