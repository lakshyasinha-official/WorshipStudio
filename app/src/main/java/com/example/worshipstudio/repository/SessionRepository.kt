package com.example.worshipstudio.repository

import com.example.worshipstudio.data.model.Session
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SessionRepository {
    private val db = FirebaseDatabase.getInstance()
    private val sessions = db.getReference("sessions")

    suspend fun createSession(setId: String, adminId: String): Result<String> {
        return try {
            val sessionId = UUID.randomUUID().toString().take(8).uppercase()
            val session = Session(
                sessionId = sessionId,
                currentSongIndex = 0,
                setId = setId,
                adminId = adminId
            )
            sessions.child(sessionId).setValue(session).await()
            Result.success(sessionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeSession(sessionId: String): Flow<Session?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Session::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        sessions.child(sessionId).addValueEventListener(listener)
        awaitClose { sessions.child(sessionId).removeEventListener(listener) }
    }

    suspend fun updateSongIndex(sessionId: String, index: Int): Result<Unit> {
        return try {
            sessions.child(sessionId).child("currentSongIndex").setValue(index).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun endSession(sessionId: String): Result<Unit> {
        return try {
            sessions.child(sessionId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
