package com.example.worshipstudio.repository

import com.example.worshipstudio.data.model.Tag
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TagRepository {
    private val db  = FirebaseFirestore.getInstance()
    private val col = db.collection("tags")

    suspend fun getTags(churchId: String): List<Tag> {
        return try {
            col.whereEqualTo("churchId", churchId)
                .get().await()
                .toObjects(Tag::class.java)
                .sortedBy { it.name.lowercase() }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun addTag(name: String, churchId: String): Result<Tag> {
        val normalised = name.trim().lowercase()
        return try {
            // Duplicate check — same name (case-insensitive) + same church
            val existing = col
                .whereEqualTo("churchId", churchId)
                .whereEqualTo("name", normalised)
                .limit(1).get().await()
            if (!existing.isEmpty) {
                return Result.failure(Exception("Tag \"$normalised\" already exists."))
            }
            val id  = UUID.randomUUID().toString()
            val tag = Tag(id = id, name = normalised, churchId = churchId)
            col.document(id).set(tag).await()
            Result.success(tag)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteTag(tagId: String): Result<Unit> {
        return try {
            col.document(tagId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }
}
