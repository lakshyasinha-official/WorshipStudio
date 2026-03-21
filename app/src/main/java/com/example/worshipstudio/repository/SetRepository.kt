package com.example.worshipstudio.repository

import com.example.worshipstudio.data.model.WorshipSet
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SetRepository {
    private val db = FirebaseFirestore.getInstance()
    private val sets = db.collection("worship_sets")

    suspend fun getSets(churchId: String): List<WorshipSet> {
        return try {
            sets.whereEqualTo("churchId", churchId)
                .get().await()
                .toObjects(WorshipSet::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createSet(set: WorshipSet): Result<String> {
        return try {
            val id = UUID.randomUUID().toString()
            sets.document(id).set(set.copy(id = id)).await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSet(set: WorshipSet): Result<Unit> {
        return try {
            sets.document(set.id).set(set).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSet(setId: String): Result<Unit> {
        return try {
            sets.document(setId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSet(setId: String): WorshipSet? {
        return try {
            sets.document(setId).get().await().toObject(WorshipSet::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
