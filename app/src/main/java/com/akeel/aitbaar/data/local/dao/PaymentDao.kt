package com.akeel.aitbaar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akeel.aitbaar.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE customerName = :customerName ORDER BY id DESC")
    fun getPaymentsForCustomer(customerName: String): Flow<List<PaymentEntity>>

    @Query("SELECT SUM(amount) FROM payments WHERE customerName = :customerName")
    fun getTotalPaid(customerName: String): Flow<Int?>
}