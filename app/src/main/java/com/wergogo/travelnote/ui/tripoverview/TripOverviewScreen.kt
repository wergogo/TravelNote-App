package com.wergogo.travelnote.ui.tripoverview

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.ui.components.AppTopBar
import com.wergogo.travelnote.ui.components.BudgetBar
import com.wergogo.travelnote.ui.components.EmptyState
import com.wergogo.travelnote.ui.components.MemberAvatarRow
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
        TripOverviewViewModel(app.container.authRepository, app.container.tripRepository, tripId)
    }
    val trip by vm.trip.collectAsState()
    val members by vm.members.collectAsState()
    val currentUid = vm.currentUserId
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppTopBar(
                title = trip?.name ?: "Trip",
                onBack = onBack,
                actions = {
                    if (currentUid == trip?.ownerId) {
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
            val dateKeys = generateDateKeys(trip?.startDate ?: "", trip?.endDate ?: "")

            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Cover image
                item {
                    Image(
                        painter = painterResource(DefaultImages.cover(trip?.coverId)),
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
                                text = trip?.destination ?: "",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${trip?.startDate} \u2013 ${trip?.endDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("Code: ${trip?.joinCode}") }
                                )
                            }
                            MemberAvatarRow(members = members)
                            BudgetBar(
                                spent = 0.0,
                                total = trip?.totalBudget ?: 0.0,
                                currency = trip?.currency ?: "USD"
                            )
                        }
                    }
                }

                // 3. Section header
                item {
                    Text(
                        text = "Itinerary",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // 4. Day tiles
                items(dateKeys) { dateKey ->
                    ElevatedCard(
                        onClick = { onDayClick(dateKey) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dateKey, style = MaterialTheme.typography.bodyMedium)
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

private fun generateDateKeys(startDate: String, endDate: String): List<String> {
    return try {
        val start = java.time.LocalDate.parse(startDate)
        val end = java.time.LocalDate.parse(endDate)
        generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .map { it.toString() }  // "yyyy-MM-dd"
            .toList()
    } catch (e: Exception) { emptyList() }
}
