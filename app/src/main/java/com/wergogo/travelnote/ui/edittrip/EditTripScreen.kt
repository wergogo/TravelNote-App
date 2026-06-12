package com.wergogo.travelnote.ui.edittrip

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.data.model.TripVisibility
import com.wergogo.travelnote.ui.components.CoverPicker
import com.wergogo.travelnote.ui.createtrip.TripFormContent
import com.wergogo.travelnote.ui.theme.TravelnoteTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTripScreen(
    tripId: String,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TravelNoteApplication
    val vm: EditTripViewModel = viewModel(key = tripId) {
        EditTripViewModel(app.container.tripRepository, tripId)
    }
    val coroutineScope = rememberCoroutineScope()
    var showCoverPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf<String?>(null) }  // "start" or "end"

    TripFormContent(
        title = "Edit Trip",
        coverId = vm.coverId,
        name = vm.name,
        onNameChange = { vm.name = it },
        destination = vm.destination,
        onDestinationChange = { vm.destination = it },
        startDate = vm.startDate,
        onStartDateClick = { showDatePicker = "start" },
        endDate = vm.endDate,
        onEndDateClick = { showDatePicker = "end" },
        totalBudget = vm.totalBudget,
        onTotalBudgetChange = { vm.totalBudget = it },
        currency = vm.currency,
        onCurrencyChange = { vm.currency = it },
        visibility = vm.visibility,
        onVisibilityChange = { vm.visibility = it },
        errorMessage = vm.errorMessage,
        isLoading = vm.isLoading,
        buttonText = "Save Changes",
        onSubmit = {
            coroutineScope.launch {
                if (vm.saveChanges()) onSaved()
            }
        },
        onCoverClick = { showCoverPicker = true },
        onBack = onBack
    )

    // Date picker dialog
    if (showDatePicker != null) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = null },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val formatted = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.of("UTC"))
                            .toLocalDate()
                            .toString()  // "yyyy-MM-dd"
                        if (showDatePicker == "start") vm.startDate = formatted
                        else vm.endDate = formatted
                    }
                    showDatePicker = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = null }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Cover picker
    if (showCoverPicker) {
        CoverPicker(
            selected = vm.coverId,
            onSelect = { vm.coverId = it; showCoverPicker = false },
            onDismiss = { showCoverPicker = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditTripScreenPreview() {
    TravelnoteTheme {
        TripFormContent(
            title = "Edit Trip",
            coverId = "cover_city",
            name = "Summer in Paris",
            onNameChange = {},
            destination = "Paris, France",
            onDestinationChange = {},
            startDate = "2026-07-01",
            onStartDateClick = {},
            endDate = "2026-07-10",
            onEndDateClick = {},
            totalBudget = "3000",
            onTotalBudgetChange = {},
            currency = "EUR",
            onCurrencyChange = {},
            visibility = TripVisibility.OPEN,
            onVisibilityChange = {},
            errorMessage = null,
            isLoading = false,
            buttonText = "Save Changes",
            onSubmit = {},
            onCoverClick = {},
            onBack = {}
        )
    }
}
