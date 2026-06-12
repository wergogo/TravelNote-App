package com.wergogo.travelnote.data.source.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.wergogo.travelnote.data.model.UserProfile
import com.wergogo.travelnote.data.repository.ProfileRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseProfileRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ProfileRepository {

    override fun observeProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val registration = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(); return@addSnapshotListener }
                val profile = snapshot?.takeIf { it.exists() }?.let { doc ->
                    UserProfile(
                        id = doc.getString("id") ?: userId,
                        email = doc.getString("email") ?: "",
                        nickname = doc.getString("nickname") ?: "",
                        avatarId = doc.getString("avatarId") ?: "avatar_01",
                        createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    )
                }
                trySend(profile)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun updateNickname(nickname: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            firestore.collection("users").document(uid)
                .update("nickname", nickname)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAvatar(avatarId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            firestore.collection("users").document(uid)
                .update("avatarId", avatarId)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun propagateProfileUpdate(): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val profileDoc = firestore.collection("users").document(uid).get().await()
            val nickname = profileDoc.getString("nickname") ?: ""
            val avatarId = profileDoc.getString("avatarId") ?: "avatar_01"

            val memberFields = mapOf<String, Any>("nickname" to nickname, "avatarId" to avatarId)
            val authorFields = mapOf<String, Any>("authorNickname" to nickname, "authorAvatarId" to avatarId)

            // Collect all document references that need updating
            val updates = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()

            // 1. Find all trips this user belongs to
            val tripsSnap = firestore.collection("trips")
                .whereArrayContains("memberIds", uid)
                .get().await()

            for (tripDoc in tripsSnap.documents) {
                // Update member doc
                val memberRef = tripDoc.reference.collection("members").document(uid)
                updates.add(memberRef to memberFields)

                // Iterate each day in the trip
                val daysSnap = tripDoc.reference.collection("days").get().await()
                for (dayDoc in daysSnap.documents) {
                    val dayRef = dayDoc.reference

                    // Events authored by this user
                    val events = dayRef.collection("events")
                        .whereEqualTo("authorId", uid).get().await()
                    for (e in events.documents) {
                        updates.add(e.reference to authorFields)
                    }

                    // Expenses authored by this user
                    val expenses = dayRef.collection("expenses")
                        .whereEqualTo("authorId", uid).get().await()
                    for (e in expenses.documents) {
                        updates.add(e.reference to authorFields)
                    }

                    // Notes authored by this user
                    val notes = dayRef.collection("notes")
                        .whereEqualTo("authorId", uid).get().await()
                    for (e in notes.documents) {
                        updates.add(e.reference to authorFields)
                    }
                }
            }

            // Batch commit (Firestore limit: 500 ops per batch)
            updates.chunked(500).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { (ref, data) -> batch.update(ref, data) }
                batch.commit().await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
