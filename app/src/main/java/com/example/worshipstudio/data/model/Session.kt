package com.example.worshipstudio.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Session(
    val sessionId:        String  = "",
    val roomCode:         String  = "",   // 4-digit human code e.g. "7342"
    val setId:            String  = "",
    val adminId:          String  = "",
    val churchId:         String  = "",
    val currentSongIndex:   Int     = 0,
    val isActive:           Boolean = true,
    /** Degree currently called out by admin (e.g. "IV", "V"). Empty = none. */
    val activeChordDegree:  String  = ""
)
