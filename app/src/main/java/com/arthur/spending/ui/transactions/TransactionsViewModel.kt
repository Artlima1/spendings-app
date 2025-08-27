package com.arthur.spending.ui.transactions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthur.spending.data.Transaction
import com.arthur.spending.data.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class TransactionsUiState(
    val transactions: List<TransactionItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val startDate: Date? = null,
    val endDate: Date? = null
)

class TransactionsViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    companion object {
        private const val TAG = "TransactionsViewModel"
    }

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        Log.d(TAG, "TransactionsViewModel initialized")
        // Set initial date range to current month
        val currentMonth = getCurrentMonthDateRange()
        _uiState.value = _uiState.value.copy(
            startDate = currentMonth.first,
            endDate = currentMonth.second
        )
        loadCategories()
        loadTransactions()
    }

    private fun loadCategories() {
        Log.d(TAG, "Loading categories from database...")
        viewModelScope.launch {
            try {
                repository.getAllCategories().collect { categories ->
                    Log.d(TAG, "Loaded ${categories.size} categories from database")
                    _uiState.value = _uiState.value.copy(categories = categories)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error loading categories: ${e.message}", e)
            }
        }
    }

    private fun loadTransactions() {
        Log.d(TAG, "Loading transactions from database...")
        
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val flow = when {
                    currentState.selectedCategory != null && 
                    currentState.startDate != null && 
                    currentState.endDate != null -> {
                        Log.d(TAG, "Filtering by category '${currentState.selectedCategory}' and date range")
                        repository.getTransactionsByCategoryAndDateRange(
                            currentState.selectedCategory!!,
                            currentState.startDate!!.time,
                            currentState.endDate!!.time
                        )
                    }
                    currentState.selectedCategory != null -> {
                        Log.d(TAG, "Filtering by category '${currentState.selectedCategory}'")
                        repository.getTransactionsByCategory(currentState.selectedCategory!!)
                    }
                    currentState.startDate != null && currentState.endDate != null -> {
                        Log.d(TAG, "Filtering by date range")
                        repository.getTransactionsByDateRange(
                            currentState.startDate!!.time,
                            currentState.endDate!!.time
                        )
                    }
                    else -> {
                        Log.d(TAG, "Loading all transactions")
                        repository.getAllTransactions()
                    }
                }
                
                flow.collect { transactions ->
                    Log.d(TAG, "Loaded ${transactions.size} transactions from database")
                    
                    val transactionItems = transactions.map { transaction ->
                        TransactionItem(
                            id = transaction.id,
                            value = transaction.value,
                            category = transaction.category,
                            location = transaction.location,
                            description = transaction.description,
                            date = transaction.date
                        )
                    }
                    
                    Log.d(TAG, "Converted to ${transactionItems.size} TransactionItems")
                    
                    _uiState.value = _uiState.value.copy(
                        transactions = transactionItems,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading transactions: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load transactions: ${e.message}"
                )
            }
        }
    }

    fun refreshTransactions() {
        Log.d(TAG, "Refreshing transactions...")
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        loadTransactions()
    }
    
    fun setCategory(category: String?) {
        Log.d(TAG, "Setting category filter: '$category'")
        _uiState.value = _uiState.value.copy(
            selectedCategory = category,
            isLoading = true
        )
        loadTransactions()
    }
    
    fun setDateRange(startDate: Date?, endDate: Date?) {
        Log.d(TAG, "Setting date range filter: $startDate to $endDate")
        _uiState.value = _uiState.value.copy(
            startDate = startDate,
            endDate = endDate,
            isLoading = true
        )
        loadTransactions()
    }
    
    fun clearFilters() {
        Log.d(TAG, "Clearing all filters")
        _uiState.value = _uiState.value.copy(
            selectedCategory = null,
            startDate = null,
            endDate = null,
            isLoading = true
        )
        loadTransactions()
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