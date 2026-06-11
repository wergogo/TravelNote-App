package com.wergogo.travelnote.ui.addevent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wergogo.travelnote.data.model.EventDraft
import com.wergogo.travelnote.data.model.EventType
import com.wergogo.travelnote.data.repository.DayRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddEventViewModel(
    private val dayRepo: DayRepository,
    private val tripId: String,
    private val dateKey: String,
    private val editEventId: String?   // null = adding new event
) : ViewModel() {

    var eventType by mutableStateOf(EventType.ACTIVITY)
    var time by mutableStateOf("")         // "HH:mm"
    var cost by mutableStateOf("")
    var flightNumber by mutableStateOf("")
    var depAirport by mutableStateOf("")
    var arrAirport by mutableStateOf("")
    var title by mutableStateOf("")
    var locationName by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    val isEditMode: Boolean get() = editEventId != null

    init {
        // If editing, pre-load the event
        if (editEventId != null) {
            viewModelScope.launch {
                dayRepo.observeEvents(tripId, dateKey).first()
                    .find { it.id == editEventId }
                    ?.let { ev ->
                        eventType = ev.type
                        time = ev.time
                        cost = ev.cost.toString()
                        flightNumber = ev.flightNumber
                        depAirport = ev.depAirport
                        arrAirport = ev.arrAirport
                        title = ev.title
                        locationName = ev.locationName
                    }
            }
        }
    }

    suspend fun save(): Boolean {
        if (time.isBlank()) { errorMessage = "Time is required"; return false }
        if (eventType == EventType.FLIGHT && (depAirport.isBlank() || arrAirport.isBlank())) {
            errorMessage = "Airports are required for flights"
            return false
        }
        if (eventType == EventType.ACTIVITY && title.isBlank()) {
            errorMessage = "Title is required for activities"
            return false
        }
        isLoading = true
        errorMessage = null
        val draft = EventDraft(
            type = eventType,
            time = time.trim(),
            cost = cost.toDoubleOrNull() ?: 0.0,
            flightNumber = flightNumber.uppercase().trim(),
            depAirport = depAirport.uppercase().trim(),
            arrAirport = arrAirport.uppercase().trim(),
            title = title.trim(),
            locationName = locationName.trim(),
            eventId = editEventId
        )
        val result = dayRepo.upsertEvent(tripId, dateKey, draft)
        isLoading = false
        if (result.isFailure) {
            errorMessage = result.exceptionOrNull()?.message ?: "Failed to save event"
            return false
        }
        return true
    }
}
