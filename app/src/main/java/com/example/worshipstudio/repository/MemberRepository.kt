package com.example.worshipstudio.repository

import com.example.worshipstudio.data.model.Membership
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MemberRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val col get() = firestore.collection("memberships")

    suspend fun getMembersForChurch(churchId: String): List<Membership> {
        return try {
            col.whereEqualTo("churchId", churchId)
                .get().await()
                .documents.mapNotNull { it.toObject(Membership::class.java) }
        } catch (e: Exception) { emptyList() }
    }

    // ── Promote / demote role ─────────────────────────────────────────────────
    suspend fun updateRole(userId: String, churchId: String, newRole: String): Result<Unit> {
        return try {
            col.document("${userId}_${churchId}")
                .update("role", newRole).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Remove a member (delete membership doc) ───────────────────────────────
    suspend fun removeMember(userId: String, churchId: String): Result<Unit> {
        return try {
            col.document("${userId}_${churchId}").delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Remove a pending member (by pending doc ID) ───────────────────────────
    suspend fun removePendingMember(docId: String): Result<Unit> {
        return try {
            col.document(docId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Pre-add a member (admin adds by email before they register) ───────────
    // Creates a pending membership. When the user joins via "Join Church"
    // with this email, the pending doc is converted to a full membership.
    suspend fun addPendingMember(
        email:       String,
        displayName: String,
        churchId:    String
    ): Result<Unit> {
        // Normalise at repo boundary
        val church     = churchId.trim().lowercase()
        val emailClean = email.trim().lowercase()
        return try {
            // Uniqueness: email + church must not already exist (active or pending)
            val existing = col
                .whereEqualTo("churchId", church)
                .whereEqualTo("email", emailClean)
                .limit(1).get().await()
            if (!existing.isEmpty) {
                val isPending = existing.documents.first().getBoolean("isPending") ?: false
                return Result.failure(Exception(
                    if (isPending) "\"$emailClean\" is already pre-added and awaiting registration."
                    else           "\"$emailClean\" is already an active member of this church."
                ))
            }

            val docId = "pending_${church}_${emailClean.replace(".", "_").replace("@", "_at_")}"
            col.document(docId).set(
                mapOf(
                    "userId"      to "",
                    "email"       to emailClean,
                    "displayName" to displayName.trim(),
                    "churchId"    to church,
                    "role"        to "member",
                    "isPending"   to true
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Find pending membership by email + church (used during Join Church) ───
    suspend fun findPendingByEmail(email: String, churchId: String): Pair<String, Membership>? {
        val church     = churchId.trim().lowercase()
        val emailClean = email.trim().lowercase()
        return try {
            val snap = col
                .whereEqualTo("churchId", church)
                .whereEqualTo("email", emailClean)
                .whereEqualTo("isPending", true)
                .get().await()
            val doc = snap.documents.firstOrNull() ?: return null
            val membership = doc.toObject(Membership::class.java) ?: return null
            doc.id to membership
        } catch (e: Exception) { null }
    }
}
