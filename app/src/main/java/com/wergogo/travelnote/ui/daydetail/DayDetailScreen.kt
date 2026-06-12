package com.wergogo.travelnote.ui.daydetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.data.model.EventType
import com.wergogo.travelnote.data.model.Expense
import com.wergogo.travelnote.data.model.ExpenseCategory
import com.wergogo.travelnote.data.model.Note
import com.wergogo.travelnote.ui.components.AmountText
import com.wergogo.travelnote.ui.components.AppTopBar
import com.wergogo.travelnote.ui.components.AuthorTag
import com.wergogo.travelnote.ui.components.CategoryChip
import com.wergogo.travelnote.ui.components.EmptyState
import com.wergogo.travelnote.ui.theme.TravelnoteTheme
import kotlinx.coroutines.launch

@Composable
fun DayDetailScreen(
    tripId: String,
    dateKey: String,
    onAddEvent: () -> Unit,
    onEditEvent: (eventId: String) -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (expenseId: String) -> Unit,
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

    DayDetailContent(
        dateKey = dateKey,
        currency = trip?.currency ?: "USD",
        events = events,
        expenses = expenses,
        notes = notes,
        onAddEvent = onAddEvent,
        onEditEvent = onEditEvent,
        onAddExpense = onAddExpense,
        onEditExpense = onEditExpense,
        onAddNote = onAddNote,
        onDeleteEvent = { id -> coroutineScope.launch { vm.deleteEvent(id) } },
        onDeleteExpense = { id -> coroutineScope.launch { vm.deleteExpense(id) } },
        onDeleteNote = { id -> coroutineScope.launch { vm.deleteNote(id) } },
        onBack = onBack
    )
}

@Composable
internal fun DayDetailContent(
    dateKey: String,
    currency: String,
    events: List<com.wergogo.travelnote.data.model.Event>,
    expenses: List<Expense>,
    notes: List<Note>,
    onAddEvent: () -> Unit,
    onEditEvent: (eventId: String) -> Unit,
    onAddExpense: () -> Unit,
    onEditExpense: (expenseId: String) -> Unit,
    onAddNote: () -> Unit,
    onDeleteEvent: (String) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onBack: () -> Unit
) {
    // Delete confirmation state
    var deleteEventId by remember { mutableStateOf<String?>(null) }
    var deleteExpenseId by remember { mutableStateOf<String?>(null) }
    var deleteNoteId by remember { mutableStateOf<String?>(null) }

    // Delete confirmation dialogs
    if (deleteEventId != null) {
        AlertDialog(
            onDismissRequest = { deleteEventId = null },
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to delete this event?") },
            confirmButton = {
                TextButton(onClick = {
                    val id = deleteEventId!!
                    deleteEventId = null
                    onDeleteEvent(id)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteEventId = null }) { Text("Cancel") }
            }
        )
    }

    if (deleteExpenseId != null) {
        AlertDialog(
            onDismissRequest = { deleteExpenseId = null },
            title = { Text("Delete Expense") },
            text = { Text("Are you sure you want to delete this expense?") },
            confirmButton = {
                TextButton(onClick = {
                    val id = deleteExpenseId!!
                    deleteExpenseId = null
                    onDeleteExpense(id)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteExpenseId = null }) { Text("Cancel") }
            }
        )
    }

    if (deleteNoteId != null) {
        AlertDialog(
            onDismissRequest = { deleteNoteId = null },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note?") },
            confirmButton = {
                TextButton(onClick = {
                    val id = deleteNoteId!!
                    deleteNoteId = null
                    onDeleteNote(id)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteNoteId = null }) { Text("Cancel") }
            }
        )
    }

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
            contentPadding = PaddingValues(16.dp),
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
                                Column(Modifier.weight(1f)) {
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
                                    if (event.cost > 0) {
                                        AmountText(event.cost, currency)
                                    }
                                }
                                Row {
                                    IconButton(onClick = { onEditEvent(event.id) }) {
                                        Icon(Icons.Default.Edit, "Edit")
                                    }
                                    IconButton(onClick = { deleteEventId = event.id }) {
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
                                    AmountText(expense.amount, currency)
                                    IconButton(onClick = { onEditExpense(expense.id) }) {
                                        Icon(Icons.Default.Edit, "Edit")
                                    }
                                    IconButton(onClick = { deleteExpenseId = expense.id }) {
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
                                IconButton(onClick = { deleteNoteId = note.id }) {
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

@Preview(showBackground = true)
@Composable
private fun DayDetailScreenPreview() {
    TravelnoteTheme {
        DayDetailContent(
            dateKey = "2026-07-02",
            currency = "EUR",
            events = listOf(
                com.wergogo.travelnote.data.model.Event(
                    id = "e1",
                    type = EventType.FLIGHT,
                    time = "08:00",
                    cost = 450.0,
                    flightNumber = "AF007",
                    depAirport = "JFK",
                    arrAirport = "CDG",
                    title = "",
                    locationName = "",
                    authorId = "user1",
                    authorNickname = "Alice",
                    authorAvatarId = "avatar_01",
                    createdAt = System.currentTimeMillis()
                ),
                com.wergogo.travelnote.data.model.Event(
                    id = "e2",
                    type = EventType.ACTIVITY,
                    time = "14:30",
                    cost = 25.0,
                    flightNumber = "",
                    depAirport = "",
                    arrAirport = "",
                    title = "Visit Eiffel Tower",
                    locationName = "Champ de Mars",
                    authorId = "user2",
                    authorNickname = "Bob",
                    authorAvatarId = "avatar_03",
                    createdAt = System.currentTimeMillis()
                )
            ),
            expenses = listOf(
                Expense(
                    id = "exp1",
                    category = ExpenseCategory.FOOD,
                    amount = 42.50,
                    description = "Dinner at Le Comptoir",
                    placeLabel = "Le Comptoir du Pantheon",
                    authorId = "user1",
                    authorNickname = "Alice",
                    authorAvatarId = "avatar_01",
                    createdAt = System.currentTimeMillis()
                ),
                Expense(
                    id = "exp2",
                    category = ExpenseCategory.TRANSPORT,
                    amount = 15.0,
                    description = "Metro tickets",
                    authorId = "user2",
                    authorNickname = "Bob",
                    authorAvatarId = "avatar_03",
                    createdAt = System.currentTimeMillis()
                )
            ),
            notes = listOf(
                Note(
                    id = "n1",
                    text = "Remember to book the museum tickets in advance!",
                    authorId = "user1",
                    authorNickname = "Alice",
                    authorAvatarId = "avatar_01",
                    createdAt = System.currentTimeMillis()
                )
            ),
            onAddEvent = {},
            onEditEvent = {},
            onAddExpense = {},
            onEditExpense = {},
            onAddNote = {},
            onDeleteEvent = {},
            onDeleteExpense = {},
            onDeleteNote = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DayDetailScreenEmptyPreview() {
    TravelnoteTheme {
        DayDetailContent(
            dateKey = "2026-07-03",
            currency = "EUR",
            events = emptyList(),
            expenses = emptyList(),
            notes = emptyList(),
            onAddEvent = {},
            onEditEvent = {},
            onAddExpense = {},
            onEditExpense = {},
            onAddNote = {},
            onDeleteEvent = {},
            onDeleteExpense = {},
            onDeleteNote = {},
            onBack = {}
        )
    }
}
