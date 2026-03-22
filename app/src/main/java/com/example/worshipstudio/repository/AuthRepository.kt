package com.example.worshipstudio.repository

import com.example.worshipstudio.repository.MemberRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth      = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    // ── Login ─────────────────────────────────────────────────────────────────
    suspend fun login(email: String, password: String, churchId: String): Result<FirebaseUser> {
        val church = churchId.trim().lowercase()
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val user   = result.user!!

            val membershipId = "${user.uid}_${church}"
            val doc = firestore.collection("memberships").document(membershipId).get().await()

            if (!doc.exists()) {
                auth.signOut()
                Result.failure(Exception("You are not registered with church \"$church\""))
            } else {
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Register as Member (church must already exist) ────────────────────────
    suspend fun registerAsMember(
        email:       String,
        password:    String,
        churchId:    String,
        displayName: String
    ): Result<FirebaseUser> {
        // Always enforce lowercase at the repo boundary
        val church = churchId.trim().lowercase()
        val emailClean = email.trim()

        return try {
            // Church existence check:
            // 1st — dedicated churches collection (new registrations)
            // 2nd — fall back to memberships (churches created before churches collection existed)
            val churchExists = run {
                val churchDoc = firestore.collection("churches").document(church).get().await()
                if (churchDoc.exists()) return@run true
                // Fallback: at least one membership exists for this churchId
                val snap = firestore.collection("memberships")
                    .whereEqualTo("churchId", church)
                    .limit(1).get().await()
                if (!snap.isEmpty) {
                    // Auto-create the missing churches doc so future lookups work
                    val adminSnap = firestore.collection("memberships")
                        .whereEqualTo("churchId", church)
                        .whereEqualTo("role", "admin")
                        .limit(1).get().await()
                    val createdBy = adminSnap.documents.firstOrNull()
                        ?.getString("userId") ?: ""
                    firestore.collection("churches").document(church).set(
                        mapOf("id" to church, "createdBy" to createdBy,
                              "createdAt" to System.currentTimeMillis())
                    ).await()
                    true
                } else false
            }

            if (!churchExists) {
                return Result.failure(Exception("No church found with name \"$church\". Ask your admin for the exact name."))
            }

            // ── Uniqueness check: email + church must not already exist ────────
            val emailChurchSnap = firestore.collection("memberships")
                .whereEqualTo("churchId", church)
                .whereEqualTo("email", emailClean)
                .limit(1).get().await()

            if (!emailChurchSnap.isEmpty) {
                val doc       = emailChurchSnap.documents.first()
                val isPending = doc.getBoolean("isPending") ?: false
                return if (isPending) {
                    Result.failure(Exception("\"$emailClean\" has been pre-added to \"$church\" by the admin. You can sign in directly once the admin activates your account, or contact them."))
                } else {
                    Result.failure(Exception("\"$emailClean\" is already registered with church \"$church\". Use Sign In instead."))
                }
            }

            val user: FirebaseUser = try {
                auth.createUserWithEmailAndPassword(emailClean, password).await().user!!
            } catch (collision: FirebaseAuthUserCollisionException) {
                auth.signInWithEmailAndPassword(emailClean, password).await().user!!
            }

            val membershipId = "${user.uid}_${church}"
            val existing = firestore.collection("memberships").document(membershipId).get().await()
            if (existing.exists() && existing.getBoolean("isPending") != true) {
                // Already a full active member
                return Result.success(user)
            }

            // Check if admin pre-added them (pending membership by email)
            val memberRepo = MemberRepository()
            val pending = memberRepo.findPendingByEmail(emailClean, church)
            val resolvedName = displayName.ifBlank {
                emailClean.substringBefore("@").replaceFirstChar { it.uppercase() }
            }

            if (pending != null) {
                val (pendingDocId, _) = pending
                firestore.collection("memberships").document(pendingDocId).delete().await()
                firestore.collection("memberships").document(membershipId).set(
                    mapOf("userId" to user.uid, "email" to emailClean,
                          "displayName" to resolvedName, "churchId" to church,
                          "role" to "member", "isPending" to false)
                ).await()
            } else {
                firestore.collection("memberships").document(membershipId).set(
                    mapOf("userId" to user.uid, "email" to emailClean,
                          "displayName" to resolvedName, "churchId" to church,
                          "role" to "member", "isPending" to false)
                ).await()
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Register New Church (church must NOT exist) — caller becomes admin ─────
    suspend fun registerNewChurch(
        email:       String,
        password:    String,
        churchId:    String,
        displayName: String
    ): Result<FirebaseUser> {
        // Enforce lowercase at repo boundary
        val church     = churchId.trim().lowercase()
        val emailClean = email.trim()

        return try {
            // Church must NOT exist in either collection
            val churchDocExists = firestore.collection("churches").document(church).get().await().exists()
            val membershipExists = if (!churchDocExists) {
                !firestore.collection("memberships")
                    .whereEqualTo("churchId", church).limit(1).get().await().isEmpty
            } else false

            if (churchDocExists || membershipExists) {
                return Result.failure(Exception("Church name \"$church\" is already taken. Please choose a different name."))
            }

            val user: FirebaseUser = try {
                auth.createUserWithEmailAndPassword(emailClean, password).await().user!!
            } catch (collision: FirebaseAuthUserCollisionException) {
                auth.signInWithEmailAndPassword(emailClean, password).await().user!!
            }

            firestore.collection("churches").document(church).set(
                mapOf("id" to church, "createdBy" to user.uid, "createdAt" to System.currentTimeMillis())
            ).await()

            val membershipId = "${user.uid}_${church}"
            firestore.collection("memberships").document(membershipId).set(
                mapOf("userId" to user.uid, "email" to emailClean, "displayName" to displayName,
                      "churchId" to church, "role" to "admin", "isPending" to false)
            ).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Legacy register (kept for compatibility) ──────────────────────────────
    suspend fun register(
        email: String, password: String, churchId: String, role: String = "member"
    ): Result<FirebaseUser> = registerAsMember(email, password, churchId,
        email.substringBefore("@").replaceFirstChar { it.uppercase() })

    // ── Fetch membership data ─────────────────────────────────────────────────
    suspend fun getMembership(churchId: String): Map<String, Any>? {
        val uid    = auth.currentUser?.uid ?: return null
        val church = churchId.trim().lowercase()
        return try {
            firestore.collection("memberships")
                .document("${uid}_${church}").get().await().data
        } catch (e: Exception) { null }
    }

    // ── Fetch all memberships for a church ────────────────────────────────────
    suspend fun getMembersForChurch(churchId: String): List<Map<String, Any>> {
        return try {
            firestore.collection("memberships")
                .whereEqualTo("churchId", churchId)
                .get().await()
                .documents.mapNotNull { it.data }
        } catch (e: Exception) { emptyList() }
    }

    // ── Delete entire church — wipes all data ────────────────────────────────
    // Deletes: memberships, songs, worship_sets, churches doc, Realtime DB sessions
    suspend fun deleteChurch(churchId: String): Result<Unit> {
        return try {
            // 1. Delete all memberships for this church
            val memberships = firestore.collection("memberships")
                .whereEqualTo("churchId", churchId)
                .get().await()
            for (doc in memberships.documents) doc.reference.delete().await()

            // 2. Delete all songs for this church
            val songs = firestore.collection("songs")
                .whereEqualTo("churchId", churchId)
                .get().await()
            for (doc in songs.documents) doc.reference.delete().await()

            // 3. Collect set IDs before deleting — needed for session cleanup
            val sets = firestore.collection("worship_sets")
                .whereEqualTo("churchId", churchId)
                .get().await()
            val setIds = sets.documents.map { it.id }.toSet()
            for (doc in sets.documents) doc.reference.delete().await()

            // 4. Delete the church document itself
            firestore.collection("churches").document(churchId).delete().await()

            // 5. Best-effort: delete Realtime DB sessions for this church's sets
            if (setIds.isNotEmpty()) {
                val rtDb = FirebaseDatabase.getInstance().getReference("sessions")
                val snapshot = rtDb.get().await()
                snapshot.children.forEach { sessionSnap ->
                    val sessionSetId = sessionSnap.child("setId").getValue(String::class.java)
                    if (sessionSetId != null && sessionSetId in setIds) {
                        sessionSnap.ref.removeValue().await()
                    }
                }
            }

            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()
}
