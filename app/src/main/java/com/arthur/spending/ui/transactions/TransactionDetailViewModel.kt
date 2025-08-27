package com.arthur.spending.ui.transactions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthur.spending.data.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class TransactionDetailUiState(
    val transaction: TransactionItem? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    // Edit mode fields
    val isEditMode: Boolean = false,
    val valueInCents: Int = 0,
    val categoryQuery: String = "",
    val isDropdownExpanded: Boolean = false,
    val location: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val allCategories: List<String> = emptyList(),
    val hasValueError: Boolean = false,
    val hasCategoryError: Boolean = false,
    val hasLocationError: Boolean = false,
    val hasDateTimeError: Boolean = false
) {
    val formattedValue: String
        get() {
            val euros = valueInCents / 100
            val cents = valueInCents % 100
            return String.format("%.2f", euros + cents / 100.0)
        }
    
    val filteredCategories: List<String>
        get() = if (categoryQuery.isBlank()) {
            allCategories
        } else {
            allCategories.filter { it.contains(categoryQuery, ignoreCase = true) }
        }
    
    val showCreateNewOption: Boolean
        get() = categoryQuery.isNotBlank() && !filteredCategories.any { it.equals(categoryQuery, ignoreCase = true) }
}

class TransactionDetailViewModel(
    private val repository: TransactionRepository,
    private val transactionId: Long
) : ViewModel() {

    companion object {
        private const val TAG = "TransactionDetailVM"
    }

    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        Log.d(TAG, "TransactionDetailViewModel initialized for transaction ID: $transactionId")
        loadCategories()
        loadTransaction()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                repository.getAllCategories().collect { categories ->
                    _uiState.value = _uiState.value.copy(
                        allCategories = categories.ifEmpty { 
                            listOf("Food", "Transportation", "Entertainment", "Shopping", "Bills", "Healthcare")
                        }
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error loading categories: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    allCategories = listOf("Food", "Transportation", "Entertainment", "Shopping", "Bills", "Healthcare")
                )
            }
        }
    }

    fun loadTransaction() {
        Log.d(TAG, "Loading transaction with ID: $transactionId")
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                repository.getAllTransactions().collect { transactions ->
                    val transaction = transactions.find { it.id == transactionId }
                    
                    if (transaction != null) {
                        val transactionItem = TransactionItem(
                            id = transaction.id,
                            value = transaction.value,
                            category = transaction.category,
                            location = transaction.location,
                            description = transaction.description,
                            date = transaction.date
                        )
                        
                        Log.d(TAG, "Loaded transaction: ${transactionItem.location}")
                        
                        // Populate edit fields
                        val valueInCents = (transactionItem.value * 100).toInt()
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        
                        _uiState.value = _uiState.value.copy(
                            transaction = transactionItem,
                            isLoading = false,
                            error = null,
                            // Populate edit fields
                            valueInCents = valueInCents,
                            categoryQuery = transactionItem.category,
                            location = transactionItem.location,
                            description = transactionItem.description,
                            date = dateFormat.format(transactionItem.date),
                            time = timeFormat.format(transactionItem.date)
                        )
                    } else {
                        Log.w(TAG, "Transaction with ID $transactionId not found")
                        _uiState.value = _uiState.value.copy(
                            transaction = null,
                            isLoading = false,
                            error = "Transaction not found"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading transaction: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load transaction: ${e.message}"
                )
            }
        }
    }

    // Edit mode methods
    fun setEditMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isEditMode = enabled)
    }
    
    fun updateValue(input: String) {
        val cleanInput = input.replace(".", "").replace(",", "").replace("€", "").trim()
        val digitsOnly = cleanInput.filter { it.isDigit() }
        val newValue = if (digitsOnly.isNotEmpty() && digitsOnly.length <= 8) {
            digitsOnly.toIntOrNull() ?: 0
        } else if (digitsOnly.isEmpty()) {
            0
        } else {
            _uiState.value.valueInCents
        }
        Log.d(TAG, "updateValue: '$input' -> '$digitsOnly' -> $newValue cents")
        _uiState.value = _uiState.value.copy(
            valueInCents = newValue,
            hasValueError = false
        )
    }
    
    fun updateCategoryQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            categoryQuery = query,
            hasCategoryError = false
        )
    }
    
    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(
            categoryQuery = category,
            isDropdownExpanded = false,
            hasCategoryError = false
        )
    }
    
    fun setDropdownExpanded(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(isDropdownExpanded = expanded)
    }
    
    fun updateLocation(location: String) {
        _uiState.value = _uiState.value.copy(
            location = location,
            hasLocationError = false
        )
    }
    
    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }
    
    fun updateDate(date: String) {
        _uiState.value = _uiState.value.copy(
            date = date,
            hasDateTimeError = false
        )
    }
    
    fun updateTime(time: String) {
        _uiState.value = _uiState.value.copy(
            time = time,
            hasDateTimeError = false
        )
    }
    
    fun setShowDatePicker(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDatePicker = show)
    }
    
    fun setShowTimePicker(show: Boolean) {
        _uiState.value = _uiState.value.copy(showTimePicker = show)
    }
    
    fun setShowDeleteConfirmation(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDeleteConfirmation = show)
    }
    
    fun cancelEdit() {
        // Reset edit fields to original transaction values
        val transaction = _uiState.value.transaction
        if (transaction != null) {
            val valueInCents = (transaction.value * 100).toInt()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            
            _uiState.value = _uiState.value.copy(
                isEditMode = false,
                valueInCents = valueInCents,
                categoryQuery = transaction.category,
                location = transaction.location,
                description = transaction.description,
                date = dateFormat.format(transaction.date),
                time = timeFormat.format(transaction.date),
                hasValueError = false,
                hasCategoryError = false,
                hasLocationError = false,
                hasDateTimeError = false,
                isDropdownExpanded = false,
                showDatePicker = false,
                showTimePicker = false
            )
        }
    }

    fun saveTransaction() {
        Log.d(TAG, "saveTransaction called")
        val currentState = _uiState.value
        
        // Validation
        val hasValueError = currentState.valueInCents <= 0
        val hasCategoryError = currentState.categoryQuery.isBlank()
        val hasLocationError = currentState.location.isBlank()
        val hasDateTimeError = currentState.date.isBlank() || currentState.time.isBlank()
        
        _uiState.value = _uiState.value.copy(
            hasValueError = hasValueError,
            hasCategoryError = hasCategoryError,
            hasLocationError = hasLocationError,
            hasDateTimeError = hasDateTimeError
        )
        
        if (hasValueError || hasCategoryError || hasLocationError || hasDateTimeError) {
            Log.d(TAG, "Validation failed")
            return
        }
        
        viewModelScope.launch {
            try {
                // Parse date and time
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val dateTime = dateFormat.parse("${currentState.date} ${currentState.time}") ?: Date()
                val valueInEuros = currentState.valueInCents / 100.0
                
                val updatedTransaction = com.arthur.spending.data.Transaction(
                    id = transactionId,
                    value = valueInEuros,
                    date = dateTime,
                    category = currentState.categoryQuery.trim(),
                    location = currentState.location.trim(),
                    description = currentState.description.trim()
                )
                
                repository.updateTransaction(updatedTransaction)
                
                // Reload transaction to update UI
                loadTransaction()
                
                _uiState.value = _uiState.value.copy(isEditMode = false)
                Log.d(TAG, "Transaction saved successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving transaction: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to save transaction: ${e.message}"
                )
            }
        }
    }

    fun deleteTransaction(onDeleteComplete: () -> Unit) {
        Log.d(TAG, "deleteTransaction called")
        val transaction = _uiState.value.transaction
        
        if (transaction == null) {
            Log.w(TAG, "Cannot delete - transaction is null")
            return
        }
        
        viewModelScope.launch {
            try {
                val dataTransaction = com.arthur.spending.data.Transaction(
                    id = transaction.id,
                    value = transaction.value,
                    date = transaction.date,
                    category = transaction.category,
                    location = transaction.location,
                    description = transaction.description
                )
                
                repository.deleteTransaction(dataTransaction)
                
                Log.d(TAG, "Transaction deleted successfully")
                _uiState.value = _uiState.value.copy(showDeleteConfirmation = false)
                
                // Navigate back after successful deletion
                onDeleteComplete()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting transaction: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    showDeleteConfirmation = false,
                    error = "Failed to delete transaction: ${e.message}"
                )
            }
        }
    }
}