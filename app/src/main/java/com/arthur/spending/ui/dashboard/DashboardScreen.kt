package com.arthur.spending.ui.dashboard

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arthur.spending.data.SpendingDatabase
import com.arthur.spending.data.TransactionRepository
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val viewModel: DashboardViewModel = viewModel {
        val database = SpendingDatabase.getDatabase(context)
        val repository = TransactionRepository(database.transactionDao())
        DashboardViewModel(repository)
    }
    
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Dashboard",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            DateFilterHeader(
                startDate = uiState.startDate,
                endDate = uiState.endDate,
                onDateRangeSelected = { start, end -> viewModel.setDateRange(start, end) },
                onClearFilter = { viewModel.clearDateFilter() }
            )
        }

        when {
            uiState.isLoading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            
            uiState.error != null -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Error loading data",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = uiState.error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.refreshSpendingData() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
            
            uiState.categorySpending.isEmpty() -> {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No spending data yet",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Add some transactions to see your spending breakdown",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            
            else -> {
                item {
                    SpendingChart(
                        categorySpending = uiState.categorySpending,
                        totalSpending = uiState.totalSpending
                    )
                }
                
                item {
                    SpendingLegend(categorySpending = uiState.categorySpending)
                }
            }
        }
    }
}

@Composable
fun SpendingChart(
    categorySpending: List<CategorySpending>,
    totalSpending: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Spending by Category",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                DonutChart(
                    categorySpending = categorySpending,
                    modifier = Modifier.size(200.dp)
                )
                
                // Center text showing total spending
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "€${String.format("%.2f", totalSpending)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    categorySpending: List<CategorySpending>,
    modifier: Modifier = Modifier
) {
    val colors = getCategoryColors()
    
    Canvas(modifier = modifier) {
        val strokeWidth = 30.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)
        
        var startAngle = -90f // Start from top
        
        categorySpending.forEachIndexed { index, spending ->
            val sweepAngle = (spending.percentage / 100f) * 360f
            val color = colors[index % colors.size]
            
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius,
                    center.y - radius
                ),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )
            
            startAngle += sweepAngle
        }
        
        // Draw white circle in the center
        drawCircle(
            color = Color.White,
            radius = radius - strokeWidth / 2,
            center = center
        )
    }
}

@Composable
fun SpendingLegend(categorySpending: List<CategorySpending>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Categories",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            val colors = getCategoryColors()
            
            categorySpending.forEachIndexed { index, spending ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .drawBehind {
                                    drawCircle(colors[index % colors.size])
                                }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = spending.category,
                            fontSize = 14.sp
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "€${String.format("%.2f", spending.amount)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${String.format("%.1f", spending.percentage)}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (index < categorySpending.size - 1) {
                    Divider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
fun getCategoryColors(): List<Color> {
    return listOf(
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green  
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF607D8B), // Blue Grey
        Color(0xFFFFC107), // Amber
        Color(0xFF795548), // Brown
        Color(0xFF009688), // Teal
        Color(0xFFE91E63)  // Pink
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilterHeader(
    startDate: java.util.Date?,
    endDate: java.util.Date?,
    onDateRangeSelected: (java.util.Date?, java.util.Date?) -> Unit,
    onClearFilter: () -> Unit
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Date Filter",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                
                if (startDate != null || endDate != null) {
                    FilledTonalButton(
                        onClick = onClearFilter,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear, 
                            contentDescription = "Clear filter",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", fontSize = 12.sp)
                    }
                }
            }
            
            // Date Range Buttons
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
            
            // Active filter summary
            if (startDate != null || endDate != null) {
                val filterText = when {
                    startDate != null && endDate != null -> 
                        "Showing data from ${dateFormatter.format(startDate)} to ${dateFormatter.format(endDate)}"
                    startDate != null -> 
                        "Showing data from ${dateFormatter.format(startDate)}"
                    endDate != null -> 
                        "Showing data until ${dateFormatter.format(endDate)}"
                    else -> ""
                }
                
                Text(
                    text = filterText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
    
    if (showDatePicker) {
        DashboardDatePickerDialog(
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
fun DashboardDatePickerDialog(
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