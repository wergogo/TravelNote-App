package com.wergogo.travelnote.ui.addevent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.data.model.EventType
import com.wergogo.travelnote.ui.components.AppTopBar
import com.wergogo.travelnote.ui.components.PrimaryButton
import kotlinx.coroutines.launch

@Composable
fun AddEventScreen(
    tripId: String,
    dateKey: String,
    eventId: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TravelNoteApplication
    val vm: AddEventViewModel = viewModel(key = "$tripId/$dateKey/$eventId") {
        AddEventViewModel(app.container.dayRepository, tripId, dateKey, eventId)
    }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (vm.isEditMode) "Edit Event" else "New Event",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Type selector
            Text("Event Type")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = vm.eventType == EventType.FLIGHT,
                    onClick = { vm.eventType = EventType.FLIGHT },
                    label = { Text("Flight") }
                )
                FilterChip(
                    selected = vm.eventType == EventType.ACTIVITY,
                    onClick = { vm.eventType = EventType.ACTIVITY },
                    label = { Text("Activity") }
                )
            }

            // 2. Time field
            OutlinedTextField(
                value = vm.time,
                onValueChange = { vm.time = it },
                label = { Text("Time (HH:mm)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Flight-specific fields
            if (vm.eventType == EventType.FLIGHT) {
                OutlinedTextField(
                    value = vm.depAirport,
                    onValueChange = { vm.depAirport = it.uppercase() },
                    label = { Text("Departure (IATA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = vm.arrAirport,
                    onValueChange = { vm.arrAirport = it.uppercase() },
                    label = { Text("Arrival (IATA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = vm.flightNumber,
                    onValueChange = { vm.flightNumber = it },
                    label = { Text("Flight Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. Activity-specific fields
            if (vm.eventType == EventType.ACTIVITY) {
                OutlinedTextField(
                    value = vm.title,
                    onValueChange = { vm.title = it },
                    label = { Text("Activity Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = vm.locationName,
                    onValueChange = { vm.locationName = it },
                    label = { Text("Location (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 5. Cost field
            OutlinedTextField(
                value = vm.cost,
                onValueChange = { vm.cost = it },
                label = { Text("Cost") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 6. Error message
            if (vm.errorMessage != null) {
                Text(
                    text = vm.errorMessage!!,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // 7. Save button
            PrimaryButton(
                text = "Save Event",
                enabled = !vm.isLoading,
                onClick = {
                    coroutineScope.launch {
                        if (vm.save()) onSaved()
                    }
                }
            )
        }
    }
}
