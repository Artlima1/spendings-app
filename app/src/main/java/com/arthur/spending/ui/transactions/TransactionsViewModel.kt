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
    val error: String? = null
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
        loadTransactions()
    }

    private fun loadTransactions() {
        Log.d(TAG, "Loading transactions from database...")
        
        viewModelScope.launch {
            try {
                repository.getAllTransactions().collect { transactions ->
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
}