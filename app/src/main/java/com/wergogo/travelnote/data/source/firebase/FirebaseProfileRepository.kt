package com.wergogo.travelnote.data.source.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.wergogo.travelnote.data.model.UserProfile
import com.wergogo.travelnote.data.repository.ProfileRepository
import com.wergogo.travelnote.util.generateDateKeys
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// The security rules look up the trip of every document a batch writes, and one batch may look up
// at most 20 documents, so profile changes are committed for at most this many trips at a time.
private const val MAX_TRIPS_PER_BATCH = 10
private const val MAX_WRITES_PER_BATCH = 500

private val ENTRY_KINDS = listOf("events", "expenses", "notes")

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

    override suspend fun updateNickname(nickname: String): Result<Unit> =
        updateOwnProfile(mapOf("nickname" to nickname))

    override suspend fun updateAvatar(avatarId: String): Result<Unit> =
        updateOwnProfile(mapOf("avatarId" to avatarId))

    /** Merge rather than update, so a profile document that was never written gets created. */
    private suspend fun updateOwnProfile(fields: Map<String, Any>): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            firestore.collection("users").document(uid).set(fields, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Runs to completion even if the screen that started it closes, so names and avatars don't end
    // up half-updated.
    override suspend fun propagateProfileUpdate(): Result<Unit> = withContext(NonCancellable) {
        try {
            val uid = auth.currentUser?.uid
                ?: return@withContext Result.failure(Exception("Not logged in"))
            val profileDoc = firestore.collection("users").document(uid).get().await()
            val nickname = profileDoc.getString("nickname") ?: ""
            val avatarId = profileDoc.getString("avatarId") ?: "avatar_01"

            val memberFields = mapOf<String, Any>("nickname" to nickname, "avatarId" to avatarId)
            val authorFields = mapOf<String, Any>("authorNickname" to nickname, "authorAvatarId" to avatarId)

            // Collect the documents that need updating, grouped by trip
            val updatesByTrip = mutableListOf<List<Pair<DocumentReference, Map<String, Any>>>>()

            val tripsSnap = firestore.collection("trips")
                .whereArrayContains("memberIds", uid)
                .get().await()

            for (tripDoc in tripsSnap.documents) {
                val updates = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()
                updatesByTrip.add(updates)

                // Member record (trips joined with an old app version may lack one).
                val memberRef = tripDoc.reference.collection("members").document(uid)
                if (memberRef.get().await().exists()) updates.add(memberRef to memberFields)

                // Entries this user paid for or wrote, on every day of the trip.
                authoredEntries(tripDoc, uid).forEach { updates.add(it.reference to authorFields) }
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Entries live under trips/{id}/days/{date}/{events|expenses|notes}, but the day documents
     * themselves are never created, so a query on "days" finds nothing. Walk the trip's dates
     * instead, querying all of them in parallel.
     */
    private suspend fun authoredEntries(tripDoc: DocumentSnapshot, uid: String): List<DocumentSnapshot> {
        val dateKeys = generateDateKeys(tripDoc.getString("startDate") ?: "", tripDoc.getString("endDate") ?: "")
        return coroutineScope {
            dateKeys.flatMap { dateKey ->
                val dayRef = tripDoc.reference.collection("days").document(dateKey)
                ENTRY_KINDS.map { kind ->
                    async { dayRef.collection(kind).whereEqualTo("authorId", uid).get().await().documents }
                }
            }.awaitAll().flatten()
        }
    }
}
