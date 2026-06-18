package com.wergogo.travelnote.ui.tripoverview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wergogo.travelnote.data.model.Event
import com.wergogo.travelnote.data.model.Member
import com.wergogo.travelnote.data.model.Trip
import com.wergogo.travelnote.data.repository.AuthRepository
import com.wergogo.travelnote.data.repository.DayRepository
import com.wergogo.travelnote.data.repository.TripRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DaySummary(
    val events: List<Event> = emptyList(),
    val totalExpense: Double = 0.0
)

class TripOverviewViewModel(
    private val authRepo: AuthRepository,
    private val tripRepo: TripRepository,
    private val dayRepo: DayRepository,
    private val tripId: String
) : ViewModel() {

    private val _trip = MutableStateFlow<Trip?>(null)
    val trip: StateFlow<Trip?> = _trip.asStateFlow()

    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    private val _spent = MutableStateFlow(0.0)
    val spent: StateFlow<Double> = _spent.asStateFlow()

    private val _daySummaries = MutableStateFlow<Map<String, DaySummary>>(emptyMap())
    val daySummaries: StateFlow<Map<String, DaySummary>> = _daySummaries.asStateFlow()

    val currentUserId: String?
        get() = authRepo.currentUserId

    private var spendingJob: Job? = null

    init {
        viewModelScope.launch {
            tripRepo.observeTrip(tripId).collect { _trip.value = it }
        }
        viewModelScope.launch {
            tripRepo.observeMembers(tripId).collect { _members.value = it }
        }
        viewModelScope.launch {
            _trip.filterNotNull().collect { trip ->
                spendingJob?.cancel()
                val dateKeys = generateDateKeys(trip.startDate, trip.endDate)
                if (dateKeys.isEmpty()) { _spent.value = 0.0; _daySummaries.value = emptyMap(); return@collect }

                spendingJob = viewModelScope.launch {
                    // Per-day summary flows (events + expense totals)
                    val summaryFlows = dateKeys.map { dateKey ->
                        combine(
                            dayRepo.observeEvents(tripId, dateKey),
                            dayRepo.observeExpenses(tripId, dateKey)
                        ) { events, expenses ->
                            dateKey to DaySummary(
                                events = events.sortedBy { it.time },
                                totalExpense = expenses.sumOf { it.amount } + events.sumOf { it.cost }
                            )
                        }
                    }

                    combine(summaryFlows) { pairs ->
                        pairs.toMap()
                    }.collect { summaryMap ->
                        _daySummaries.value = summaryMap
                        // totalExpense already includes event costs
                        _spent.value = summaryMap.values.sumOf { it.totalExpense }
                    }
                }
            }
        }
    }

    suspend fun deleteTrip(): Result<Unit> = tripRepo.deleteTrip(tripId)

    suspend fun leaveTrip(): Result<Unit> = tripRepo.leaveTrip(tripId)
}

internal fun generateDateKeys(startDate: String, endDate: String): List<String> {
    return try {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)
        generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .map { it.toString() }
            .toList()
    } catch (e: Exception) { emptyList() }
}
