package com.akeel.aitbaar.data.local.db


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.akeel.aitbaar.data.local.StatusConverter
import com.akeel.aitbaar.data.local.dao.PaymentDao
import com.akeel.aitbaar.data.local.dao.TransactionDao
import com.akeel.aitbaar.data.local.entity.PaymentEntity
import com.akeel.aitbaar.data.local.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class,
        PaymentEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(StatusConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun paymentDao(): PaymentDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aitbaar_db"
                ).fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }



}
