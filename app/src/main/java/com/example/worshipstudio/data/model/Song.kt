package com.example.worshipstudio.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Song(
    val id: String = "",
    val name: String = "",
    /** Legacy single-block lyrics (kept for backward-compat; empty on new songs). */
    val lyrics: String = "",
    /** Structured song parts — the primary storage for new songs. */
    val parts: List<SongPart> = emptyList(),
    val rootKey: String = "C",
    val keyQuality: String = "Major",   // "Major" | "Minor" | "Diminished"
    val createdBy: String = "",
    val churchId: String = "",
    val createdAt: Long = 0L,
    val nameLowercase: String = ""
) {
    /** Combined lyrics for display / search when using legacy format. */
    val combinedLyrics: String get() =
        if (parts.isNotEmpty()) parts.joinToString("\n\n") { it.lyrics }
        else lyrics
}
