package com.wergogo.travelnote.ui.daydetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.data.model.EventType
import com.wergogo.travelnote.ui.components.AmountText
import com.wergogo.travelnote.ui.components.AppTopBar
import com.wergogo.travelnote.ui.components.AuthorTag
import com.wergogo.travelnote.ui.components.CategoryChip
import com.wergogo.travelnote.ui.components.EmptyState
import kotlinx.coroutines.launch

@Composable
fun DayDetailScreen(
    tripId: String,
    dateKey: String,
    onAddEvent: () -> Unit,
    onEditEvent: (eventId: String) -> Unit,
    onAddExpense: () -> Unit,
    onAddNote: () -> Unit,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TravelNoteApplication
    val vm: DayDetailViewModel = viewModel(key = "$tripId/$dateKey") {
        DayDetailViewModel(app.container.tripRepository, app.container.dayRepository, tripId, dateKey)
    }
    val trip by vm.trip.collectAsState()
    val day by vm.day.collectAsState()
    val events by vm.events.collectAsState()
    val expenses by vm.expenses.collectAsState()
    val notes by vm.notes.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppTopBar(
                title = dateKey,
                onBack = onBack,
                actions = {
                    IconButton(onClick = onAddEvent) { Icon(Icons.Default.Event, "Add event") }
                    IconButton(onClick = onAddExpense) { Icon(Icons.Default.AttachMoney, "Add expense") }
                    IconButton(onClick = onAddNote) { Icon(Icons.Default.EditNote, "Add note") }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Events section
            if (events.isNotEmpty()) {
                item {
                    Text("Events", style = MaterialTheme.typography.titleSmall)
                }
                items(events) { event ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        event.time,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (event.type == EventType.FLIGHT) {
                                        Text(
                                            "${event.depAirport} \u2192 ${event.arrAirport}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(event.flightNumber, style = MaterialTheme.typography.bodySmall)
                                    } else {
                                        Text(event.title, style = MaterialTheme.typography.bodyMedium)
                                        if (event.locationName.isNotBlank()) {
                                            Text(event.locationName, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                Row {
                                    IconButton(onClick = { onEditEvent(event.id) }) {
                                        Icon(Icons.Default.Edit, "Edit")
                                    }
                                    IconButton(onClick = {
                                        coroutineScope.launch { vm.deleteEvent(event.id) }
                                    }) {
                                        Icon(Icons.Default.Delete, "Delete")
                                    }
                                }
                            }
                            AuthorTag(event.authorAvatarId, event.authorNickname, event.createdAt)
                        }
                    }
                }
            }

            // Expenses section
            if (expenses.isNotEmpty()) {
                item {
                    Text("Expenses", style = MaterialTheme.typography.titleSmall)
                }
                items(expenses) { expense ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    CategoryChip(expense.category)
                                    Text(expense.description, style = MaterialTheme.typography.bodySmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AmountText(expense.amount, trip?.currency ?: "USD")
                                    IconButton(onClick = {
                                        coroutineScope.launch { vm.deleteExpense(expense.id) }
                                    }) {
                                        Icon(Icons.Default.Delete, "Delete")
                                    }
                                }
                            }
                            AuthorTag(expense.authorAvatarId, expense.authorNickname, expense.createdAt)
                        }
                    }
                }
            }

            // Notes section
            if (notes.isNotEmpty()) {
                item {
                    Text("Notes", style = MaterialTheme.typography.titleSmall)
                }
                items(notes) { note ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    note.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    coroutineScope.launch { vm.deleteNote(note.id) }
                                }) {
                                    Icon(Icons.Default.Delete, "Delete")
                                }
                            }
                            AuthorTag(note.authorAvatarId, note.authorNickname, note.createdAt)
                        }
                    }
                }
            }

            // Empty state
            if (events.isEmpty() && expenses.isEmpty() && notes.isEmpty()) {
                item {
                    EmptyState("Nothing here yet \u2014 add an event, expense, or note!")
                }
            }
        }
    }
}
