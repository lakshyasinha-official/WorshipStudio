package com.example.worshipstudio.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth      = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    // ── Login ─────────────────────────────────────────────────────────────────
    // Requires all three: email + password + churchId.
    // Fails if the user is not a member of that church.
    suspend fun login(
        email: String,
        password: String,
        churchId: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user   = result.user!!

            // Verify membership exists for this church
            val membershipId = "${user.uid}_${churchId}"
            val doc = firestore.collection("memberships")
                .document(membershipId).get().await()

            if (!doc.exists()) {
                auth.signOut()
                Result.failure(Exception("You are not registered with church \"$churchId\""))
            } else {
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Register ──────────────────────────────────────────────────────────────
    // Creates Firebase Auth account (or reuses existing one if email already
    // exists) and writes a membership doc for this email + churchId combination.
    suspend fun register(
        email: String,
        password: String,
        churchId: String,
        role: String = "member"
    ): Result<FirebaseUser> {
        return try {
            val user: FirebaseUser = try {
                // Happy path: brand new account
                auth.createUserWithEmailAndPassword(email, password).await().user!!
            } catch (collision: FirebaseAuthUserCollisionException) {
                // Email already exists in Firebase Auth → sign in and add new membership
                auth.signInWithEmailAndPassword(email, password).await().user!!
            }

            val displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
            val membershipId = "${user.uid}_${churchId}"

            // Check if this membership already exists
            val existing = firestore.collection("memberships")
                .document(membershipId).get().await()
            if (existing.exists()) {
                auth.signOut()
                return Result.failure(Exception("Already registered with church \"$churchId\""))
            }

            firestore.collection("memberships").document(membershipId).set(
                mapOf(
                    "userId"      to user.uid,
                    "email"       to email,
                    "displayName" to displayName,
                    "churchId"    to churchId,
                    "role"        to role
                )
            ).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Fetch membership data for current user + church ───────────────────────
    suspend fun getMembership(churchId: String): Map<String, Any>? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            firestore.collection("memberships")
                .document("${uid}_${churchId}").get().await().data
        } catch (e: Exception) {
            null
        }
    }

    // ── Fetch all memberships for a church (admin use) ────────────────────────
    suspend fun getMembersForChurch(churchId: String): List<Map<String, Any>> {
        return try {
            firestore.collection("memberships")
                .whereEqualTo("churchId", churchId)
                .get().await()
                .documents.mapNotNull { it.data }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun logout() = auth.signOut()
}
