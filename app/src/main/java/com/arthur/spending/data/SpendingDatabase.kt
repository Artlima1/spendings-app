package com.arthur.spending.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context

@Database(
    entities = [Transaction::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class SpendingDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: SpendingDatabase? = null

        fun getDatabase(context: Context): SpendingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpendingDatabase::class.java,
                    "spending_database"
                ).fallbackToDestructiveMigration() // For now, just recreate the database
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}