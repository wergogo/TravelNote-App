package com.wergogo.travelnote.data.source.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.wergogo.travelnote.data.model.Day
import com.wergogo.travelnote.data.model.Event
import com.wergogo.travelnote.data.model.EventDraft
import com.wergogo.travelnote.data.model.EventType
import com.wergogo.travelnote.data.model.Expense
import com.wergogo.travelnote.data.model.ExpenseCategory
import com.wergogo.travelnote.data.model.ExpenseDraft
import com.wergogo.travelnote.data.model.Note
import com.wergogo.travelnote.data.model.NoteDraft
import com.wergogo.travelnote.data.repository.DayRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseDayRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : DayRepository {

    // ── Observe ───────────────────────────────────────────────────────────────

    override fun observeDay(tripId: String, dateKey: String): Flow<Day> = callbackFlow {
        val ref = dayRef(tripId, dateKey)
        val reg = ref.addSnapshotListener { snap, err ->
            if (err != null) { close(); return@addSnapshotListener }
            val day = if (snap != null && snap.exists()) {
                Day(
                    date = dateKey,
                    stayName = snap.getString("stayName") ?: "",
                    stayCost = snap.getDouble("stayCost") ?: 0.0
                )
            } else {
                Day(date = dateKey)
            }
            trySend(day)
        }
        awaitClose { reg.remove() }
    }

    override fun observeEvents(tripId: String, dateKey: String): Flow<List<Event>> = callbackFlow {
        val reg = dayRef(tripId, dateKey).collection("events")
            .orderBy("createdAt")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.toEvent() } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    override fun observeExpenses(tripId: String, dateKey: String): Flow<List<Expense>> = callbackFlow {
        val reg = dayRef(tripId, dateKey).collection("expenses")
            .orderBy("createdAt")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.toExpense() } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    override fun observeNotes(tripId: String, dateKey: String): Flow<List<Note>> = callbackFlow {
        val reg = dayRef(tripId, dateKey).collection("notes")
            .orderBy("createdAt")
            .addSnapshotListener { snap, err ->
                if (err != null) { close(); return@addSnapshotListener }
                trySend(snap?.documents?.mapNotNull { it.toNote() } ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    override suspend fun setStay(
        tripId: String, dateKey: String, name: String?, cost: Double?
    ): Result<Unit> = try {
        val data = mapOf(
            "date" to dateKey,
            "stayName" to (name ?: ""),
            "stayCost" to (cost ?: 0.0)
        )
        dayRef(tripId, dateKey).set(data).await()
        Result.success(Unit)
    } catch (e: CancellationException) { throw e } catch (e: Exception) { Result.failure(e) }

    override suspend fun upsertEvent(
        tripId: String, dateKey: String, draft: EventDraft
    ): Result<Unit> = try {
        val author = if (draft.authorId != null) {
            Triple(draft.authorId, draft.authorNickname ?: "", draft.authorAvatarId ?: "avatar_01")
        } else {
            currentAuthor()
        }
        val ref = if (draft.eventId != null) {
            dayRef(tripId, dateKey).collection("events").document(draft.eventId)
        } else {
            dayRef(tripId, dateKey).collection("events").document()
        }
        val data = mutableMapOf<String, Any?>(
            "type" to draft.type.name.lowercase(),
            "time" to draft.time,
            "cost" to draft.cost,
            "flightNumber" to draft.flightNumber,
            "depAirport" to draft.depAirport,
            "arrAirport" to draft.arrAirport,
            "title" to draft.title,
            "locationName" to draft.locationName,
            "authorId" to author.first,
            "authorNickname" to author.second,
            "authorAvatarId" to author.third
        )
        if (draft.eventId == null) {
            data["createdAt"] = FieldValue.serverTimestamp()
            // Who actually added the entry; the rules require it on create and keep it fixed.
            data["createdBy"] = auth.currentUser?.uid
        }
        ref.set(data, SetOptions.merge()).await()
        Result.success(Unit)
    } catch (e: CancellationException) { throw e } catch (e: Exception) { Result.failure(e) }

    override suspend fun deleteEvent(
        tripId: String, dateKey: String, eventId: String
    ): Result<Unit> = try {
        dayRef(tripId, dateKey).collection("events").document(eventId).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) { throw e } catch (e: Exception) { Result.failure(e) }

    override suspend fun addExpense(
        tripId: String, dateKey: String, draft: ExpenseDraft
    ): Result<Unit> = try {
        val author = if (draft.authorId != null) {
            Triple(draft.authorId, draft.authorNickname ?: "", draft.authorAvatarId ?: "avatar_01")
        } else {
            currentAuthor()
        }
        val ref = if (draft.expenseId != null) {
            dayRef(tripId, dateKey).collection("expenses").document(draft.expenseId)
        } else {
            dayRef(tripId, dateKey).collection("expenses").document()
        }
        val data = mutableMapOf<String, Any?>(
            "category" to draft.category.name.lowercase(),
            "amount" to draft.amount,
            "description" to draft.description,
            "placeLabel" to draft.placeLabel,
            "authorId" to author.first,
            "authorNickname" to author.second,
            "authorAvatarId" to author.third
        )
        if (draft.lat != null) data["lat"] = draft.lat
        if (draft.lng != null) data["lng"] = draft.lng
        if (draft.expenseId == null) {
            data["createdAt"] = FieldValue.serverTimestamp()
            // Who actually added the entry; the payer is stored separately in authorId.
            data["createdBy"] = auth.currentUser?.uid
        }
        ref.set(data, SetOptions.merge()).await()
        Result.success(Unit)
    } catch (e: CancellationException) { throw e } catch (e: Exception) { Result.failure(e) }

    override suspend fun deleteExpense(
        tripId: String, dateKey: String, expenseId: String
    ): Result<Unit> = try {
        dayRef(tripId, dateKey).collection("expenses").document(expenseId).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) { throw e } catch (e: Exception) { Result.failure(e) }

    override suspend fun addNote(
        tripId: String, dateKey: String, draft: NoteDraft
    ): Result<Unit> = try {
        val author = currentAuthor()
        dayRef(tripId, dateKey).collection("notes").add(
            mapOf(
                "text" to draft.text,
                "authorId" to author.first,
                "authorNickname" to author.second,
                "authorAvatarId" to author.third,
                "createdAt" to FieldValue.serverTimestamp(),
                "createdBy" to auth.currentUser?.uid
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) { throw e } catch (e: Exception) { Result.failure(e) }

    override suspend fun deleteNote(
        tripId: String, dateKey: String, noteId: String
    ): Result<Unit> = try {
        dayRef(tripId, dateKey).collection("notes").document(noteId).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) { throw e } catch (e: Exception) { Result.failure(e) }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun dayRef(tripId: String, dateKey: String) =
        firestore.collection("trips").document(tripId).collection("days").document(dateKey)

    /** Reads current user's nickname + avatarId from Firestore for author snapshot. */
    private suspend fun currentAuthor(): Triple<String, String, String> {
        val uid = auth.currentUser?.uid ?: return Triple("", "", "avatar_01")
        val doc = firestore.collection("users").document(uid).get().await()
        return Triple(
            uid,
            doc.getString("nickname") ?: "",
            doc.getString("avatarId") ?: "avatar_01"
        )
    }

    // ── Document mappers ──────────────────────────────────────────────────────

    private fun com.google.firebase.firestore.DocumentSnapshot.toEvent(): Event? = try {
        val typeStr = getString("type") ?: "activity"
        val type = if (typeStr == "flight") EventType.FLIGHT else EventType.ACTIVITY
        Event(
            id = id,
            type = type,
            time = getString("time") ?: "",
            cost = getDouble("cost") ?: 0.0,
            flightNumber = getString("flightNumber") ?: "",
            depAirport = getString("depAirport") ?: "",
            arrAirport = getString("arrAirport") ?: "",
            title = getString("title") ?: "",
            locationName = getString("locationName") ?: "",
            authorId = getString("authorId") ?: "",
            authorNickname = getString("authorNickname") ?: "",
            authorAvatarId = getString("authorAvatarId") ?: "avatar_01",
            createdAt = getTimestamp("createdAt")?.toDate()?.time ?: 0L
        )
    } catch (e: Exception) { null }

    private fun com.google.firebase.firestore.DocumentSnapshot.toExpense(): Expense? = try {
        val catStr = getString("category") ?: "other"
        val category = when (catStr) {
            "food" -> ExpenseCategory.FOOD
            "transport" -> ExpenseCategory.TRANSPORT
            "shopping" -> ExpenseCategory.SHOPPING
            "accommodation" -> ExpenseCategory.ACCOMMODATION
            else -> ExpenseCategory.OTHER
        }
        Expense(
            id = id,
            category = category,
            amount = getDouble("amount") ?: 0.0,
            description = getString("description") ?: "",
            lat = getDouble("lat"),
            lng = getDouble("lng"),
            placeLabel = getString("placeLabel") ?: "",
            authorId = getString("authorId") ?: "",
            authorNickname = getString("authorNickname") ?: "",
            authorAvatarId = getString("authorAvatarId") ?: "avatar_01",
            createdAt = getTimestamp("createdAt")?.toDate()?.time ?: 0L
        )
    } catch (e: Exception) { null }

    private fun com.google.firebase.firestore.DocumentSnapshot.toNote(): Note? {
        return try {
            Note(
                id = id,
                text = getString("text") ?: return null,
                authorId = getString("authorId") ?: "",
                authorNickname = getString("authorNickname") ?: "",
                authorAvatarId = getString("authorAvatarId") ?: "avatar_01",
                createdAt = getTimestamp("createdAt")?.toDate()?.time ?: 0L
            )
        } catch (e: Exception) { null }
    }
}
