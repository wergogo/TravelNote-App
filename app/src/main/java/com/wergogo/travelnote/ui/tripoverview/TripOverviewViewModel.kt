package com.wergogo.travelnote.ui.tripoverview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wergogo.travelnote.data.model.Member
import com.wergogo.travelnote.data.model.Trip
import com.wergogo.travelnote.data.repository.AuthRepository
import com.wergogo.travelnote.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TripOverviewViewModel(
    private val authRepo: AuthRepository,
    private val tripRepo: TripRepository,
    private val tripId: String
) : ViewModel() {

    private val _trip = MutableStateFlow<Trip?>(null)
    val trip: StateFlow<Trip?> = _trip.asStateFlow()

    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    val currentUserId: String?
        get() = authRepo.currentUserId

    init {
        viewModelScope.launch {
            tripRepo.observeTrip(tripId).collect { _trip.value = it }
        }
        viewModelScope.launch {
            tripRepo.observeMembers(tripId).collect { _members.value = it }
        }
    }

    suspend fun deleteTrip(): Result<Unit> = tripRepo.deleteTrip(tripId)

    suspend fun leaveTrip(): Result<Unit> = tripRepo.leaveTrip(tripId)
}
