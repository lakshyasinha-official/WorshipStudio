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
    private val db       = FirebaseDatabase.getInstance()
    private val sessions = db.getReference("sessions")

    // ── Create session ────────────────────────────────────────────────────────
    suspend fun createSession(setId: String, adminId: String): Result<String> {
        return try {
            val sessionId = UUID.randomUUID().toString().take(8).uppercase()
            val session   = Session(
                sessionId        = sessionId,
                currentSongIndex = 0,
                setId            = setId,
                adminId          = adminId
            )
            sessions.child(sessionId).setValue(session).await()
            Result.success(sessionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Observe session data (song index changes) ─────────────────────────────
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

    // ── Presence: register this device and auto-remove on disconnect ──────────
    // Returns the unique participant key so the caller can remove it manually too.
    fun registerPresence(sessionId: String, label: String): String {
        val participantKey = UUID.randomUUID().toString().take(8)
        val ref = sessions
            .child(sessionId)
            .child("participants")
            .child(participantKey)
        ref.setValue(label)
        // Firebase will delete this node automatically if the connection drops
        ref.onDisconnect().removeValue()
        return participantKey
    }

    fun removePresence(sessionId: String, participantKey: String) {
        sessions.child(sessionId).child("participants").child(participantKey).removeValue()
    }

    // ── Observe live participant count ────────────────────────────────────────
    fun observeParticipantCount(sessionId: String): Flow<Int> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.childrenCount.toInt())
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        sessions.child(sessionId).child("participants")
            .addValueEventListener(listener)
        awaitClose {
            sessions.child(sessionId).child("participants")
                .removeEventListener(listener)
        }
    }

    // ── Update song index (admin only) ────────────────────────────────────────
    suspend fun updateSongIndex(sessionId: String, index: Int): Result<Unit> {
        return try {
            sessions.child(sessionId).child("currentSongIndex").setValue(index).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── End session (deletes the node) ────────────────────────────────────────
    suspend fun endSession(sessionId: String): Result<Unit> {
        return try {
            sessions.child(sessionId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
