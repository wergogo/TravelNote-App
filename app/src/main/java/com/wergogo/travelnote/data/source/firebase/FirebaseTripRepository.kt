package com.wergogo.travelnote.data.source.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.wergogo.travelnote.data.model.Member
import com.wergogo.travelnote.data.model.Trip
import com.wergogo.travelnote.data.model.TripDraft
import com.wergogo.travelnote.data.model.TripVisibility
import com.wergogo.travelnote.data.repository.TripRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val JOIN_CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

class FirebaseTripRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : TripRepository {

    // ── Observe ───────────────────────────────────────────────────────────────

    override fun observeMyTrips(): Flow<List<Trip>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptyList()); close(); return@callbackFlow }
        val reg = firestore.collection("trips")
            .whereArrayContains("memberIds", uid)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.toTrip() } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    override fun observeTrip(tripId: String): Flow<Trip?> = callbackFlow {
        val reg = firestore.collection("trips").document(tripId)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.takeIf { it.exists() }?.toTrip())
            }
        awaitClose { reg.remove() }
    }

    override fun observeMembers(tripId: String): Flow<List<Member>> = callbackFlow {
        val reg = firestore.collection("trips").document(tripId).collection("members")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.toMember() } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    override suspend fun createTrip(draft: TripDraft): Result<Trip> = try {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        val profile = firestore.collection("users").document(uid).get().await()
        val nickname = profile.getString("nickname") ?: ""
        val avatarId = profile.getString("avatarId") ?: "avatar_01"

        val joinCode = generateUniqueJoinCode()
        val tripRef = firestore.collection("trips").document()
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
        tripRef.set(data).await()

        // Write denormalized member doc
        tripRef.collection("members").document(uid).set(
            mapOf(
                "userId" to uid,
                "nickname" to nickname,
                "avatarId" to avatarId,
                "role" to "owner",
                "joinedAt" to FieldValue.serverTimestamp()
            )
        ).await()

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
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateTrip(tripId: String, draft: TripDraft): Result<Unit> = try {
        firestore.collection("trips").document(tripId).update(
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
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun findByJoinCode(code: String): Result<Trip?> = try {
        val snap = firestore.collection("trips")
            .whereEqualTo("joinCode", code.uppercase())
            .get().await()
        val trip = snap.documents.firstOrNull()?.toTrip()
        Result.success(trip)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun joinTrip(code: String): Result<String> = try {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        val profile = firestore.collection("users").document(uid).get().await()
        val nickname = profile.getString("nickname") ?: ""
        val avatarId = profile.getString("avatarId") ?: "avatar_01"

        val snap = firestore.collection("trips")
            .whereEqualTo("joinCode", code.uppercase())
            .get().await()
        val tripDoc = snap.documents.firstOrNull()
            ?: return Result.failure(Exception("Trip not found"))
        val tripId = tripDoc.id

        // Add uid to memberIds array
        firestore.collection("trips").document(tripId)
            .update("memberIds", FieldValue.arrayUnion(uid))
            .await()

        // Write denormalized member doc
        firestore.collection("trips").document(tripId).collection("members").document(uid).set(
            mapOf(
                "userId" to uid,
                "nickname" to nickname,
                "avatarId" to avatarId,
                "role" to "member",
                "joinedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        Result.success(tripId)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun leaveTrip(tripId: String): Result<Unit> = try {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        firestore.collection("trips").document(tripId)
            .update("memberIds", FieldValue.arrayRemove(uid))
            .await()
        firestore.collection("trips").document(tripId).collection("members").document(uid)
            .delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteTrip(tripId: String): Result<Unit> = try {
        firestore.collection("trips").document(tripId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun regenerateJoinCode(tripId: String): Result<String> = try {
        val newCode = generateUniqueJoinCode()
        firestore.collection("trips").document(tripId)
            .update("joinCode", newCode)
            .await()
        Result.success(newCode)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun generateUniqueJoinCode(): String {
        repeat(10) {
            val code = (1..6).map { JOIN_CODE_CHARS.random() }.joinToString("")
            val existing = firestore.collection("trips")
                .whereEqualTo("joinCode", code).get().await()
            if (existing.isEmpty) return code
        }
        // Extremely unlikely to fail — return a fallback (still random)
        return (1..6).map { JOIN_CODE_CHARS.random() }.joinToString("")
    }

    // ── Document mappers ──────────────────────────────────────────────────────

    private fun com.google.firebase.firestore.DocumentSnapshot.toTrip(): Trip? = try {
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

    private fun com.google.firebase.firestore.DocumentSnapshot.toMember(): Member? = try {
        Member(
            userId = getString("userId") ?: return null,
            nickname = getString("nickname") ?: "",
            avatarId = getString("avatarId") ?: "avatar_01",
            role = getString("role") ?: "member",
            joinedAt = getTimestamp("joinedAt")?.toDate()?.time ?: 0L
        )
    } catch (e: Exception) { null }
}
