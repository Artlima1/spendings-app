package com.arthur.spending.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arthur.spending.data.SpendingDatabase
import com.arthur.spending.data.TransactionRepository
import java.text.SimpleDateFormat
import java.util.*

data class TransactionItem(
    val id: Long = 0,
    val value: Double,
    val category: String,
    val location: String,
    val description: String = "",
    val date: Date
) {
    val formattedValue: String
        get() = String.format("%.2f", value)
    
    val formattedDate: String
        get() {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            return formatter.format(date)
        }
    
    val formattedTime: String
        get() {
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            return formatter.format(date)
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen() {
    val context = LocalContext.current
    val viewModel: TransactionsViewModel = viewModel {
        val database = SpendingDatabase.getDatabase(context)
        val repository = TransactionRepository(database.transactionDao())
        TransactionsViewModel(repository)
    }
    
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Transactions",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FilterHeader(
            categories = uiState.categories,
            selectedCategory = uiState.selectedCategory,
            startDate = uiState.startDate,
            endDate = uiState.endDate,
            onCategorySelected = { viewModel.setCategory(it) },
            onDateRangeSelected = { start, end -> viewModel.setDateRange(start, end) },
            onClearFilters = { viewModel.clearFilters() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading transactions",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = uiState.error ?: "Unknown error",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.refreshTransactions() }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            
            uiState.transactions.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions yet",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.transactions) { transaction ->
                        TransactionCard(transaction = transaction)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionCard(transaction: TransactionItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = transaction.location,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    
                    if (transaction.category.isNotBlank()) {
                        Text(
                            text = transaction.category,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (transaction.description.isNotBlank()) {
                        Text(
                            text = transaction.description,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Text(
                    text = "€${transaction.formattedValue}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = transaction.formattedDate,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = transaction.formattedTime,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterHeader(
    categories: List<String>,
    selectedCategory: String?,
    startDate: java.util.Date?,
    endDate: java.util.Date?,
    onCategorySelected: (String?) -> Unit,
    onDateRangeSelected: (java.util.Date?, java.util.Date?) -> Unit,
    onClearFilters: () -> Unit
) {
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var datePickerType by remember { mutableStateOf("start") }
    
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Filters",
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category Filter
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedCategory ?: "All Categories",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Categories") },
                            onClick = { 
                                onCategorySelected(null)
                                categoryDropdownExpanded = false
                            }
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = { 
                                    onCategorySelected(category)
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Clear filters button
                if (selectedCategory != null || startDate != null || endDate != null) {
                    FilledTonalButton(
                        onClick = onClearFilters,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear filters")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
            }
            
            // Date Range Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { 
                        datePickerType = "start"
                        showDatePicker = true 
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Start date")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(startDate?.let { dateFormatter.format(it) } ?: "Start Date")
                }
                
                OutlinedButton(
                    onClick = { 
                        datePickerType = "end"
                        showDatePicker = true 
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "End date")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(endDate?.let { dateFormatter.format(it) } ?: "End Date")
                }
            }
            
            // Active filters summary
            if (selectedCategory != null || startDate != null || endDate != null) {
                val filters = mutableListOf<String>()
                selectedCategory?.let { filters.add("Category: $it") }
                if (startDate != null && endDate != null) {
                    filters.add("Date: ${dateFormatter.format(startDate)} - ${dateFormatter.format(endDate)}")
                } else {
                    startDate?.let { filters.add("From: ${dateFormatter.format(it)}") }
                    endDate?.let { filters.add("Until: ${dateFormatter.format(it)}") }
                }
                
                Text(
                    text = "Active filters: ${filters.joinToString(", ")}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
    
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        
        DatePickerDialog(
            onDateSelected = { selectedDateMillis ->
                selectedDateMillis?.let { 
                    val selectedDate = java.util.Date(it)
                    when (datePickerType) {
                        "start" -> onDateRangeSelected(selectedDate, endDate)
                        "end" -> onDateRangeSelected(startDate, selectedDate)
                    }
                }
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}