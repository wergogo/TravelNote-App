package com.wergogo.travelnote.data.source.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.wergogo.travelnote.data.model.Member
import com.wergogo.travelnote.data.model.Trip
import com.wergogo.travelnote.data.model.TripDraft
import com.wergogo.travelnote.data.model.TripVisibility
import com.wergogo.travelnote.data.repository.TripRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val JOIN_CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
private const val JOIN_CODE_LENGTH = 6

/**
 * Firestore-backed trips.
 *
 * Join codes live in `joinCodes/{CODE}` → `{ tripId }`, so someone who knows a code can find its
 * trip without being able to read or list other people's trips (see `firebase/firestore.rules`).
 * Changes that touch several documents are committed as one batch: the security rules check them
 * together, and a failure can never leave a half-created or half-joined trip.
 */
class FirebaseTripRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : TripRepository {

    private val tripsRef = firestore.collection("trips")
    private val joinCodesRef = firestore.collection("joinCodes")
    private val deletedTripsRef = firestore.collection("deletedTrips")

    /** User for whom [ensureJoinCodes] has already completed in this process. */
    @Volatile private var joinCodesCheckedFor: String? = null

    // ── Observe ───────────────────────────────────────────────────────────────

    override fun observeMyTrips(): Flow<List<Trip>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val reg = tripsRef
            .whereArrayContains("memberIds", uid)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.toTrip() } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    override fun observeTrip(tripId: String): Flow<Trip?> = callbackFlow {
        val reg = tripsRef.document(tripId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(); return@addSnapshotListener }
                trySend(snap?.takeIf { it.exists() }?.toTrip())
            }
        awaitClose { reg.remove() }
    }

    override fun observeMembers(tripId: String): Flow<List<Member>> = callbackFlow {
        val reg = tripsRef.document(tripId).collection("members")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.toMember() } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    override suspend fun createTrip(draft: TripDraft): Result<Trip> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val profile = firestore.collection("users").document(uid).get().await()
            val nickname = profile.getString("nickname") ?: ""
            val avatarId = profile.getString("avatarId") ?: "avatar_01"

            val joinCode = generateUniqueJoinCode()
            val tripRef = tripsRef.document()
            val tripId = tripRef.id

            val data = mapOf(
                "id" to tripId,
                "name" to draft.name,
                "destination" to draft.destination,
                "startDate" to draft.startDate,
                "endDate" to draft.endDate,
                "totalBudget" to draft.totalBudget,
                "currency" to draft.currency,
                "visibility" to draft.visibility.name.lowercase(),
                "joinCode" to joinCode,
                "coverId" to draft.coverId,
                "ownerId" to uid,
                "memberIds" to listOf(uid),
                "createdAt" to FieldValue.serverTimestamp()
            )

            // Trip, the owner's member record and the join-code lookup are created together.
            firestore.batch()
                .set(tripRef, data)
                .set(tripRef.collection("members").document(uid), memberDoc(uid, nickname, avatarId, role = "owner"))
                .set(joinCodesRef.document(joinCode), mapOf("tripId" to tripId))
                .commit().await()

            val trip = Trip(
                id = tripId,
                name = draft.name,
                destination = draft.destination,
                startDate = draft.startDate,
                endDate = draft.endDate,
                totalBudget = draft.totalBudget,
                currency = draft.currency,
                visibility = draft.visibility,
                joinCode = joinCode,
                coverId = draft.coverId,
                ownerId = uid,
                memberIds = listOf(uid)
            )
            Result.success(trip)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTrip(tripId: String, draft: TripDraft): Result<Unit> = try {
        tripsRef.document(tripId).update(
            mapOf(
                "name" to draft.name,
                "destination" to draft.destination,
                "startDate" to draft.startDate,
                "endDate" to draft.endDate,
                "totalBudget" to draft.totalBudget,
                "currency" to draft.currency,
                "visibility" to draft.visibility.name.lowercase(),
                "coverId" to draft.coverId
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun joinTrip(code: String): Result<String> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))

            // Codes only ever use JOIN_CODE_CHARS; anything else (such as "/") isn't a valid code.
            val normalized = code.trim().uppercase()
            if (normalized.length != JOIN_CODE_LENGTH || normalized.any { it !in JOIN_CODE_CHARS }) {
                return Result.failure(Exception("Trip not found"))
            }

            val record = joinCodesRef.document(normalized).get().await()
            val tripId = record.getString("tripId")
                ?: return Result.failure(Exception("Trip not found"))
            val tripRef = tripsRef.document(tripId)

            when (accessTo(tripRef)) {
                TripAccess.MEMBER -> return Result.failure(Exception("You are already a member of this trip"))
                TripAccess.NOT_FOUND -> return Result.failure(Exception("Trip not found"))
                TripAccess.NO_ACCESS -> Unit   // expected for a non-member: go ahead and join
            }

            val profile = firestore.collection("users").document(uid).get().await()
            val nickname = profile.getString("nickname") ?: ""
            val avatarId = profile.getString("avatarId") ?: "avatar_01"

            try {
                firestore.batch()
                    .update(tripRef, "memberIds", FieldValue.arrayUnion(uid))
                    // The member record carries the code used: the rules only accept the trip's current code.
                    .set(
                        tripRef.collection("members").document(uid),
                        memberDoc(uid, nickname, avatarId, role = "member") + ("joinCode" to normalized)
                    )
                    .commit().await()
            } catch (e: FirebaseFirestoreException) {
                // The rules let a non-member add only themselves, only to an open trip, with its current code.
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    return Result.failure(Exception("This trip is private and not accepting new members"))
                }
                throw e
            }
            Result.success(tripId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun leaveTrip(tripId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val tripRef = tripsRef.document(tripId)
            firestore.batch()
                .update(tripRef, "memberIds", FieldValue.arrayRemove(uid))
                .delete(tripRef.collection("members").document(uid))
                .commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTrip(tripId: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val tripRef = tripsRef.document(tripId)
            val code = tripRef.get().await().getString("joinCode")
            val memberRefs = tripRef.collection("members").get().await().documents.map { it.reference }

            // One batch: member records, the join-code record, the trip itself, and a tombstone.
            // Firestore keeps a deleted document's sub-collections (days, entries), so the tombstone
            // stops anyone re-creating this id to read them.
            val batch = firestore.batch()
            memberRefs.forEach { batch.delete(it) }
            if (code != null && joinCodeBelongsTo(code, tripId)) batch.delete(joinCodesRef.document(code))
            batch.delete(tripRef)
            batch.set(
                deletedTripsRef.document(tripId),
                mapOf("deletedBy" to uid, "deletedAt" to FieldValue.serverTimestamp())
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun regenerateJoinCode(tripId: String): Result<String> = try {
        val tripRef = tripsRef.document(tripId)
        val newCode = generateUniqueJoinCode()
        // A transaction, so two regenerations at once (e.g. a double tap) can't leave an old code
        // record behind: if the trip or its record changes underneath, Firestore retries.
        firestore.runTransaction { tx ->
            val oldCode = tx.get(tripRef).getString("joinCode")
            val oldRecord = oldCode?.let { tx.get(joinCodesRef.document(it)) }
            tx.update(tripRef, "joinCode", newCode)
            tx.set(joinCodesRef.document(newCode), mapOf("tripId" to tripId))
            if (oldCode != null && oldRecord?.getString("tripId") == tripId) {
                tx.delete(joinCodesRef.document(oldCode))
            }
            newCode
        }.await()
        Result.success(newCode)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun ensureJoinCodes(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        if (joinCodesCheckedFor == uid) return Result.success(Unit)
        return try {
            val myTrips = tripsRef.whereArrayContains("memberIds", uid).get().await()
            var firstError: Exception? = null
            for (tripDoc in myTrips.documents) {
                try {
                    backfillJoinCode(tripDoc, uid)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (firstError == null) firstError = e   // keep going; retry next launch
                }
            }
            if (firstError != null) {
                Result.failure(firstError)
            } else {
                joinCodesCheckedFor = uid
                Result.success(Unit)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Trips created before 1.0.2 have no lookup record; create it so their code works again. */
    private suspend fun backfillJoinCode(tripDoc: DocumentSnapshot, uid: String) {
        val code = tripDoc.getString("joinCode") ?: return
        val codeRef = joinCodesRef.document(code)
        val record = codeRef.get().await()
        when {
            !record.exists() -> codeRef.set(mapOf("tripId" to tripDoc.id)).await()
            // An old code collided with a newer trip's code: only the owner can pick a new one.
            record.getString("tripId") != tripDoc.id && tripDoc.getString("ownerId") == uid ->
                regenerateJoinCode(tripDoc.id).getOrThrow()
        }
    }

    private suspend fun joinCodeBelongsTo(code: String, tripId: String): Boolean =
        joinCodesRef.document(code).get().await().getString("tripId") == tripId

    private enum class TripAccess { MEMBER, NOT_FOUND, NO_ACCESS }

    /** Only members can read a trip; anyone may learn that a trip id doesn't exist (see the rules). */
    private suspend fun accessTo(ref: DocumentReference): TripAccess = try {
        if (ref.get(Source.SERVER).await().exists()) TripAccess.MEMBER else TripAccess.NOT_FOUND
    } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) TripAccess.NO_ACCESS else throw e
    }

    private suspend fun generateUniqueJoinCode(): String {
        repeat(10) {
            val code = randomJoinCode()
            if (!joinCodesRef.document(code).get().await().exists()) return code
        }
        // Practically unreachable. If the code is taken after all, the rules reject the batch
        // rather than overwrite another trip's record.
        return randomJoinCode()
    }

    private fun randomJoinCode(): String =
        (1..JOIN_CODE_LENGTH).map { JOIN_CODE_CHARS.random() }.joinToString("")

    private fun memberDoc(uid: String, nickname: String, avatarId: String, role: String) = mapOf(
        "userId" to uid,
        "nickname" to nickname,
        "avatarId" to avatarId,
        "role" to role,
        "joinedAt" to FieldValue.serverTimestamp()
    )

    // ── Document mappers ──────────────────────────────────────────────────────

    private fun DocumentSnapshot.toTrip(): Trip? {
        return try {
            val visibilityStr = getString("visibility") ?: "open"
            val visibility = if (visibilityStr == "locked") TripVisibility.LOCKED else TripVisibility.OPEN
            @Suppress("UNCHECKED_CAST")
            Trip(
                id = id,
                name = getString("name") ?: return null,
                destination = getString("destination") ?: "",
                startDate = getString("startDate") ?: return null,
                endDate = getString("endDate") ?: return null,
                totalBudget = getDouble("totalBudget") ?: 0.0,
                currency = getString("currency") ?: "USD",
                visibility = visibility,
                joinCode = getString("joinCode") ?: "",
                coverId = getString("coverId") ?: "cover_mountains",
                ownerId = getString("ownerId") ?: "",
                memberIds = get("memberIds") as? List<String> ?: emptyList(),
                createdAt = getTimestamp("createdAt")?.toDate()?.time ?: 0L
            )
        } catch (e: Exception) { null }
    }

    private fun DocumentSnapshot.toMember(): Member? {
        return try {
            Member(
                userId = getString("userId") ?: return null,
                nickname = getString("nickname") ?: "",
                avatarId = getString("avatarId") ?: "avatar_01",
                role = getString("role") ?: "member",
                joinedAt = getTimestamp("joinedAt")?.toDate()?.time ?: 0L
            )
        } catch (e: Exception) { null }
    }
}
