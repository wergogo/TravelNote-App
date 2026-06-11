package com.wergogo.travelnote.repository

import com.wergogo.travelnote.data.model.Day
import com.wergogo.travelnote.data.model.Event
import com.wergogo.travelnote.data.model.EventDraft
import com.wergogo.travelnote.data.model.Expense
import com.wergogo.travelnote.data.model.ExpenseDraft
import com.wergogo.travelnote.data.model.Note
import com.wergogo.travelnote.data.model.NoteDraft
import com.wergogo.travelnote.data.repository.DayRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeDayRepository : DayRepository {
    private val _day = MutableStateFlow(Day())
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    private val _notes = MutableStateFlow<List<Note>>(emptyList())

    fun setDay(day: Day) { _day.value = day }
    fun setEvents(events: List<Event>) { _events.value = events }
    fun setExpenses(expenses: List<Expense>) { _expenses.value = expenses }
    fun setNotes(notes: List<Note>) { _notes.value = notes }

    override fun observeDay(tripId: String, dateKey: String): Flow<Day> = _day
    override fun observeEvents(tripId: String, dateKey: String): Flow<List<Event>> = _events
    override fun observeExpenses(tripId: String, dateKey: String): Flow<List<Expense>> = _expenses
    override fun observeNotes(tripId: String, dateKey: String): Flow<List<Note>> = _notes

    override suspend fun setStay(tripId: String, dateKey: String, name: String?, cost: Double?): Result<Unit> {
        _day.value = _day.value.copy(stayName = name ?: "", stayCost = cost ?: 0.0)
        return Result.success(Unit)
    }
    override suspend fun upsertEvent(tripId: String, dateKey: String, draft: EventDraft): Result<Unit> = Result.success(Unit)
    override suspend fun deleteEvent(tripId: String, dateKey: String, eventId: String): Result<Unit> = Result.success(Unit)
    override suspend fun addExpense(tripId: String, dateKey: String, draft: ExpenseDraft): Result<Unit> = Result.success(Unit)
    override suspend fun deleteExpense(tripId: String, dateKey: String, expenseId: String): Result<Unit> = Result.success(Unit)
    override suspend fun addNote(tripId: String, dateKey: String, draft: NoteDraft): Result<Unit> = Result.success(Unit)
    override suspend fun deleteNote(tripId: String, dateKey: String, noteId: String): Result<Unit> = Result.success(Unit)
}
