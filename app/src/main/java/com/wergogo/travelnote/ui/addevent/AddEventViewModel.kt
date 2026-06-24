package com.wergogo.travelnote.ui.addevent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wergogo.travelnote.data.model.EventDraft
import com.wergogo.travelnote.data.model.EventType
import com.wergogo.travelnote.data.model.Member
import com.wergogo.travelnote.data.repository.AuthRepository
import com.wergogo.travelnote.data.repository.DayRepository
import com.wergogo.travelnote.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddEventViewModel(
    private val dayRepo: DayRepository,
    private val tripRepo: TripRepository,
    private val authRepo: AuthRepository,
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

    // Payer selection
    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    var selectedPayerId by mutableStateOf(authRepo.currentUserId ?: "")
    var selectedPayerNickname by mutableStateOf("")
    var selectedPayerAvatarId by mutableStateOf("")

    val isEditMode: Boolean get() = editEventId != null

    init {
        // Load trip members
        viewModelScope.launch {
            tripRepo.observeMembers(tripId).collect { memberList ->
                _members.value = memberList
                // Set default payer nickname from member list if not yet set
                if (selectedPayerNickname.isEmpty()) {
                    memberList.find { it.userId == selectedPayerId }?.let {
                        selectedPayerNickname = it.nickname
                        selectedPayerAvatarId = it.avatarId
                    }
                }
            }
        }

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
                        // Restore the original payer
                        selectedPayerId = ev.authorId
                        selectedPayerNickname = ev.authorNickname
                        selectedPayerAvatarId = ev.authorAvatarId
                    }
            }
        }
    }

    fun selectPayer(member: Member) {
        selectedPayerId = member.userId
        selectedPayerNickname = member.nickname
        selectedPayerAvatarId = member.avatarId
    }

    suspend fun save(): Boolean {
        if (time.isBlank()) { errorMessage = "Time is required"; return false }
        if (!time.trim().matches(Regex("""\d{1,2}:\d{2}"""))) {
            errorMessage = "Time must be in HH:mm format (e.g. 09:30)"
            return false
        }
        val parts = time.trim().split(":")
        val h = parts[0].toIntOrNull() ?: -1
        val m = parts[1].toIntOrNull() ?: -1
        if (h !in 0..23 || m !in 0..59) {
            errorMessage = "Invalid time — hours 0-23, minutes 0-59"
            return false
        }
        if (cost.isNotBlank() && cost.toDoubleOrNull() == null) {
            errorMessage = "Cost must be a valid number"
            return false
        }
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

        // Only set author override if a different member is selected
        val isSelf = selectedPayerId == authRepo.currentUserId
        val draft = EventDraft(
            type = eventType,
            time = time.trim(),
            cost = cost.toDoubleOrNull() ?: 0.0,
            flightNumber = flightNumber.uppercase().trim(),
            depAirport = depAirport.uppercase().trim(),
            arrAirport = arrAirport.uppercase().trim(),
            title = title.trim(),
            locationName = locationName.trim(),
            eventId = editEventId,
            authorId = if (!isSelf) selectedPayerId else null,
            authorNickname = if (!isSelf) selectedPayerNickname else null,
            authorAvatarId = if (!isSelf) selectedPayerAvatarId else null
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
