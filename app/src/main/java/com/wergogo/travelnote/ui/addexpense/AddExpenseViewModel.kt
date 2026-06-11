package com.wergogo.travelnote.ui.addexpense

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.wergogo.travelnote.data.model.ExpenseCategory
import com.wergogo.travelnote.data.model.ExpenseDraft
import com.wergogo.travelnote.data.repository.DayRepository

class AddExpenseViewModel(
    private val dayRepo: DayRepository,
    private val tripId: String,
    private val dateKey: String
) : ViewModel() {

    var category by mutableStateOf(ExpenseCategory.OTHER)
    var amount by mutableStateOf("")
    var description by mutableStateOf("")
    var lat by mutableStateOf<Double?>(null)
    var lng by mutableStateOf<Double?>(null)
    var placeLabel by mutableStateOf("")
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    suspend fun fetchLocation(context: android.content.Context) {
        val loc = com.wergogo.travelnote.util.LocationHelper.getCurrentLocation(context)
        if (loc != null) {
            lat = loc.first
            lng = loc.second
            placeLabel = "%.4f, %.4f".format(loc.first, loc.second)
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
                placeLabel = placeLabel
            )
        )
        isLoading = false
        if (result.isFailure) {
            errorMessage = result.exceptionOrNull()?.message ?: "Failed to add expense"
            return false
        }
        return true
    }
}
