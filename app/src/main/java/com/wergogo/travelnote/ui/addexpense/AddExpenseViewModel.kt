package com.wergogo.travelnote.ui.addexpense

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wergogo.travelnote.data.model.ExpenseCategory
import com.wergogo.travelnote.data.model.ExpenseDraft
import com.wergogo.travelnote.data.repository.DayRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddExpenseViewModel(
    private val dayRepo: DayRepository,
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

    val isEditMode: Boolean get() = editExpenseId != null

    init {
        if (editExpenseId != null) {
            viewModelScope.launch {
                dayRepo.observeExpenses(tripId, dateKey).first()
                    .find { it.id == editExpenseId }
                    ?.let { exp ->
                        category = exp.category
                        amount = exp.amount.toString()
                        description = exp.description
                        lat = exp.lat
                        lng = exp.lng
                        placeLabel = exp.placeLabel
                    }
            }
        }
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
        val amt = amount.toDoubleOrNull()
        if (amt == null || amt <= 0) { errorMessage = "Enter a valid amount"; return false }
        isLoading = true
        errorMessage = null
        val result = dayRepo.addExpense(
            tripId, dateKey,
            ExpenseDraft(
                category = category,
                amount = amt,
                description = description.trim(),
                lat = lat,
                lng = lng,
                placeLabel = placeLabel,
                expenseId = editExpenseId
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
