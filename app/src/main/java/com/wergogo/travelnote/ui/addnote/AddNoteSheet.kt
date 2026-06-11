package com.wergogo.travelnote.ui.addnote

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.ui.components.AppTopBar
import com.wergogo.travelnote.ui.components.PrimaryButton
import kotlinx.coroutines.launch

@Composable
fun AddNoteSheet(
    tripId: String,
    dateKey: String,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TravelNoteApplication
    val vm: AddNoteViewModel = viewModel(key = "$tripId/$dateKey") {
        AddNoteViewModel(app.container.dayRepository, tripId, dateKey)
    }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { AppTopBar("Add Note", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = vm.text,
                onValueChange = { vm.text = it },
                label = { Text("Write your note...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                maxLines = 10
            )

            if (vm.errorMessage != null) {
                Text(
                    text = vm.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            PrimaryButton(
                text = "Save Note",
                enabled = !vm.isLoading,
                onClick = { coroutineScope.launch { if (vm.save()) onSaved() } }
            )
        }
    }
}
