package com.arthur.spending.ui.newtransaction

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthur.spending.data.Transaction
import com.arthur.spending.data.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class NewTransactionUiState(
    val valueInCents: Int = 0,
    val categoryQuery: String = "",
    val isDropdownExpanded: Boolean = false,
    val location: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val showConfirmationDialog: Boolean = false,
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false,
    val allCategories: List<String> = listOf("Food", "Transportation", "Entertainment", "Shopping", "Bills", "Healthcare"),
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
    
    val isFormValid: Boolean
        get() = valueInCents > 0 && 
                categoryQuery.isNotBlank() && 
                location.isNotBlank() && 
                date.isNotBlank() && 
                time.isNotBlank()
}

class NewTransactionViewModel(
    private val repository: TransactionRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "NewTransactionViewModel"
    }
    
    private val _uiState = MutableStateFlow(NewTransactionUiState())
    val uiState = _uiState.asStateFlow()
    
    // Load categories from database
    private val _allCategories = repository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    init {
        Log.d(TAG, "ViewModel initialized")
        
        // Update UI state with categories from database
        viewModelScope.launch {
            _allCategories.collect { categories ->
                Log.d(TAG, "Categories loaded from database: ${categories.size} categories")
                Log.d(TAG, "Categories: $categories")
                
                val finalCategories = if (categories.isEmpty()) {
                    Log.d(TAG, "Database empty, using hardcoded categories")
                    listOf("Food", "Transportation", "Entertainment", "Shopping", "Bills", "Healthcare")
                } else {
                    Log.d(TAG, "Using database categories")
                    categories
                }
                
                _uiState.value = _uiState.value.copy(allCategories = finalCategories)
                Log.d(TAG, "UI state updated with categories: ${finalCategories.size}")
            }
        }
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
            hasValueError = false // Clear error when user types
        )
    }
    
    fun updateCategoryQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            categoryQuery = query,
            hasCategoryError = false // Clear error when user types
        )
    }
    
    fun setDropdownExpanded(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(isDropdownExpanded = expanded)
    }
    
    fun selectCategory(category: String) {
        Log.d(TAG, "selectCategory: '$category'")
        _uiState.value = _uiState.value.copy(
            categoryQuery = category,
            isDropdownExpanded = false
        )
    }
    
    fun updateLocation(location: String) {
        _uiState.value = _uiState.value.copy(
            location = location,
            hasLocationError = false // Clear error when user types
        )
    }
    
    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }
    
    fun setCurrentDateTime() {
        val now = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        _uiState.value = _uiState.value.copy(
            date = dateFormat.format(now.time),
            time = timeFormat.format(now.time),
            hasDateTimeError = false // Clear error when date/time is set
        )
    }
    
    fun showConfirmationDialog() {
        Log.d(TAG, "showConfirmationDialog called")
        validateForm()
        if (_uiState.value.isFormValid) {
            _uiState.value = _uiState.value.copy(showConfirmationDialog = true)
        }
    }
    
    private fun validateForm() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            hasValueError = currentState.valueInCents <= 0,
            hasCategoryError = currentState.categoryQuery.isBlank(),
            hasLocationError = currentState.location.isBlank(),
            hasDateTimeError = currentState.date.isBlank() || currentState.time.isBlank()
        )
        Log.d(TAG, "Form validation - Valid: ${_uiState.value.isFormValid}")
        Log.d(TAG, "Validation errors - Value: ${_uiState.value.hasValueError}, Category: ${_uiState.value.hasCategoryError}, Location: ${_uiState.value.hasLocationError}, DateTime: ${_uiState.value.hasDateTimeError}")
    }
    
    fun hideConfirmationDialog() {
        _uiState.value = _uiState.value.copy(showConfirmationDialog = false)
    }
    
    fun showDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = true)
    }
    
    fun hideDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = false)
    }
    
    fun updateDate(date: String) {
        _uiState.value = _uiState.value.copy(
            date = date, 
            showDatePicker = false,
            hasDateTimeError = false // Clear error when date is updated
        )
    }
    
    fun showTimePicker() {
        _uiState.value = _uiState.value.copy(showTimePicker = true)
    }
    
    fun hideTimePicker() {
        _uiState.value = _uiState.value.copy(showTimePicker = false)
    }
    
    fun updateTime(time: String) {
        _uiState.value = _uiState.value.copy(
            time = time, 
            showTimePicker = false,
            hasDateTimeError = false // Clear error when time is updated
        )
    }
    
    fun saveTransaction() {
        val currentState = _uiState.value
        Log.d(TAG, "saveTransaction called")
        Log.d(TAG, "Current state - Value: ${currentState.valueInCents}, Category: '${currentState.categoryQuery}', Location: '${currentState.location}'")
        Log.d(TAG, "Current state - Description: '${currentState.description}', Date: '${currentState.date}', Time: '${currentState.time}'")
        
        // Validate that we have minimum required data
        if (currentState.valueInCents <= 0 || currentState.categoryQuery.isBlank()) {
            Log.w(TAG, "Validation failed - Value: ${currentState.valueInCents}, Category blank: ${currentState.categoryQuery.isBlank()}")
            return // Don't save invalid transactions
        }
        
        // Parse date and time, use current if not set
        val dateTime = if (currentState.date.isNotEmpty() && currentState.time.isNotEmpty()) {
            try {
                val dateTimeString = "${currentState.date} ${currentState.time}"
                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val parsedDate = formatter.parse(dateTimeString) ?: Date()
                Log.d(TAG, "Parsed date/time: $dateTimeString -> $parsedDate")
                parsedDate
            } catch (e: Exception) {
                Log.w(TAG, "Date parsing failed: ${e.message}, using current date")
                Date() // Fallback to current date if parsing fails
            }
        } else {
            Log.d(TAG, "No date/time specified, using current date")
            Date() // Use current date/time if not specified
        }
        
        // Create transaction object
        val valueInEuros = currentState.valueInCents / 100.0
        val transaction = Transaction(
            value = valueInEuros, // Convert cents to euros
            date = dateTime,
            category = currentState.categoryQuery.trim(),
            location = currentState.location.trim(),
            description = currentState.description.trim()
        )
        
        Log.d(TAG, "Created transaction object:")
        Log.d(TAG, "  Value: €${valueInEuros} (${currentState.valueInCents} cents)")
        Log.d(TAG, "  Date: $dateTime")
        Log.d(TAG, "  Category: '${transaction.category}'")
        Log.d(TAG, "  Location: '${transaction.location}'")
        Log.d(TAG, "  Description: '${transaction.description}'")
        
        // Save to database
        viewModelScope.launch {
            try {
                Log.d(TAG, "Attempting to save transaction to database...")
                repository.insertTransaction(transaction)
                Log.i(TAG, "Transaction saved successfully!")
                
                // Reset form after successful save
                _uiState.value = NewTransactionUiState()
                Log.d(TAG, "Form reset to initial state")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save transaction: ${e.message}", e)
                // Handle error - in a real app you might want to show an error message
                // For now, we'll just not reset the form so user can try again
            }
        }
    }
}