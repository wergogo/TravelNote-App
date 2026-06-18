package com.wergogo.travelnote.ui.tripoverview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.data.model.EventType
import com.wergogo.travelnote.data.model.Member
import com.wergogo.travelnote.data.model.Trip
import com.wergogo.travelnote.data.model.TripVisibility
import com.wergogo.travelnote.ui.components.AppTopBar
import com.wergogo.travelnote.ui.components.BudgetBar
import com.wergogo.travelnote.ui.components.EmptyState
import com.wergogo.travelnote.ui.components.MemberAvatarRow
import com.wergogo.travelnote.ui.theme.Success
import com.wergogo.travelnote.ui.theme.TravelnoteTheme
import com.wergogo.travelnote.util.DefaultImages

@Composable
fun TripOverviewScreen(
    tripId: String,
    onDayClick: (dateKey: String) -> Unit,
    onEditTrip: () -> Unit,
    onMembersClick: () -> Unit,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TravelNoteApplication
    val vm: TripOverviewViewModel = viewModel(key = tripId) {
        TripOverviewViewModel(app.container.authRepository, app.container.tripRepository, app.container.dayRepository, tripId)
    }
    val trip by vm.trip.collectAsState()
    val members by vm.members.collectAsState()
    val spent by vm.spent.collectAsState()
    val daySummaries by vm.daySummaries.collectAsState()
    val currentUid = vm.currentUserId
    val coroutineScope = rememberCoroutineScope()

    TripOverviewContent(
        trip = trip,
        members = members,
        spent = spent,
        daySummaries = daySummaries,
        isOwner = currentUid == trip?.ownerId,
        onDayClick = onDayClick,
        onEditTrip = onEditTrip,
        onMembersClick = onMembersClick,
        onBack = onBack
    )
}

@Composable
internal fun TripOverviewContent(
    trip: Trip?,
    members: List<Member>,
    spent: Double,
    daySummaries: Map<String, DaySummary>,
    isOwner: Boolean,
    onDayClick: (dateKey: String) -> Unit,
    onEditTrip: () -> Unit,
    onMembersClick: () -> Unit,
    onBack: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = trip?.name ?: "Trip",
                onBack = onBack,
                actions = {
                    if (isOwner) {
                        IconButton(onClick = onEditTrip) {
                            Icon(Icons.Default.Edit, "Edit trip")
                        }
                    }
                    IconButton(onClick = onMembersClick) {
                        Icon(Icons.Default.Group, "Members")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (trip == null) {
            EmptyState(
                message = "Loading trip...",
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        } else {
            val dateKeys = generateDateKeys(trip.startDate, trip.endDate)

            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Cover image
                item {
                    Image(
                        painter = painterResource(DefaultImages.cover(trip.coverId)),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                }

                // 2. Trip info card
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = trip.destination,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${trip.startDate} \u2013 ${trip.endDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Code: ${trip.joinCode}") }
                                )
                            }
                            MemberAvatarRow(members = members)
                            BudgetBar(
                                spent = spent,
                                total = trip.totalBudget,
                                currency = trip.currency
                            )
                        }
                    }
                }

                // 3. Section header with expand/collapse toggle
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Itinerary",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                contentDescription = if (expanded) "Collapse details" else "Expand details"
                            )
                        }
                    }
                }

                // 4. Day tiles with optional summary
                items(dateKeys) { dateKey ->
                    val summary = daySummaries[dateKey]

                    ElevatedCard(
                        onClick = { onDayClick(dateKey) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            // Date header row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(dateKey, style = MaterialTheme.typography.bodyMedium)
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }

                            // Expandable detail section
                            AnimatedVisibility(visible = expanded && summary != null) {
                                summary?.let { s ->
                                    val hasContent = s.events.isNotEmpty() || s.totalExpense > 0
                                    if (hasContent) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Event summaries
                                            s.events.forEach { event ->
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = event.time,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.width(48.dp)
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(
                                                        text = if (event.type == EventType.FLIGHT)
                                                            "${event.depAirport} \u2192 ${event.arrAirport}"
                                                        else
                                                            event.title,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            // Total expense for the day
                                            if (s.totalExpense > 0) {
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = "${trip.currency} ${"%.2f".format(s.totalExpense)} spent",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Success
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TripOverviewExpandedPreview() {
    TravelnoteTheme {
        TripOverviewContent(
            trip = Trip(
                id = "trip1",
                name = "Summer in Paris",
                destination = "Paris, France",
                startDate = "2026-07-01",
                endDate = "2026-07-03",
                totalBudget = 3000.0,
                currency = "EUR",
                visibility = TripVisibility.OPEN,
                joinCode = "PAR123",
                coverId = "cover_city",
                ownerId = "user1",
                memberIds = listOf("user1", "user2"),
                createdAt = System.currentTimeMillis()
            ),
            members = listOf(
                Member(userId = "user1", nickname = "Alice", avatarId = "avatar_01", role = "owner", joinedAt = System.currentTimeMillis()),
                Member(userId = "user2", nickname = "Bob", avatarId = "avatar_03", role = "member", joinedAt = System.currentTimeMillis())
            ),
            spent = 1250.0,
            daySummaries = mapOf(
                "2026-07-01" to DaySummary(
                    events = listOf(
                        com.wergogo.travelnote.data.model.Event(id = "e1", type = EventType.FLIGHT, time = "08:00", depAirport = "JFK", arrAirport = "CDG", flightNumber = "AF007", title = "", locationName = "", authorId = "u1", authorNickname = "Alice", authorAvatarId = "avatar_01", createdAt = 0L),
                        com.wergogo.travelnote.data.model.Event(id = "e2", type = EventType.ACTIVITY, time = "18:00", title = "Check in hotel", locationName = "", depAirport = "", arrAirport = "", flightNumber = "", authorId = "u1", authorNickname = "Alice", authorAvatarId = "avatar_01", createdAt = 0L)
                    ),
                    totalExpense = 85.0
                ),
                "2026-07-02" to DaySummary(
                    events = listOf(
                        com.wergogo.travelnote.data.model.Event(id = "e3", type = EventType.ACTIVITY, time = "10:00", title = "Louvre Museum", locationName = "", depAirport = "", arrAirport = "", flightNumber = "", authorId = "u2", authorNickname = "Bob", authorAvatarId = "avatar_03", createdAt = 0L),
                        com.wergogo.travelnote.data.model.Event(id = "e4", type = EventType.ACTIVITY, time = "14:30", title = "Eiffel Tower", locationName = "", depAirport = "", arrAirport = "", flightNumber = "", authorId = "u1", authorNickname = "Alice", authorAvatarId = "avatar_01", createdAt = 0L)
                    ),
                    totalExpense = 120.50
                ),
                "2026-07-03" to DaySummary(
                    events = emptyList(),
                    totalExpense = 0.0
                )
            ),
            isOwner = true,
            onDayClick = {},
            onEditTrip = {},
            onMembersClick = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TripOverviewCollapsedPreview() {
    TravelnoteTheme {
        TripOverviewContent(
            trip = Trip(
                id = "trip1",
                name = "Tokyo Adventure",
                destination = "Tokyo, Japan",
                startDate = "2026-08-15",
                endDate = "2026-08-17",
                totalBudget = 5000.0,
                currency = "USD",
                visibility = TripVisibility.LOCKED,
                joinCode = "TKY456",
                coverId = "cover_mountains",
                ownerId = "other_user",
                memberIds = listOf("other_user", "user1"),
                createdAt = System.currentTimeMillis()
            ),
            members = listOf(
                Member(userId = "other_user", nickname = "Host", avatarId = "avatar_02", role = "owner", joinedAt = System.currentTimeMillis()),
                Member(userId = "user1", nickname = "Me", avatarId = "avatar_04", role = "member", joinedAt = System.currentTimeMillis())
            ),
            spent = 4800.0,
            daySummaries = emptyMap(),
            isOwner = false,
            onDayClick = {},
            onEditTrip = {},
            onMembersClick = {},
            onBack = {}
        )
    }
}
