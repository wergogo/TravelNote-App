package com.wergogo.travelnote.ui.addexpense

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.data.model.ExpenseCategory
import com.wergogo.travelnote.ui.components.AppTopBar
import com.wergogo.travelnote.ui.components.PrimaryButton
import kotlinx.coroutines.launch

@Composable
fun AddExpenseSheet(
    tripId: String,
    dateKey: String,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TravelNoteApplication
    val context = LocalContext.current
    val vm: AddExpenseViewModel = viewModel(key = "$tripId/$dateKey") {
        AddExpenseViewModel(app.container.dayRepository, tripId, dateKey)
    }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { AppTopBar("Add Expense", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category selector
            Text("Category")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExpenseCategory.values().forEach { cat ->
                    FilterChip(
                        selected = vm.category == cat,
                        onClick = { vm.category = cat },
                        label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Amount field
            OutlinedTextField(
                value = vm.amount,
                onValueChange = { vm.amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Description field
            OutlinedTextField(
                value = vm.description,
                onValueChange = { vm.description = it },
                label = { Text("Description (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // GPS location row
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (vm.placeLabel.isNotBlank()) {
                    Text(
                        vm.placeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        "No location",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
                IconButton(onClick = { coroutineScope.launch { vm.fetchLocation(context) } }) {
                    Icon(Icons.Default.LocationOn, "Get location")
                }
            }

            // Error message
            if (vm.errorMessage != null) {
                Text(
                    text = vm.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Save button
            PrimaryButton(
                text = "Add Expense",
                enabled = !vm.isLoading,
                onClick = { coroutineScope.launch { if (vm.save()) onSaved() } }
            )
        }
    }
}
