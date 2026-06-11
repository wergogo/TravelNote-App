package com.wergogo.travelnote.ui.addnote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.wergogo.travelnote.data.model.NoteDraft
import com.wergogo.travelnote.data.repository.DayRepository

class AddNoteViewModel(
    private val dayRepo: DayRepository,
    private val tripId: String,
    private val dateKey: String
) : ViewModel() {

    var text by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    suspend fun save(): Boolean {
        if (text.isBlank()) { errorMessage = "Note cannot be empty"; return false }
        isLoading = true
        errorMessage = null
        val result = dayRepo.addNote(tripId, dateKey, NoteDraft(text.trim()))
        isLoading = false
        if (result.isFailure) {
            errorMessage = result.exceptionOrNull()?.message ?: "Failed to save note"
            return false
        }
        return true
    }
}
