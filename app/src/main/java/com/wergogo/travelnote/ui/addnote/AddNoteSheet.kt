package com.wergogo.travelnote.ui.addnote

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.ui.components.AppTopBar
import com.wergogo.travelnote.ui.components.PrimaryButton
import com.wergogo.travelnote.ui.theme.TravelnoteTheme
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

    AddNoteContent(
        text = vm.text,
        onTextChange = { vm.text = it },
        errorMessage = vm.errorMessage,
        isLoading = vm.isLoading,
        onSave = { coroutineScope.launch { if (vm.save()) onSaved() } },
        onBack = onBack
    )
}

@Composable
internal fun AddNoteContent(
    text: String,
    onTextChange: (String) -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
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
                value = text,
                onValueChange = onTextChange,
                label = { Text("Write your note...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                maxLines = 10
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            PrimaryButton(
                text = "Save Note",
                enabled = !isLoading,
                onClick = onSave
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddNoteSheetPreview() {
    TravelnoteTheme {
        AddNoteContent(
            text = "Had an amazing time at the Eiffel Tower today! The view from the top was breathtaking.",
            onTextChange = {},
            errorMessage = null,
            isLoading = false,
            onSave = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddNoteSheetEmptyPreview() {
    TravelnoteTheme {
        AddNoteContent(
            text = "",
            onTextChange = {},
            errorMessage = null,
            isLoading = false,
            onSave = {},
            onBack = {}
        )
    }
}
