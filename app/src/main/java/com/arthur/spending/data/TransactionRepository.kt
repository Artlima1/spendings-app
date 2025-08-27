package com.arthur.spending.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByCategory(category)

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByDateRange(startDate, endDate)

    fun getTransactionsByCategoryAndDateRange(
        category: String, 
        startDate: Long, 
        endDate: Long
    ): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByCategoryAndDateRange(category, startDate, endDate)

    fun getAllCategories(): Flow<List<String>> = transactionDao.getAllCategories()

    fun getTotalSpending(): Flow<Double?> = transactionDao.getTotalSpending()

    fun getTotalSpendingByDateRange(startDate: Long, endDate: Long): Flow<Double?> = 
        transactionDao.getTotalSpendingByDateRange(startDate, endDate)

    fun getTotalSpendingByCategory(category: String): Flow<Double?> = 
        transactionDao.getTotalSpendingByCategory(category)

    fun getTotalSpendingByCategoryAndDateRange(
        category: String, 
        startDate: Long, 
        endDate: Long
    ): Flow<Double?> = 
        transactionDao.getTotalSpendingByCategoryAndDateRange(category, startDate, endDate)

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