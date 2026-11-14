package com.wergogo.travelnote.ui.createtrip

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.wergogo.travelnote.data.model.TripDraft
import com.wergogo.travelnote.data.model.TripVisibility
import com.wergogo.travelnote.data.repository.TripRepository
import com.wergogo.travelnote.util.parseAmount
import com.wergogo.travelnote.util.tripDatesError

class CreateTripViewModel(
    private val tripRepo: TripRepository
) : ViewModel() {
    var name by mutableStateOf("")
    var destination by mutableStateOf("")
    var startDate by mutableStateOf("")   // "yyyy-MM-dd"
    var endDate by mutableStateOf("")
    var totalBudget by mutableStateOf("")  // String so user can type freely
    var currency by mutableStateOf("USD")
    var visibility by mutableStateOf(TripVisibility.OPEN)
    var coverId by mutableStateOf("cover_mountains")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // returns tripId on success, null on failure
    suspend fun createTrip(): String? {
        if (name.isBlank()) { errorMessage = "Name is required"; return null }
        tripDatesError(startDate, endDate)?.let { errorMessage = it; return null }
        val budget = if (totalBudget.isBlank()) 0.0 else parseAmount(totalBudget)
            ?: run { errorMessage = "Enter a valid budget"; return null }
        isLoading = true
        errorMessage = null
        val result = tripRepo.createTrip(
            TripDraft(name.trim(), destination.trim(), startDate, endDate, budget, currency, visibility, coverId)
        )
        isLoading = false
        return if (result.isSuccess) {
            result.getOrNull()?.id
        } else {
            errorMessage = result.exceptionOrNull()?.message ?: "Failed to create trip"
            null
        }
    }
}
