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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.ui.components.AppTopBar
import com.wergogo.travelnote.ui.components.BottomNavTab
import com.wergogo.travelnote.ui.components.EmptyState
import com.wergogo.travelnote.ui.components.PrimaryButton
import com.wergogo.travelnote.ui.components.TravelNoteBottomNav
import com.wergogo.travelnote.ui.components.TripCard

@Composable
fun HomeScreen(
    onTripClick: (String) -> Unit,
    onCreateTrip: () -> Unit,
    onJoinTrip: () -> Unit,
    onProfileClick: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TravelNoteApplication
    val vm: HomeViewModel = viewModel {
        HomeViewModel(app.container.authRepository, app.container.tripRepository)
    }
    val trips by vm.trips.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
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
            TravelNoteBottomNav(selectedTab = selectedTab, onTabSelected = { tab ->
                selectedTab = tab
                if (tab == BottomNavTab.PROFILE) onProfileClick()
            })
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
                                onClick = { onTripClick(trip.tripId) }
                            )
                        }
                    }
                }
            }
        }
    }
}
