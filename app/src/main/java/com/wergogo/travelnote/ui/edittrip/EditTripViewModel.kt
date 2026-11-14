package com.wergogo.travelnote.ui.edittrip

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wergogo.travelnote.data.model.TripDraft
import com.wergogo.travelnote.data.model.TripVisibility
import com.wergogo.travelnote.data.repository.TripRepository
import com.wergogo.travelnote.util.amountInputText
import com.wergogo.travelnote.util.parseAmount
import com.wergogo.travelnote.util.tripDatesError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EditTripViewModel(
    private val tripRepo: TripRepository,
    private val tripId: String
) : ViewModel() {
    var name by mutableStateOf("")
    var destination by mutableStateOf("")
    var startDate by mutableStateOf("")
    var endDate by mutableStateOf("")
    var totalBudget by mutableStateOf("")
    var currency by mutableStateOf("USD")
    var visibility by mutableStateOf(TripVisibility.OPEN)
    var coverId by mutableStateOf("cover_mountains")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            tripRepo.observeTrip(tripId).first()?.let { trip ->
                name = trip.name
                destination = trip.destination
                startDate = trip.startDate
                endDate = trip.endDate
                totalBudget = amountInputText(trip.totalBudget)
                currency = trip.currency
                visibility = trip.visibility
                coverId = trip.coverId
            }
        }
    }

    suspend fun saveChanges(): Boolean {
        if (name.isBlank()) { errorMessage = "Name is required"; return false }
        tripDatesError(startDate, endDate)?.let { errorMessage = it; return false }
        val budget = if (totalBudget.isBlank()) 0.0 else parseAmount(totalBudget)
            ?: run { errorMessage = "Enter a valid budget"; return false }
        isLoading = true
        errorMessage = null
        val result = tripRepo.updateTrip(
            tripId,
            TripDraft(name.trim(), destination.trim(), startDate, endDate, budget, currency, visibility, coverId)
        )
        isLoading = false
        if (result.isFailure) {
            errorMessage = result.exceptionOrNull()?.message ?: "Failed to save"
            return false
        }
        return true
    }
}
