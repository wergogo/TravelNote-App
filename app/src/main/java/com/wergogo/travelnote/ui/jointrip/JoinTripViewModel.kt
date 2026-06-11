package com.wergogo.travelnote.ui.jointrip

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.wergogo.travelnote.data.repository.TripRepository

class JoinTripViewModel(
    private val tripRepo: TripRepository
) : ViewModel() {
    var joinCode by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun onCodeChange(v: String) {
        joinCode = v.uppercase().take(6)
        errorMessage = null
    }

    // returns tripId on success, null on failure
    suspend fun joinTrip(): String? {
        if (joinCode.length != 6) { errorMessage = "Enter a 6-character code"; return null }
        isLoading = true
        errorMessage = null
        val result = tripRepo.joinTrip(joinCode)
        isLoading = false
        return if (result.isSuccess) {
            result.getOrNull()
        } else {
            errorMessage = result.exceptionOrNull()?.message ?: "Failed to join trip"
            null
        }
    }
}
