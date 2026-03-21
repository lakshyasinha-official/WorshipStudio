package com.example.worshipstudio.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Membership(
    val userId: String      = "",
    val email: String       = "",
    val displayName: String = "",
    val churchId: String    = "",
    val role: String        = "member"
)
