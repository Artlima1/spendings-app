package com.arthur.spending.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val value: Double,
    val date: Date,
    val category: String,
    val location: String,
    val description: String = ""
)