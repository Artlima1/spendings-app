package com.arthur.spending.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByCategory(category)

    fun getAllCategories(): Flow<List<String>> = transactionDao.getAllCategories()

    fun getTotalSpending(): Flow<Double?> = transactionDao.getTotalSpending()

    fun getTotalSpendingByCategory(category: String): Flow<Double?> = 
        transactionDao.getTotalSpendingByCategory(category)

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }
}