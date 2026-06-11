package com.wergogo.travelnote.ui.createtrip

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.data.model.TripVisibility
import com.wergogo.travelnote.ui.components.AppTopBar
import com.wergogo.travelnote.ui.components.CoverPicker
import com.wergogo.travelnote.ui.components.PrimaryButton
import com.wergogo.travelnote.util.DefaultImages
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripScreen(
    onTripCreated: (String) -> Unit,  // receives tripId
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TravelNoteApplication
    val vm: CreateTripViewModel = viewModel {
        CreateTripViewModel(app.container.authRepository, app.container.tripRepository)
    }
    val coroutineScope = rememberCoroutineScope()
    var showCoverPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf<String?>(null) }  // "start" or "end"

    Scaffold(
        topBar = { AppTopBar("New Trip", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Cover image preview + change button
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                Image(
                    painter = painterResource(DefaultImages.cover(vm.coverId)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                TextButton(
                    onClick = { showCoverPicker = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) { Text("Change Cover", color = Color.White) }
            }

            // 2. Trip name
            OutlinedTextField(
                value = vm.name,
                onValueChange = { vm.name = it },
                label = { Text("Trip Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Destination
            OutlinedTextField(
                value = vm.destination,
                onValueChange = { vm.destination = it },
                label = { Text("Destination") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 4. Date row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = vm.startDate,
                    onValueChange = {},
                    label = { Text("Start") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pick start date"
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDatePicker = "start" }
                )
                OutlinedTextField(
                    value = vm.endDate,
                    onValueChange = {},
                    label = { Text("End") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Pick end date"
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDatePicker = "end" }
                )
            }

            // 5. Budget row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = vm.totalBudget,
                    onValueChange = { vm.totalBudget = it },
                    label = { Text("Budget") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = vm.currency,
                    onValueChange = { vm.currency = it },
                    label = { Text("Currency") },
                    singleLine = true,
                    modifier = Modifier.width(88.dp)
                )
            }

            // 6. Visibility toggle
            Text("Visibility", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = vm.visibility == TripVisibility.OPEN,
                    onClick = { vm.visibility = TripVisibility.OPEN },
                    label = { Text("Open") }
                )
                FilterChip(
                    selected = vm.visibility == TripVisibility.LOCKED,
                    onClick = { vm.visibility = TripVisibility.LOCKED },
                    label = { Text("Locked") }
                )
            }

            // 7. Error message
            if (vm.errorMessage != null) {
                Text(
                    text = vm.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // 8. Create button
            PrimaryButton(
                text = "Create Trip",
                enabled = !vm.isLoading,
                onClick = {
                    coroutineScope.launch {
                        val id = vm.createTrip()
                        if (id != null) onTripCreated(id)
                    }
                }
            )
        }
    }

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
            selectedKey = vm.coverId,
            onSelect = { vm.coverId = it; showCoverPicker = false },
            onDismiss = { showCoverPicker = false }
        )
    }
}
