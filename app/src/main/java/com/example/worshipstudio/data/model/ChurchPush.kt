package com.example.worshipstudio.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ChurchPush(
    val sessionId: String = "",
    val songName:  String = "",
    val adminName: String = "",
    val timestamp: Long   = 0L
)
