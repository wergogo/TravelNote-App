package com.wergogo.travelnote.ui.createtrip

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.wergogo.travelnote.data.model.TripDraft
import com.wergogo.travelnote.data.model.TripVisibility
import com.wergogo.travelnote.data.repository.TripRepository
import java.time.LocalDate

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
        if (startDate.isBlank() || endDate.isBlank()) { errorMessage = "Dates are required"; return null }
        try {
            if (LocalDate.parse(endDate).isBefore(LocalDate.parse(startDate))) {
                errorMessage = "End date cannot be before start date"; return null
            }
        } catch (_: Exception) {
            errorMessage = "Invalid date format"; return null
        }
        val budget = totalBudget.toDoubleOrNull() ?: 0.0
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
