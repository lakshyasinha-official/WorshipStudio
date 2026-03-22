package com.example.worshipstudio.data.model

import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.IgnoreExtraProperties

@Immutable
@IgnoreExtraProperties
data class SongPart(
    val type: String = "",       // "Start" | "Verse" | "Chorus" | "Bridge" | "Other" | "End"
    val number: Int  = 1,        // auto-assigned; ignored for Start & End
    val lyrics: String = "",
    val repeatCount: Int = 1     // how many times this section is sung (default 1)
) {
    /** Human-readable section label shown in the UI. */
    val displayName: String get() = when (type) {
        "Start", "End" -> type
        else           -> "$type $number"
    }
}
