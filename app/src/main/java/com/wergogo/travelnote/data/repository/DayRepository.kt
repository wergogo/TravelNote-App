package com.wergogo.travelnote.data.repository

import com.wergogo.travelnote.data.model.Day
import com.wergogo.travelnote.data.model.Event
import com.wergogo.travelnote.data.model.EventDraft
import com.wergogo.travelnote.data.model.Expense
import com.wergogo.travelnote.data.model.ExpenseDraft
import com.wergogo.travelnote.data.model.Note
import com.wergogo.travelnote.data.model.NoteDraft
import kotlinx.coroutines.flow.Flow

interface DayRepository {
    fun observeDay(tripId: String, dateKey: String): Flow<Day>
    fun observeEvents(tripId: String, dateKey: String): Flow<List<Event>>
    fun observeExpenses(tripId: String, dateKey: String): Flow<List<Expense>>
    fun observeNotes(tripId: String, dateKey: String): Flow<List<Note>>
    suspend fun setStay(tripId: String, dateKey: String, name: String?, cost: Double?): Result<Unit>
    suspend fun upsertEvent(tripId: String, dateKey: String, draft: EventDraft): Result<Unit>
    suspend fun deleteEvent(tripId: String, dateKey: String, eventId: String): Result<Unit>
    suspend fun addExpense(tripId: String, dateKey: String, draft: ExpenseDraft): Result<Unit>
    suspend fun deleteExpense(tripId: String, dateKey: String, expenseId: String): Result<Unit>
    suspend fun addNote(tripId: String, dateKey: String, draft: NoteDraft): Result<Unit>
    suspend fun deleteNote(tripId: String, dateKey: String, noteId: String): Result<Unit>
}
