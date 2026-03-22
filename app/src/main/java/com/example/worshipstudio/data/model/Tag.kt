package com.example.worshipstudio.data.model

import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.IgnoreExtraProperties

@Immutable
@IgnoreExtraProperties
data class Tag(
    val id:       String = "",
    val name:     String = "",
    val churchId: String = ""
)
