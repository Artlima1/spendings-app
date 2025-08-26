package com.arthur.spending.ui.newtransaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arthur.spending.data.SpendingDatabase
import com.arthur.spending.data.TransactionRepository
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionScreen() {
    val context = LocalContext.current
    val viewModel: NewTransactionViewModel = viewModel {
        val database = SpendingDatabase.getDatabase(context)
        val repository = TransactionRepository(database.transactionDao())
        NewTransactionViewModel(repository)
    }
    10
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "New Transaction",
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Value Input in Euros
        OutlinedTextField(
            value = uiState.formattedValue,
            onValueChange = { input -> viewModel.updateValue(input) },
            label = { Text("Value *") },
            prefix = { Text("€") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = uiState.hasValueError,
            supportingText = if (uiState.hasValueError) {
                { Text("Value is required", color = MaterialTheme.colorScheme.error) }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(
            expanded = uiState.isDropdownExpanded,
            onExpandedChange = { viewModel.setDropdownExpanded(!uiState.isDropdownExpanded) }
        ) {
            OutlinedTextField(
                value = uiState.categoryQuery,
                onValueChange = { viewModel.updateCategoryQuery(it) },
                label = { Text("Category *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isDropdownExpanded) },
                isError = uiState.hasCategoryError,
                supportingText = if (uiState.hasCategoryError) {
                    { Text("Category is required", color = MaterialTheme.colorScheme.error) }
                } else null,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            
            ExposedDropdownMenu(
                expanded = uiState.isDropdownExpanded,
                onDismissRequest = { viewModel.setDropdownExpanded(false) }
            ) {
                uiState.filteredCategories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category) },
                        onClick = { viewModel.selectCategory(category) }
                    )
                }
                
                if (uiState.showCreateNewOption) {
                    DropdownMenuItem(
                        text = { Text("Create \"${uiState.categoryQuery}\"") },
                        onClick = { viewModel.selectCategory(uiState.categoryQuery) }
                    )
                }
            }
        }

        OutlinedTextField(
            value = uiState.location,
            onValueChange = { viewModel.updateLocation(it) },
            label = { Text("Location *") },
            isError = uiState.hasLocationError,
            supportingText = if (uiState.hasLocationError) {
                { Text("Location is required", color = MaterialTheme.colorScheme.error) }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.description,
            onValueChange = { viewModel.updateDescription(it) },
            label = { Text("Description (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )

        Text(
            text = "Date & Time *",
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp),
            color = if (uiState.hasDateTimeError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.date,
                onValueChange = { },
                label = { Text("Date") },
                readOnly = true,
                isError = uiState.hasDateTimeError,
                interactionSource = remember { MutableInteractionSource() }
                    .also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect {
                                if (it is PressInteraction.Release) {
                                    viewModel.showDatePicker()
                                }
                            }
                        }
                    },
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = uiState.time,
                onValueChange = { },
                label = { Text("Time") },
                readOnly = true,
                isError = uiState.hasDateTimeError,
                interactionSource = remember { MutableInteractionSource() }
                    .also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect {
                                if (it is PressInteraction.Release) {
                                    viewModel.showTimePicker()
                                }
                            }
                        }
                    },
                modifier = Modifier.weight(1f)
            )
        }

        if (uiState.hasDateTimeError) {
            Text(
                text = "Date and time are required",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        OutlinedButton(
            onClick = { viewModel.setCurrentDateTime() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Use Now")
        }

        Button(
            onClick = { viewModel.showConfirmationDialog() },
            enabled = uiState.isFormValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Transaction")
        }
    }
    
    if (uiState.showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideConfirmationDialog() },
            title = { Text("Confirm Transaction") },
            text = {
                Column {
                    Text("Please confirm the transaction details:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Value: €${uiState.formattedValue}", style = MaterialTheme.typography.bodyMedium)
                    Text("Category: ${uiState.categoryQuery}", style = MaterialTheme.typography.bodyMedium)
                    Text("Location: ${uiState.location}", style = MaterialTheme.typography.bodyMedium)
                    if (uiState.description.isNotEmpty()) {
                        Text("Description: ${uiState.description}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (uiState.date.isNotEmpty() && uiState.time.isNotEmpty()) {
                        Text("Date & Time: ${uiState.date} at ${uiState.time}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveTransaction() }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.hideConfirmationDialog() }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Date Picker Dialog
    if (uiState.showDatePicker) {
        val datePickerState = rememberDatePickerState()
        AlertDialog(
            onDismissRequest = { viewModel.hideDatePicker() },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val dateString = formatter.format(Date(millis))
                        viewModel.updateDate(dateString)
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDatePicker() }) {
                    Text("Cancel")
                }
            },
            text = {
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.padding(16.dp)
                )
            }
        )
    }
    
    // Time Picker Dialog
    if (uiState.showTimePicker) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { viewModel.hideTimePicker() },
            confirmButton = {
                TextButton(onClick = {
                    val timeString = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                    viewModel.updateTime(timeString)
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideTimePicker() }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(
                    state = timePickerState,
                    modifier = Modifier.padding(16.dp)
                )
            }
        )
    }
}