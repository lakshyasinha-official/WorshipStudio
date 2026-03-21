package com.example.worshipstudio.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class WorshipSet(
    val id: String = "",
    val name: String = "",
    val date: String = "",
    val songs: List<String> = emptyList(),
    val createdBy: String = "",
    val churchId: String = ""
)
