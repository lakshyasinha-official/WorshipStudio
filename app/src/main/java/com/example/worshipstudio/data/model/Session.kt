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
    val activeChordDegree:  String  = "",
    /** Set when this is a push session with a single song (no set). Empty = regular session. */
    val pushSongId:         String  = "",
    /** Key selected by admin during live session. Empty = use song's rootKey. */
    val adminKey:           String  = "",
    /** Quality selected by admin during live session. Empty = use song's keyQuality. */
    val adminQuality:       String  = ""
)
