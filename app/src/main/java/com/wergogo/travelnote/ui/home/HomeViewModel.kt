package com.wergogo.travelnote.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wergogo.travelnote.data.model.Trip
import com.wergogo.travelnote.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val tripRepo: TripRepository
) : ViewModel() {

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips.asStateFlow()

    val isLoading = MutableStateFlow(true)

    init {
        // Backfill join-code records for trips created before 1.0.2 (no-op after the first run).
        viewModelScope.launch { tripRepo.ensureJoinCodes() }
        viewModelScope.launch {
            tripRepo.observeMyTrips().collect { list ->
                _trips.value = list
                isLoading.value = false
            }
        }
    }
}
