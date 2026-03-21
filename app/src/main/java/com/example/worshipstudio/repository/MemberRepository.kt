package com.example.worshipstudio.repository

import com.example.worshipstudio.data.model.Membership
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MemberRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getMembersForChurch(churchId: String): List<Membership> {
        return try {
            firestore.collection("memberships")
                .whereEqualTo("churchId", churchId)
                .get().await()
                .documents.mapNotNull { doc ->
                    doc.toObject(Membership::class.java)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
