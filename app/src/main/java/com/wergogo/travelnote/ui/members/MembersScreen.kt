package com.wergogo.travelnote.ui.members

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.ui.components.AppTopBar
import com.wergogo.travelnote.util.DefaultImages

@Composable
fun MembersScreen(
    tripId: String,
    onBack: () -> Unit,
    onTripLeft: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TravelNoteApplication
    val vm: MembersViewModel = viewModel(key = tripId) {
        MembersViewModel(app.container.authRepository, app.container.tripRepository, tripId)
    }
    val trip by vm.trip.collectAsState()
    val members by vm.members.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Trip?") },
            text = { Text("This will permanently delete the trip for all members.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    coroutineScope.launch { vm.deleteTrip(); onTripLeft() }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Trip?") },
            text = { Text("You will no longer have access to this trip.") },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveDialog = false
                    coroutineScope.launch { vm.leaveTrip(); onTripLeft() }
                }) { Text("Leave") }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = { AppTopBar("Members", onBack = onBack) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Join Code section — only visible to trip owner
            if (vm.currentUserId == trip?.ownerId) {
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Join Code", style = MaterialTheme.typography.titleSmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = trip?.joinCode ?: "",
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(trip?.joinCode ?: ""))
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                }
                                IconButton(onClick = {
                                    coroutineScope.launch { vm.regenerateJoinCode() }
                                }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
                                }
                            }
                        }
                    }
                }
            }

            // Section header
            item {
                Text(
                    text = "${members.size} Members",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            // Member rows
            items(members) { member ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(DefaultImages.avatar(member.avatarId)),
                        contentDescription = member.nickname,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column(Modifier.weight(1f)) {
                        Text(member.nickname, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = member.role,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bottom actions card
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (vm.currentUserId == trip?.ownerId) {
                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Delete Trip")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showLeaveDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Leave Trip")
                            }
                        }
                    }
                }
            }
        }
    }
}
