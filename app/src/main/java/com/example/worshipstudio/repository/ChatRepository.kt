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

    fun sendMessage(churchId: String, message: ChatMessage) {
        val ref = db.getReference("chat/$churchId/messages").push()
        ref.setValue(message.copy(id = ref.key ?: ""))
    }

    // Returns the last 100 messages ordered by timestamp
    fun observeMessages(churchId: String): Flow<List<ChatMessage>> = callbackFlow {
        val ref = db.getReference("chat/$churchId/messages")
            .orderByChild("timestamp")
            .limitToLast(100)

        val listener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val messages = snap.children
                    .mapNotNull { it.getValue(ChatMessage::class.java) }
                trySend(messages)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
