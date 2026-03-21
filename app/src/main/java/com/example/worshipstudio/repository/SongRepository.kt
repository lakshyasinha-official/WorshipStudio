package com.example.worshipstudio.repository

import com.example.worshipstudio.data.model.Song
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SongRepository {
    private val db = FirebaseFirestore.getInstance()
    private val songs = db.collection("songs")

    suspend fun getSongs(churchId: String): List<Song> {
        return try {
            songs.whereEqualTo("churchId", churchId)
                .get().await()
                .toObjects(Song::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchSongs(churchId: String, query: String): List<Song> {
        return try {
            val q = query.lowercase()
            songs.whereEqualTo("churchId", churchId)
                .whereGreaterThanOrEqualTo("nameLowercase", q)
                .whereLessThanOrEqualTo("nameLowercase", q + "\uF8FF")
                .get().await()
                .toObjects(Song::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addSong(song: Song): Result<String> {
        return try {
            val id = UUID.randomUUID().toString()
            val toSave = song.copy(
                id = id,
                nameLowercase = song.name.lowercase(),
                createdAt = System.currentTimeMillis()
            )
            songs.document(id).set(toSave).await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSong(song: Song): Result<Unit> {
        return try {
            songs.document(song.id).set(song.copy(nameLowercase = song.name.lowercase())).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSong(songId: String): Result<Unit> {
        return try {
            songs.document(songId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSong(songId: String): Song? {
        return try {
            songs.document(songId).get().await().toObject(Song::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
