package com.wergogo.travelnote.ui.addexpense

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wergogo.travelnote.TravelNoteApplication
import com.wergogo.travelnote.data.model.ExpenseCategory
import com.wergogo.travelnote.ui.components.AppTopBar
import com.wergogo.travelnote.ui.components.PrimaryButton
import com.wergogo.travelnote.ui.theme.TravelnoteTheme
import kotlinx.coroutines.launch

@Composable
fun AddExpenseSheet(
    tripId: String,
    dateKey: String,
    expenseId: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TravelNoteApplication
    val context = LocalContext.current
    val vm: AddExpenseViewModel = viewModel(key = "$tripId/$dateKey/${expenseId ?: "new"}") {
        AddExpenseViewModel(app.container.dayRepository, tripId, dateKey, expenseId)
    }
    val coroutineScope = rememberCoroutineScope()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            coroutineScope.launch { vm.fetchLocation(context) }
        } else {
            vm.errorMessage = "Location permission denied"
        }
    }

    AddExpenseContent(
        isEditMode = vm.isEditMode,
        category = vm.category,
        onCategoryChange = { vm.category = it },
        amount = vm.amount,
        onAmountChange = { vm.amount = it },
        description = vm.description,
        onDescriptionChange = { vm.description = it },
        placeLabel = vm.placeLabel,
        errorMessage = vm.errorMessage,
        isLoading = vm.isLoading,
        onLocationClick = {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                coroutineScope.launch { vm.fetchLocation(context) }
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        },
        onSave = { coroutineScope.launch { if (vm.save()) onSaved() } },
        onBack = onBack
    )
}

@Composable
internal fun AddExpenseContent(
    isEditMode: Boolean,
    category: ExpenseCategory,
    onCategoryChange: (ExpenseCategory) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    placeLabel: String,
    errorMessage: String?,
    isLoading: Boolean,
    onLocationClick: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                if (isEditMode) "Edit Expense" else "Add Expense",
                onBack = onBack
            )
        }
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
                ExpenseCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { onCategoryChange(cat) },
                        label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Amount field
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Description field
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Description (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // GPS location row
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (placeLabel.isNotBlank()) {
                    Text(
                        placeLabel,
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
                IconButton(onClick = onLocationClick) {
                    Icon(Icons.Default.LocationOn, "Get location")
                }
            }

            // Error message
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Save button
            PrimaryButton(
                text = if (isEditMode) "Save Expense" else "Add Expense",
                enabled = !isLoading,
                onClick = onSave
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddExpenseSheetPreview() {
    TravelnoteTheme {
        AddExpenseContent(
            isEditMode = false,
            category = ExpenseCategory.FOOD,
            onCategoryChange = {},
            amount = "42.50",
            onAmountChange = {},
            description = "Dinner at Le Comptoir",
            onDescriptionChange = {},
            placeLabel = "Le Comptoir du Pantheon, Paris",
            errorMessage = null,
            isLoading = false,
            onLocationClick = {},
            onSave = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddExpenseSheetEmptyPreview() {
    TravelnoteTheme {
        AddExpenseContent(
            isEditMode = false,
            category = ExpenseCategory.OTHER,
            onCategoryChange = {},
            amount = "",
            onAmountChange = {},
            description = "",
            onDescriptionChange = {},
            placeLabel = "",
            errorMessage = null,
            isLoading = false,
            onLocationClick = {},
            onSave = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditExpenseSheetPreview() {
    TravelnoteTheme {
        AddExpenseContent(
            isEditMode = true,
            category = ExpenseCategory.TRANSPORT,
            onCategoryChange = {},
            amount = "15.00",
            onAmountChange = {},
            description = "Metro pass",
            onDescriptionChange = {},
            placeLabel = "Gare du Nord, Paris",
            errorMessage = null,
            isLoading = false,
            onLocationClick = {},
            onSave = {},
            onBack = {}
        )
    }
}
