package com.wergogo.travelnote.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.data.model.Trip
import com.wergogo.travelnote.data.model.TripVisibility
import com.wergogo.travelnote.ui.components.AppTopBar
import com.wergogo.travelnote.ui.components.BottomNavTab
import com.wergogo.travelnote.ui.components.EmptyState
import com.wergogo.travelnote.ui.components.PrimaryButton
import com.wergogo.travelnote.ui.components.TravelNoteBottomNav
import com.wergogo.travelnote.ui.components.TripCard
import com.wergogo.travelnote.ui.theme.TravelnoteTheme

@Composable
fun HomeScreen(
    onTripClick: (String) -> Unit,
    onCreateTrip: () -> Unit,
    onJoinTrip: () -> Unit,
    onProfileClick: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TravelNoteApplication
    val vm: HomeViewModel = viewModel {
        HomeViewModel(app.container.tripRepository)
    }
    val trips by vm.trips.collectAsState()
    val isLoading by vm.isLoading.collectAsState()

    HomeContent(
        trips = trips,
        isLoading = isLoading,
        onTripClick = onTripClick,
        onCreateTrip = onCreateTrip,
        onJoinTrip = onJoinTrip,
        onProfileClick = onProfileClick
    )
}

@Composable
internal fun HomeContent(
    trips: List<Trip>,
    isLoading: Boolean,
    onTripClick: (String) -> Unit,
    onCreateTrip: () -> Unit,
    onJoinTrip: () -> Unit,
    onProfileClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(BottomNavTab.TRIPS) }

    Scaffold(
        topBar = {
            AppTopBar(title = "My Trips", actions = {
                IconButton(onClick = onCreateTrip) {
                    Icon(Icons.Default.Add, contentDescription = "New trip")
                }
            })
        },
        bottomBar = {
            TravelNoteBottomNav(
                selected = selectedTab,
                onTripsClick = { selectedTab = BottomNavTab.TRIPS },
                onProfileClick = { selectedTab = BottomNavTab.PROFILE; onProfileClick() }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Join Trip") },
                icon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                onClick = onJoinTrip
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                trips.isEmpty() -> {
                    EmptyState(
                        message = "No trips yet",
                        modifier = Modifier.align(Alignment.Center),
                        action = {
                            PrimaryButton("Create a Trip", onCreateTrip)
                        }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(trips) { trip ->
                            TripCard(
                                trip = trip,
                                onClick = { onTripClick(trip.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenWithTripsPreview() {
    TravelnoteTheme {
        HomeContent(
            trips = listOf(
                Trip(
                    id = "1",
                    name = "Summer in Paris",
                    destination = "Paris, France",
                    startDate = "2026-07-01",
                    endDate = "2026-07-10",
                    totalBudget = 3000.0,
                    currency = "EUR",
                    visibility = TripVisibility.OPEN,
                    joinCode = "ABC123",
                    coverId = "cover_city",
                    ownerId = "user1",
                    memberIds = listOf("user1", "user2"),
                    createdAt = System.currentTimeMillis()
                ),
                Trip(
                    id = "2",
                    name = "Tokyo Adventure",
                    destination = "Tokyo, Japan",
                    startDate = "2026-08-15",
                    endDate = "2026-08-25",
                    totalBudget = 5000.0,
                    currency = "USD",
                    visibility = TripVisibility.LOCKED,
                    joinCode = "XYZ789",
                    coverId = "cover_mountains",
                    ownerId = "user1",
                    memberIds = listOf("user1", "user2", "user3"),
                    createdAt = System.currentTimeMillis()
                )
            ),
            isLoading = false,
            onTripClick = {},
            onCreateTrip = {},
            onJoinTrip = {},
            onProfileClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    TravelnoteTheme {
        HomeContent(
            trips = emptyList(),
            isLoading = false,
            onTripClick = {},
            onCreateTrip = {},
            onJoinTrip = {},
            onProfileClick = {}
        )
    }
}
