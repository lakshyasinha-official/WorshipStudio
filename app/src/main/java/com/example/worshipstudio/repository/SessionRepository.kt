package com.example.worshipstudio.repository

import com.example.worshipstudio.data.model.ChurchPush
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
    private val db           = FirebaseDatabase.getInstance()
    private val sessions     = db.getReference("sessions")
    private val roomCodes    = db.getReference("roomCodes")   // roomCode → sessionId index
    private val churchPushes = db.getReference("churchPush")  // churchId → active push

    // ── Create session (returns sessionId + 4-digit roomCode) ─────────────────
    suspend fun createSession(
        setId:    String,
        adminId:  String,
        churchId: String
    ): Result<Pair<String, String>> = try {
        val sessionId = UUID.randomUUID().toString().take(8).uppercase()
        val roomCode  = (1000..9999).random().toString()
        val session   = Session(
            sessionId        = sessionId,
            roomCode         = roomCode,
            setId            = setId,
            adminId          = adminId,
            churchId         = churchId,
            currentSongIndex = 0,
            isActive         = true
        )
        sessions.child(sessionId).setValue(session).await()
        // Write reverse index: roomCode → sessionId
        roomCodes.child(roomCode).setValue(sessionId).await()
        Result.success(Pair(sessionId, roomCode))
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Look up sessionId by 4-digit room code ────────────────────────────────
    suspend fun findByRoomCode(code: String): Result<String> = try {
        val snap = roomCodes.child(code.trim()).get().await()
        val id   = snap.getValue(String::class.java)
            ?: return Result.failure(Exception("Room code \"$code\" not found or session has ended."))
        Result.success(id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Observe active session for a church (auto-discovery) ──────────────────
    fun observeChurchSession(churchId: String): Flow<Session?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val active = snap.children
                    .mapNotNull { it.getValue(Session::class.java) }
                    .firstOrNull { it.churchId == churchId && it.isActive }
                trySend(active)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        sessions.addValueEventListener(listener)
        awaitClose { sessions.removeEventListener(listener) }
    }

    // ── Observe a specific session ─────────────────────────────────────────────
    fun observeSession(sessionId: String): Flow<Session?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                trySend(snap.getValue(Session::class.java))
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        sessions.child(sessionId).addValueEventListener(listener)
        awaitClose { sessions.child(sessionId).removeEventListener(listener) }
    }

    // ── Presence ──────────────────────────────────────────────────────────────
    fun registerPresence(sessionId: String, label: String): String {
        val key = UUID.randomUUID().toString().take(8)
        val ref = sessions.child(sessionId).child("participants").child(key)
        ref.setValue(label)
        ref.onDisconnect().removeValue()
        return key
    }

    fun removePresence(sessionId: String, key: String) {
        sessions.child(sessionId).child("participants").child(key).removeValue()
    }

    fun observeParticipantCount(sessionId: String): Flow<Int> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) { trySend(snap.childrenCount.toInt()) }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        sessions.child(sessionId).child("participants").addValueEventListener(listener)
        awaitClose { sessions.child(sessionId).child("participants").removeEventListener(listener) }
    }

    // ── Update active chord (admin callout) ───────────────────────────────────
    fun updateActiveChord(sessionId: String, degree: String) {
        sessions.child(sessionId).child("activeChordDegree").setValue(degree)
    }

    // ── Update current song index (admin) ─────────────────────────────────────
    suspend fun updateSongIndex(sessionId: String, index: Int): Result<Unit> = try {
        sessions.child(sessionId).child("currentSongIndex").setValue(index).await()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }

    // ── Create push session (single song, no set) ─────────────────────────────
    suspend fun createPushSession(
        songId:    String,
        songName:  String,
        adminId:   String,
        adminName: String,
        churchId:  String
    ): Result<String> = try {
        val sessionId = UUID.randomUUID().toString().take(8).uppercase()
        val session = Session(
            sessionId        = sessionId,
            roomCode         = "",
            setId            = "",
            adminId          = adminId,
            churchId         = churchId,
            currentSongIndex = 0,
            isActive         = true,
            pushSongId       = songId
        )
        sessions.child(sessionId).setValue(session).await()
        // Broadcast to all church members
        val push = ChurchPush(
            sessionId = sessionId,
            songName  = songName,
            adminName = adminName,
            timestamp = System.currentTimeMillis()
        )
        churchPushes.child(churchId).setValue(push).await()
        Result.success(sessionId)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Observe church push (members watch this for instant join pop-up) ──────
    fun observeChurchPush(churchId: String): Flow<ChurchPush?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                trySend(snap.getValue(ChurchPush::class.java))
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        churchPushes.child(churchId).addValueEventListener(listener)
        awaitClose { churchPushes.child(churchId).removeEventListener(listener) }
    }

    // ── Clear church push (admin clears when session ends) ────────────────────
    fun clearChurchPush(churchId: String) {
        churchPushes.child(churchId).removeValue()
    }

    // ── End session — removes node + cleans up roomCode index ─────────────────
    suspend fun endSession(sessionId: String, roomCode: String): Result<Unit> = try {
        sessions.child(sessionId).removeValue().await()
        if (roomCode.isNotEmpty()) roomCodes.child(roomCode).removeValue()
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(e) }
}
