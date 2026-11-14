package com.wergogo.travelnote.ui.daydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wergogo.travelnote.data.model.Day
import com.wergogo.travelnote.data.model.Event
import com.wergogo.travelnote.data.model.Expense
import com.wergogo.travelnote.data.model.Note
import com.wergogo.travelnote.data.model.Trip
import com.wergogo.travelnote.data.repository.DayRepository
import com.wergogo.travelnote.data.repository.TripRepository
import com.wergogo.travelnote.util.EventChronology
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DayDetailViewModel(
    private val tripRepo: TripRepository,
    private val dayRepo: DayRepository,
    private val tripId: String,
    private val dateKey: String
) : ViewModel() {

    private val _trip = MutableStateFlow<Trip?>(null)
    val trip: StateFlow<Trip?> = _trip.asStateFlow()

    private val _day = MutableStateFlow(Day())
    val day: StateFlow<Day> = _day.asStateFlow()

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    init {
        viewModelScope.launch { tripRepo.observeTrip(tripId).collect { _trip.value = it } }
        viewModelScope.launch { dayRepo.observeDay(tripId, dateKey).collect { _day.value = it } }
        // Firestore returns events in the order they were added; show them by time of day.
        viewModelScope.launch {
            dayRepo.observeEvents(tripId, dateKey).collect { _events.value = it.sortedWith(EventChronology) }
        }
        viewModelScope.launch { dayRepo.observeExpenses(tripId, dateKey).collect { _expenses.value = it } }
        viewModelScope.launch { dayRepo.observeNotes(tripId, dateKey).collect { _notes.value = it } }
    }

    suspend fun deleteEvent(eventId: String) = dayRepo.deleteEvent(tripId, dateKey, eventId)
    suspend fun deleteExpense(expenseId: String) = dayRepo.deleteExpense(tripId, dateKey, expenseId)
    suspend fun deleteNote(noteId: String) = dayRepo.deleteNote(tripId, dateKey, noteId)
}
