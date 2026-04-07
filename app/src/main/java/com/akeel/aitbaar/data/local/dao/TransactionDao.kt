package com.akeel.aitbaar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.akeel.aitbaar.data.local.entity.TransactionEntity
import com.akeel.aitbaar.data.model.CustomerBalance
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    // 🔹 Insert new transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    // 🔹 Get all transactions (latest first)
    @Query("SELECT * FROM transactions ORDER BY id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("UPDATE transactions SET status = :newStatus WHERE id = :transactionId")
    suspend fun updateTransactionStatus(transactionId: Int, newStatus: Status)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("""
SELECT t.customerName AS name,
(
    SUM(CASE WHEN t.status = 'ACCEPTED' THEN t.amount ELSE 0 END)
    -
    IFNULL((
        SELECT SUM(p.amount)
        FROM payments p
        WHERE p.customerName = t.customerName
    ), 0)
) AS balance
FROM transactions t
GROUP BY t.customerName
ORDER BY t.customerName ASC
""")
    fun getCustomerBalances(): Flow<List<CustomerBalance>>

    @Query("SELECT * FROM transactions WHERE customerName = :customerName ORDER BY id DESC")
    fun getTransactionsForCustomer(customerName: String): Flow<List<TransactionEntity>>




}
