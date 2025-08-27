package com.arthur.spending.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY date DESC")
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE category = :category AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getTransactionsByCategoryAndDateRange(category: String, startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT DISTINCT category FROM transactions ORDER BY category")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT SUM(value) FROM transactions")
    fun getTotalSpending(): Flow<Double?>

    @Query("SELECT SUM(value) FROM transactions WHERE category = :category")
    fun getTotalSpendingByCategory(category: String): Flow<Double?>

    @Insert
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}