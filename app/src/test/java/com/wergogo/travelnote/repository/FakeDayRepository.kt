package com.wergogo.travelnote.repository

import com.wergogo.travelnote.data.model.Day
import com.wergogo.travelnote.data.model.Event
import com.wergogo.travelnote.data.model.EventDraft
import com.wergogo.travelnote.data.model.Expense
import com.wergogo.travelnote.data.model.ExpenseDraft
import com.wergogo.travelnote.data.model.Note
import com.wergogo.travelnote.data.model.NoteDraft
import com.wergogo.travelnote.data.repository.DayRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Controllable [DayRepository]. Observed data is keyed by (tripId, dateKey), so tests can feed
 * several days and trips, and a ViewModel observing the wrong one gets nothing.
 */
class FakeDayRepository : DayRepository {
    private val days = mutableMapOf<Pair<String, String>, MutableStateFlow<Day>>()
    private val events = mutableMapOf<Pair<String, String>, MutableStateFlow<List<Event>>>()
    private val expenses = mutableMapOf<Pair<String, String>, MutableStateFlow<List<Expense>>>()
    private val notes = mutableMapOf<Pair<String, String>, MutableStateFlow<List<Note>>>()

    private fun dayFlow(tripId: String, dateKey: String) =
        days.getOrPut(tripId to dateKey) { MutableStateFlow(Day(date = dateKey)) }
    private fun eventsFlow(tripId: String, dateKey: String) =
        events.getOrPut(tripId to dateKey) { MutableStateFlow(emptyList()) }
    private fun expensesFlow(tripId: String, dateKey: String) =
        expenses.getOrPut(tripId to dateKey) { MutableStateFlow(emptyList()) }
    private fun notesFlow(tripId: String, dateKey: String) =
        notes.getOrPut(tripId to dateKey) { MutableStateFlow(emptyList()) }

    fun setDay(tripId: String, dateKey: String, day: Day) { dayFlow(tripId, dateKey).value = day }
    fun setEvents(tripId: String, dateKey: String, list: List<Event>) { eventsFlow(tripId, dateKey).value = list }
    fun setExpenses(tripId: String, dateKey: String, list: List<Expense>) { expensesFlow(tripId, dateKey).value = list }
    fun setNotes(tripId: String, dateKey: String, list: List<Note>) { notesFlow(tripId, dateKey).value = list }

    /** A write or delete call: which trip and day it targeted and, for deletes, which item. */
    data class Target(val tripId: String, val dateKey: String, val id: String? = null)

    var upsertEventResult: Result<Unit> = Result.success(Unit)
    var deleteEventResult: Result<Unit> = Result.success(Unit)
    var addExpenseResult: Result<Unit> = Result.success(Unit)
    var deleteExpenseResult: Result<Unit> = Result.success(Unit)
    var addNoteResult: Result<Unit> = Result.success(Unit)
    var deleteNoteResult: Result<Unit> = Result.success(Unit)

    /** When set, suspending calls wait for it, so tests can observe loading flags mid-call. */
    var gate: CompletableDeferred<Unit>? = null

    var upsertEventCalls = 0
        private set
    var lastEventDraft: EventDraft? = null
        private set
    var addExpenseCalls = 0
        private set
    var lastExpenseDraft: ExpenseDraft? = null
        private set
    var addNoteCalls = 0
        private set
    var lastNoteDraft: NoteDraft? = null
        private set

    /** Every write/delete call in order, with the trip, day and (for deletes) item id it targeted. */
    val eventWrites = mutableListOf<Target>()
    val expenseWrites = mutableListOf<Target>()
    val noteWrites = mutableListOf<Target>()
    val deletedEvents = mutableListOf<Target>()
    val deletedExpenses = mutableListOf<Target>()
    val deletedNotes = mutableListOf<Target>()

    override fun observeDay(tripId: String, dateKey: String): Flow<Day> = dayFlow(tripId, dateKey)
    override fun observeEvents(tripId: String, dateKey: String): Flow<List<Event>> = eventsFlow(tripId, dateKey)
    override fun observeExpenses(tripId: String, dateKey: String): Flow<List<Expense>> = expensesFlow(tripId, dateKey)
    override fun observeNotes(tripId: String, dateKey: String): Flow<List<Note>> = notesFlow(tripId, dateKey)

    override suspend fun setStay(tripId: String, dateKey: String, name: String?, cost: Double?): Result<Unit> =
        Result.success(Unit) // not used by any ViewModel under test

    override suspend fun upsertEvent(tripId: String, dateKey: String, draft: EventDraft): Result<Unit> {
        upsertEventCalls++
        lastEventDraft = draft
        eventWrites += Target(tripId, dateKey)
        gate?.await()
        return upsertEventResult
    }

    override suspend fun deleteEvent(tripId: String, dateKey: String, eventId: String): Result<Unit> {
        deletedEvents += Target(tripId, dateKey, eventId)
        return deleteEventResult
    }

    override suspend fun addExpense(tripId: String, dateKey: String, draft: ExpenseDraft): Result<Unit> {
        addExpenseCalls++
        lastExpenseDraft = draft
        expenseWrites += Target(tripId, dateKey)
        gate?.await()
        return addExpenseResult
    }

    override suspend fun deleteExpense(tripId: String, dateKey: String, expenseId: String): Result<Unit> {
        deletedExpenses += Target(tripId, dateKey, expenseId)
        return deleteExpenseResult
    }

    override suspend fun addNote(tripId: String, dateKey: String, draft: NoteDraft): Result<Unit> {
        addNoteCalls++
        lastNoteDraft = draft
        noteWrites += Target(tripId, dateKey)
        gate?.await()
        return addNoteResult
    }

    override suspend fun deleteNote(tripId: String, dateKey: String, noteId: String): Result<Unit> {
        deletedNotes += Target(tripId, dateKey, noteId)
        return deleteNoteResult
    }
}
