package com.example.worshipstudio.data.model

import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.IgnoreExtraProperties

@Immutable
@IgnoreExtraProperties
data class Membership(
    val userId:      String  = "",
    val email:       String  = "",
    val displayName: String  = "",
    val churchId:    String  = "",
    val role:        String  = "member",
    val isPending:   Boolean = false   // true = admin pre-added, awaiting user to register
)
