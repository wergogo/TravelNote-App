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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.data.model.EventType
import com.wergogo.travelnote.data.model.Member
import com.wergogo.travelnote.ui.components.AppTopBar
import com.wergogo.travelnote.ui.components.PayerSelector
import com.wergogo.travelnote.ui.components.PrimaryButton
import com.wergogo.travelnote.ui.theme.TravelnoteTheme
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
        AddEventViewModel(
            app.container.dayRepository,
            app.container.tripRepository,
            app.container.authRepository,
            tripId, dateKey, eventId
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val members by vm.members.collectAsState()

    AddEventContent(
        isEditMode = vm.isEditMode,
        eventType = vm.eventType,
        onEventTypeChange = { vm.eventType = it },
        time = vm.time,
        onTimeChange = { vm.time = it },
        depAirport = vm.depAirport,
        onDepAirportChange = { vm.depAirport = it.uppercase() },
        arrAirport = vm.arrAirport,
        onArrAirportChange = { vm.arrAirport = it.uppercase() },
        flightNumber = vm.flightNumber,
        onFlightNumberChange = { vm.flightNumber = it },
        title = vm.title,
        onTitleChange = { vm.title = it },
        locationName = vm.locationName,
        onLocationNameChange = { vm.locationName = it },
        cost = vm.cost,
        onCostChange = { vm.cost = it },
        members = members,
        selectedPayerId = vm.selectedPayerId,
        selectedPayerNickname = vm.selectedPayerNickname,
        onPayerSelected = { vm.selectPayer(it) },
        errorMessage = vm.errorMessage,
        isLoading = vm.isLoading,
        onSave = {
            coroutineScope.launch {
                if (vm.save()) onSaved()
            }
        },
        onBack = onBack
    )
}

@Composable
internal fun AddEventContent(
    isEditMode: Boolean,
    eventType: EventType,
    onEventTypeChange: (EventType) -> Unit,
    time: String,
    onTimeChange: (String) -> Unit,
    depAirport: String,
    onDepAirportChange: (String) -> Unit,
    arrAirport: String,
    onArrAirportChange: (String) -> Unit,
    flightNumber: String,
    onFlightNumberChange: (String) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    locationName: String,
    onLocationNameChange: (String) -> Unit,
    cost: String,
    onCostChange: (String) -> Unit,
    members: List<Member> = emptyList(),
    selectedPayerId: String = "",
    selectedPayerNickname: String = "",
    onPayerSelected: (Member) -> Unit = {},
    errorMessage: String?,
    isLoading: Boolean,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = if (isEditMode) "Edit Event" else "New Event",
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
                    selected = eventType == EventType.FLIGHT,
                    onClick = { onEventTypeChange(EventType.FLIGHT) },
                    label = { Text("Flight") }
                )
                FilterChip(
                    selected = eventType == EventType.ACTIVITY,
                    onClick = { onEventTypeChange(EventType.ACTIVITY) },
                    label = { Text("Activity") }
                )
            }

            // 2. Time field
            OutlinedTextField(
                value = time,
                onValueChange = onTimeChange,
                label = { Text("Time (HH:mm)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Flight-specific fields
            if (eventType == EventType.FLIGHT) {
                OutlinedTextField(
                    value = depAirport,
                    onValueChange = onDepAirportChange,
                    label = { Text("Departure (IATA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = arrAirport,
                    onValueChange = onArrAirportChange,
                    label = { Text("Arrival (IATA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = flightNumber,
                    onValueChange = onFlightNumberChange,
                    label = { Text("Flight Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. Activity-specific fields
            if (eventType == EventType.ACTIVITY) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Activity Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = locationName,
                    onValueChange = onLocationNameChange,
                    label = { Text("Location (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 5. Cost field
            OutlinedTextField(
                value = cost,
                onValueChange = onCostChange,
                label = { Text("Cost") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 6. Payer selector
            if (members.size > 1) {
                PayerSelector(
                    members = members,
                    selectedPayerId = selectedPayerId,
                    selectedPayerNickname = selectedPayerNickname,
                    onPayerSelected = onPayerSelected
                )
            }

            // 7. Error message
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // 8. Save button
            PrimaryButton(
                text = "Save Event",
                enabled = !isLoading,
                onClick = onSave
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddEventActivityPreview() {
    TravelnoteTheme {
        AddEventContent(
            isEditMode = false,
            eventType = EventType.ACTIVITY,
            onEventTypeChange = {},
            time = "14:30",
            onTimeChange = {},
            depAirport = "",
            onDepAirportChange = {},
            arrAirport = "",
            onArrAirportChange = {},
            flightNumber = "",
            onFlightNumberChange = {},
            title = "Visit Eiffel Tower",
            onTitleChange = {},
            locationName = "Champ de Mars, Paris",
            onLocationNameChange = {},
            cost = "25.00",
            onCostChange = {},
            members = listOf(
                Member(userId = "me", nickname = "Alice", avatarId = "avatar_01", role = "owner"),
                Member(userId = "u2", nickname = "Bob", avatarId = "avatar_03", role = "member")
            ),
            selectedPayerId = "me",
            selectedPayerNickname = "Alice",
            errorMessage = null,
            isLoading = false,
            onSave = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddEventFlightPreview() {
    TravelnoteTheme {
        AddEventContent(
            isEditMode = false,
            eventType = EventType.FLIGHT,
            onEventTypeChange = {},
            time = "08:00",
            onTimeChange = {},
            depAirport = "JFK",
            onDepAirportChange = {},
            arrAirport = "CDG",
            onArrAirportChange = {},
            flightNumber = "AF007",
            onFlightNumberChange = {},
            title = "",
            onTitleChange = {},
            locationName = "",
            onLocationNameChange = {},
            cost = "450.00",
            onCostChange = {},
            errorMessage = null,
            isLoading = false,
            onSave = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditEventPreview() {
    TravelnoteTheme {
        AddEventContent(
            isEditMode = true,
            eventType = EventType.ACTIVITY,
            onEventTypeChange = {},
            time = "10:00",
            onTimeChange = {},
            depAirport = "",
            onDepAirportChange = {},
            arrAirport = "",
            onArrAirportChange = {},
            flightNumber = "",
            onFlightNumberChange = {},
            title = "Louvre Museum",
            onTitleChange = {},
            locationName = "Rue de Rivoli, Paris",
            onLocationNameChange = {},
            cost = "17.00",
            onCostChange = {},
            errorMessage = null,
            isLoading = false,
            onSave = {},
            onBack = {}
        )
    }
}
