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
import kotlinx.coroutines.flow.first
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

    private val _currentUid = MutableStateFlow<String?>(null)
    val currentUid: StateFlow<String?> = _currentUid.asStateFlow()

    init {
        viewModelScope.launch {
            authRepo.authState().collect { uid ->
                _currentUid.value = uid
            }
        }
        viewModelScope.launch {
            authRepo.authState().first()?.let { uid ->
                tripRepo.observeMyTrips(uid).collect { trips ->
                    val found = trips.find { it.tripId == tripId }
                    _trip.value = found
                }
            }
        }
    }

    suspend fun deleteTrip(): Result<Unit> {
        val uid = _currentUid.value ?: return Result.failure(Exception("Not logged in"))
        return tripRepo.deleteTrip(uid, tripId)
    }

    suspend fun leaveTrip(): Result<Unit> {
        val uid = _currentUid.value ?: return Result.failure(Exception("Not logged in"))
        return tripRepo.leaveTrip(uid, tripId)
    }
}
