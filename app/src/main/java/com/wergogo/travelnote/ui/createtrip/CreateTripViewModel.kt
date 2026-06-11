package com.wergogo.travelnote.ui.createtrip

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.wergogo.travelnote.data.model.TripDraft
import com.wergogo.travelnote.data.model.TripVisibility
import com.wergogo.travelnote.data.repository.AuthRepository
import com.wergogo.travelnote.data.repository.TripRepository
import kotlinx.coroutines.flow.first

class CreateTripViewModel(
    private val authRepo: AuthRepository,
    private val tripRepo: TripRepository
) : ViewModel() {
    var name by mutableStateOf("")
    var destination by mutableStateOf("")
    var startDate by mutableStateOf("")   // "yyyy-MM-dd"
    var endDate by mutableStateOf("")
    var totalBudget by mutableStateOf("")  // String so user can type freely
    var currency by mutableStateOf("USD")
    var visibility by mutableStateOf(TripVisibility.OPEN)
    var coverId by mutableStateOf("cover_default")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // returns tripId on success, null on failure
    suspend fun createTrip(): String? {
        if (name.isBlank()) { errorMessage = "Name is required"; return null }
        if (startDate.isBlank() || endDate.isBlank()) { errorMessage = "Dates are required"; return null }
        val budget = totalBudget.toDoubleOrNull() ?: 0.0
        val uid = authRepo.authState().first() ?: run { errorMessage = "Not logged in"; return null }
        isLoading = true
        errorMessage = null
        val result = tripRepo.createTrip(
            uid,
            TripDraft(name.trim(), destination.trim(), startDate, endDate, budget, currency, visibility, coverId)
        )
        isLoading = false
        return if (result.isSuccess) {
            result.getOrNull()
        } else {
            errorMessage = result.exceptionOrNull()?.message ?: "Failed to create trip"
            null
        }
    }
}
