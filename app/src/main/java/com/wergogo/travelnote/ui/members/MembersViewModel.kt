package com.wergogo.travelnote.ui.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wergogo.travelnote.data.model.Member
import com.wergogo.travelnote.data.model.Trip
import com.wergogo.travelnote.data.repository.AuthRepository
import com.wergogo.travelnote.data.repository.DayRepository
import com.wergogo.travelnote.data.repository.TripRepository
import com.wergogo.travelnote.ui.tripoverview.generateDateKeys
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class MembersViewModel(
    private val authRepo: AuthRepository,
    private val tripRepo: TripRepository,
    private val dayRepo: DayRepository,
    private val tripId: String
) : ViewModel() {

    private val _trip = MutableStateFlow<Trip?>(null)
    val trip: StateFlow<Trip?> = _trip.asStateFlow()

    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    private val _memberSpending = MutableStateFlow<Map<String, Double>>(emptyMap())
    val memberSpending: StateFlow<Map<String, Double>> = _memberSpending.asStateFlow()

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
                if (dateKeys.isEmpty()) {
                    _memberSpending.value = emptyMap()
                    return@collect
                }

                spendingJob = viewModelScope.launch {
                    val dayFlows = dateKeys.map { dateKey ->
                        combine(
                            dayRepo.observeEvents(tripId, dateKey),
                            dayRepo.observeExpenses(tripId, dateKey)
                        ) { events, expenses ->
                            val spending = mutableMapOf<String, Double>()
                            events.forEach { ev ->
                                if (ev.cost > 0) {
                                    spending[ev.authorId] =
                                        (spending[ev.authorId] ?: 0.0) + ev.cost
                                }
                            }
                            expenses.forEach { exp ->
                                spending[exp.authorId] =
                                    (spending[exp.authorId] ?: 0.0) + exp.amount
                            }
                            spending
                        }
                    }

                    combine(dayFlows) { dayMaps ->
                        val total = mutableMapOf<String, Double>()
                        dayMaps.forEach { dayMap ->
                            dayMap.forEach { (userId, amount) ->
                                total[userId] = (total[userId] ?: 0.0) + amount
                            }
                        }
                        total.toMap()
                    }.collect { _memberSpending.value = it }
                }
            }
        }
    }

    suspend fun regenerateJoinCode(): Result<String> = tripRepo.regenerateJoinCode(tripId)

    suspend fun leaveTrip(): Result<Unit> = tripRepo.leaveTrip(tripId)

    suspend fun deleteTrip(): Result<Unit> = tripRepo.deleteTrip(tripId)
}
