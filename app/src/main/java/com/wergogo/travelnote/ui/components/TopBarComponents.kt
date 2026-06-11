package com.wergogo.travelnote.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.wergogo.travelnote.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = { actions() }
    )
}

enum class BottomNavTab { TRIPS, PROFILE }

@Composable
fun TravelNoteBottomNav(
    selected: BottomNavTab,
    onTripsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            selected = selected == BottomNavTab.TRIPS,
            onClick = onTripsClick,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_trips),
                    contentDescription = "Trips"
                )
            },
            label = { Text("Trips") }
        )
        NavigationBarItem(
            selected = selected == BottomNavTab.PROFILE,
            onClick = onProfileClick,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_profile),
                    contentDescription = "Profile"
                )
            },
            label = { Text("Profile") }
        )
    }
}
