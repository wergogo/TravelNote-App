package com.wergogo.travelnote.ui.addexpense

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wergogo.travelnote.data.model.ExpenseCategory
import com.wergogo.travelnote.data.model.ExpenseDraft
import com.wergogo.travelnote.data.model.Member
import com.wergogo.travelnote.data.repository.AuthRepository
import com.wergogo.travelnote.data.repository.DayRepository
import com.wergogo.travelnote.data.repository.TripRepository
import com.wergogo.travelnote.util.amountInputText
import com.wergogo.travelnote.util.parseAmount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddExpenseViewModel(
    private val dayRepo: DayRepository,
    private val tripRepo: TripRepository,
    private val authRepo: AuthRepository,
    private val tripId: String,
    private val dateKey: String,
    private val editExpenseId: String?
) : ViewModel() {

    var category by mutableStateOf(ExpenseCategory.OTHER)
    var amount by mutableStateOf("")
    var description by mutableStateOf("")
    var lat by mutableStateOf<Double?>(null)
    var lng by mutableStateOf<Double?>(null)
    var placeLabel by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // Payer selection
    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    var selectedPayerId by mutableStateOf(authRepo.currentUserId ?: "")
    var selectedPayerNickname by mutableStateOf("")
    var selectedPayerAvatarId by mutableStateOf("")

    val isEditMode: Boolean get() = editExpenseId != null

    init {
        // Load trip members
        viewModelScope.launch {
            tripRepo.observeMembers(tripId).collect { memberList ->
                _members.value = memberList
                if (selectedPayerNickname.isEmpty()) {
                    memberList.find { it.userId == selectedPayerId }?.let {
                        selectedPayerNickname = it.nickname
                        selectedPayerAvatarId = it.avatarId
                    }
                }
            }
        }

        if (editExpenseId != null) {
            viewModelScope.launch {
                dayRepo.observeExpenses(tripId, dateKey).first()
                    .find { it.id == editExpenseId }
                    ?.let { exp ->
                        category = exp.category
                        amount = amountInputText(exp.amount)
                        description = exp.description
                        lat = exp.lat
                        lng = exp.lng
                        placeLabel = exp.placeLabel
                        // Restore the original payer
                        selectedPayerId = exp.authorId
                        selectedPayerNickname = exp.authorNickname
                        selectedPayerAvatarId = exp.authorAvatarId
                    }
            }
        }
    }

    fun selectPayer(member: Member) {
        selectedPayerId = member.userId
        selectedPayerNickname = member.nickname
        selectedPayerAvatarId = member.avatarId
    }

    suspend fun fetchLocation(context: android.content.Context) {
        errorMessage = null
        val loc = com.wergogo.travelnote.util.LocationHelper.getCurrentLocation(context)
        if (loc != null) {
            lat = loc.first
            lng = loc.second
            placeLabel = "%.4f, %.4f".format(loc.first, loc.second)
        } else {
            errorMessage = "Could not get location. On emulator, set a location via Extended Controls."
        }
    }

    suspend fun save(): Boolean {
        val amt = parseAmount(amount)
        if (amt == null || amt <= 0) { errorMessage = "Enter a valid amount"; return false }
        isLoading = true
        errorMessage = null

        val isSelf = selectedPayerId == authRepo.currentUserId
        val result = dayRepo.addExpense(
            tripId, dateKey,
            ExpenseDraft(
                category = category,
                amount = amt,
                description = description.trim(),
                lat = lat,
                lng = lng,
                placeLabel = placeLabel,
                expenseId = editExpenseId,
                authorId = if (!isSelf) selectedPayerId else null,
                authorNickname = if (!isSelf) selectedPayerNickname else null,
                authorAvatarId = if (!isSelf) selectedPayerAvatarId else null
            )
        )
        isLoading = false
        if (result.isFailure) {
            errorMessage = result.exceptionOrNull()?.message ?: "Failed to save expense"
            return false
        }
        return true
    }
}
