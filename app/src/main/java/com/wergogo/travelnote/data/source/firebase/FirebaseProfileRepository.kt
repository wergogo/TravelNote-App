package com.wergogo.travelnote.data.source.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.wergogo.travelnote.data.model.UserProfile
import com.wergogo.travelnote.data.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// The security rules look up the trip of every document a batch writes, and one batch may look up
// at most 20 documents, so profile changes are committed for at most this many trips at a time.
private const val MAX_TRIPS_PER_BATCH = 10
private const val MAX_WRITES_PER_BATCH = 500

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

            // Collect the documents that need updating, grouped by trip
            val updatesByTrip = mutableListOf<List<Pair<DocumentReference, Map<String, Any>>>>()

            // 1. Find all trips this user belongs to
            val tripsSnap = firestore.collection("trips")
                .whereArrayContains("memberIds", uid)
                .get().await()

            for (tripDoc in tripsSnap.documents) {
                val updates = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()
                updatesByTrip.add(updates)

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

            // Batch commit: at most MAX_TRIPS_PER_BATCH trips and MAX_WRITES_PER_BATCH writes per batch
            updatesByTrip.chunked(MAX_TRIPS_PER_BATCH).forEach { tripGroup ->
                tripGroup.flatten().chunked(MAX_WRITES_PER_BATCH).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { (ref, data) -> batch.update(ref, data) }
                    batch.commit().await()
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
