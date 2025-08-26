package com.arthur.spending.ui.newtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

data class NewTransactionUiState(
    val valueInCents: Int = 0,
    val categoryQuery: String = "",
    val isDropdownExpanded: Boolean = false,
    val location: String = "",
    val date: String = "",
    val time: String = "",
    val showConfirmationDialog: Boolean = false,
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false,
    val allCategories: List<String> = listOf("Food", "Transportation", "Entertainment", "Shopping", "Bills", "Healthcare")
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

class NewTransactionViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(NewTransactionUiState())
    val uiState = _uiState.asStateFlow()
    
    fun updateValue(input: String) {
        val cleanInput = input.replace(".", "").replace(",", "")
        val digitsOnly = cleanInput.filter { it.isDigit() }
        val newValue = if (digitsOnly.isNotEmpty() && digitsOnly.length <= 8) {
            digitsOnly.toIntOrNull() ?: 0
        } else if (digitsOnly.isEmpty()) {
            0
        } else {
            _uiState.value.valueInCents
        }
        _uiState.value = _uiState.value.copy(valueInCents = newValue)
    }
    
    fun updateCategoryQuery(query: String) {
        _uiState.value = _uiState.value.copy(categoryQuery = query)
    }
    
    fun setDropdownExpanded(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(isDropdownExpanded = expanded)
    }
    
    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(
            categoryQuery = category,
            isDropdownExpanded = false
        )
    }
    
    fun updateLocation(location: String) {
        _uiState.value = _uiState.value.copy(location = location)
    }
    
    fun setCurrentDateTime() {
        val now = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        _uiState.value = _uiState.value.copy(
            date = dateFormat.format(now.time),
            time = timeFormat.format(now.time)
        )
    }
    
    fun showConfirmationDialog() {
        _uiState.value = _uiState.value.copy(showConfirmationDialog = true)
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
        _uiState.value = _uiState.value.copy(date = date, showDatePicker = false)
    }
    
    fun showTimePicker() {
        _uiState.value = _uiState.value.copy(showTimePicker = true)
    }
    
    fun hideTimePicker() {
        _uiState.value = _uiState.value.copy(showTimePicker = false)
    }
    
    fun updateTime(time: String) {
        _uiState.value = _uiState.value.copy(time = time, showTimePicker = false)
    }
    
    fun saveTransaction() {
        // TODO: Save to database
        // Reset form after saving
        _uiState.value = NewTransactionUiState()
    }
}