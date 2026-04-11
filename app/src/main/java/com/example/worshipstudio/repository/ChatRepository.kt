package com.example.worshipstudio.repository

import com.example.worshipstudio.data.model.ChatMessage
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ChatRepository {
    private val db = FirebaseDatabase.getInstance()

    private fun cutoff() = System.currentTimeMillis() - 24 * 60 * 60 * 1000L

    fun sendMessage(churchId: String, message: ChatMessage) {
        val ref = db.getReference("chat/$churchId/messages").push()
        ref.setValue(message.copy(id = ref.key ?: ""))
    }

    /** Delete all messages older than 24 hours from Firebase (best-effort). */
    fun pruneOldMessages(churchId: String) {
        db.getReference("chat/$churchId/messages")
            .orderByChild("timestamp")
            .endBefore(cutoff().toDouble())
            .get()
            .addOnSuccessListener { snap ->
                snap.children.forEach { it.ref.removeValue() }
            }
    }

    // Returns the last 100 messages ordered by timestamp, filtering out anything >24h old
    fun observeMessages(churchId: String): Flow<List<ChatMessage>> = callbackFlow {
        val ref = db.getReference("chat/$churchId/messages")
            .orderByChild("timestamp")
            .limitToLast(100)

        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val threshold = cutoff()
                val messages = snap.children
                    .mapNotNull { it.getValue(ChatMessage::class.java) }
                    .filter { it.timestamp >= threshold }
                trySend(messages)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
