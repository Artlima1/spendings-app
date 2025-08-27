package com.arthur.spending.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthur.spending.data.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

data class CategorySpending(
    val category: String,
    val amount: Double,
    val percentage: Float
)

data class DashboardUiState(
    val categorySpending: List<CategorySpending> = emptyList(),
    val totalSpending: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val startDate: java.util.Date? = null,
    val endDate: java.util.Date? = null
)

class DashboardViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    companion object {
        private const val TAG = "DashboardViewModel"
    }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        Log.d(TAG, "DashboardViewModel initialized")
        // Set initial date range to current month
        val currentMonth = getCurrentMonthDateRange()
        _uiState.value = _uiState.value.copy(
            startDate = currentMonth.first,
            endDate = currentMonth.second
        )
        loadSpendingData()
    }

    private fun loadSpendingData() {
        Log.d(TAG, "Loading spending data...")
        
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val hasDateFilter = currentState.startDate != null && currentState.endDate != null
                
                // Use combine to get all data in one go
                combine(
                    repository.getAllCategories(),
                    if (hasDateFilter) {
                        repository.getTotalSpendingByDateRange(
                            currentState.startDate!!.time,
                            currentState.endDate!!.time
                        )
                    } else {
                        repository.getTotalSpending()
                    }
                ) { categories, total ->
                    val totalAmount = total ?: 0.0
                    Log.d(TAG, "Loaded ${categories.size} categories: $categories")
                    Log.d(TAG, "Total spending: €$totalAmount (date filtered: $hasDateFilter)")
                    
                    if (categories.isEmpty() || totalAmount <= 0.0) {
                        Log.d(TAG, "No data found, setting empty state")
                        return@combine Pair(emptyList<CategorySpending>(), totalAmount)
                    }
                    
                    // Get spending for each category using first() to avoid nested flows
                    val categorySpendingList = mutableListOf<CategorySpending>()
                    
                    categories.forEach { category ->
                        try {
                            val categoryAmount = if (hasDateFilter) {
                                repository.getTotalSpendingByCategoryAndDateRange(
                                    category,
                                    currentState.startDate!!.time,
                                    currentState.endDate!!.time
                                ).first() ?: 0.0
                            } else {
                                repository.getTotalSpendingByCategory(category).first() ?: 0.0
                            }
                            Log.d(TAG, "Category '$category': €$categoryAmount")
                            
                            if (categoryAmount > 0.0) {
                                val percentage = (categoryAmount / totalAmount * 100).toFloat()
                                categorySpendingList.add(
                                    CategorySpending(
                                        category = category,
                                        amount = categoryAmount,
                                        percentage = percentage
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error getting spending for category '$category': ${e.message}")
                        }
                    }
                    
                    Log.d(TAG, "Processed ${categorySpendingList.size} categories with spending")
                    return@combine Pair(categorySpendingList.sortedByDescending { it.amount }, totalAmount)
                }.collect { (categorySpending, totalAmount) ->
                    Log.d(TAG, "Updating UI state with ${categorySpending.size} categories")
                    _uiState.value = _uiState.value.copy(
                        categorySpending = categorySpending,
                        totalSpending = totalAmount,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading spending data: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load spending data: ${e.message}"
                )
            }
        }
    }

    fun refreshSpendingData() {
        Log.d(TAG, "Refreshing spending data...")
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        loadSpendingData()
    }
    
    fun setDateRange(startDate: java.util.Date?, endDate: java.util.Date?) {
        Log.d(TAG, "Setting date range filter: $startDate to $endDate")
        _uiState.value = _uiState.value.copy(
            startDate = startDate,
            endDate = endDate,
            isLoading = true
        )
        loadSpendingData()
    }
    
    fun clearDateFilter() {
        Log.d(TAG, "Clearing date filter")
        _uiState.value = _uiState.value.copy(
            startDate = null,
            endDate = null,
            isLoading = true
        )
        loadSpendingData()
    }
    
    private fun getCurrentMonthDateRange(): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        val today = Date()
        
        // Set to start of current month
        calendar.time = today
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.time
        
        // End date is today
        calendar.time = today
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.time
        
        Log.d(TAG, "Default date range: ${startOfMonth} to ${endOfDay}")
        return Pair(startOfMonth, endOfDay)
    }
}