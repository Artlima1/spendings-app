package com.arthur.spending.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
fun TransactionDetailScreen(
    transactionId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: TransactionDetailViewModel = viewModel {
        val database = SpendingDatabase.getDatabase(context)
        val repository = TransactionRepository(database.transactionDao())
        TransactionDetailViewModel(repository, transactionId)
    }
    
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "Edit Transaction" else "Transaction Details",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isEditMode) {
                            viewModel.cancelEdit()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { 
                            viewModel.setEditMode(!uiState.isEditMode)
                        }
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = if (uiState.isEditMode) "Cancel Edit" else "Edit Transaction"
                        )
                    }
                    if (!uiState.isEditMode) {
                        IconButton(
                            onClick = { 
                                viewModel.setShowDeleteConfirmation(true)
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Transaction",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading transaction",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = uiState.error ?: "Unknown error",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadTransaction() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            
            uiState.transaction != null -> {
                TransactionDetailContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    onNavigateBack = onNavigateBack,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailContent(
    uiState: TransactionDetailUiState,
    viewModel: TransactionDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!uiState.isEditMode) {
            // View Mode - Amount Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Amount",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "€${uiState.transaction?.formattedValue}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // View Mode - Transaction Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Transaction Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    TransactionViewRow("Location", uiState.transaction?.location ?: "")
                    TransactionViewRow("Category", uiState.transaction?.category ?: "")
                    if (uiState.transaction?.description?.isNotBlank() == true) {
                        TransactionViewRow("Description", uiState.transaction.description)
                    }
                    TransactionViewRow("Date", uiState.transaction?.formattedDate ?: "")
                    TransactionViewRow("Time", uiState.transaction?.formattedTime ?: "")
                }
            }
        } else {
            // Edit Mode - Same patterns as NewTransactionScreen
            Text(
                text = "Edit Transaction",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
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

            // Category Dropdown
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

            // Location
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

            // Description
            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // Date & Time
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
                                        viewModel.setShowDatePicker(true)
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
                                        viewModel.setShowTimePicker(true)
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
                    fontSize = 12.sp
                )
            }

            // Save and Cancel buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.cancelEdit() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = { viewModel.saveTransaction() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Changes")
                }
            }
        }
    }

    // Date Picker Dialog
    if (uiState.showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { viewModel.setShowDatePicker(false) },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Date(millis)
                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            viewModel.updateDate(formatter.format(date))
                        }
                        viewModel.setShowDatePicker(false)
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowDatePicker(false) }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (uiState.showTimePicker) {
        val timePickerState = rememberTimePickerState()
        TimePickerDialog(
            onDismissRequest = { viewModel.setShowTimePicker(false) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val hour = timePickerState.hour
                        val minute = timePickerState.minute
                        val time = String.format("%02d:%02d", hour, minute)
                        viewModel.updateTime(time)
                        viewModel.setShowTimePicker(false)
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowTimePicker(false) }) {
                    Text("Cancel")
                }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    // Delete Confirmation Dialog
    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowDeleteConfirmation(false) },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Delete Transaction",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Are you sure you want to delete this transaction?",
                        fontSize = 16.sp
                    )
                    Text(
                        text = "This action cannot be undone.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    uiState.transaction?.let { transaction ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "€${transaction.formattedValue}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = transaction.location,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "${transaction.category} • ${transaction.formattedDate}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(onNavigateBack)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.setShowDeleteConfirmation(false) }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TransactionViewRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        text = {
            content()
        }
    )
}