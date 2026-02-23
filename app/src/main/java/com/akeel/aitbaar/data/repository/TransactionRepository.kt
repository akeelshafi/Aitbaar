package com.akeel.aitbaar.data.repository

import android.content.Context
import com.akeel.aitbaar.data.local.db.AppDatabase
import com.akeel.aitbaar.data.local.entity.TransactionEntity
import com.akeel.aitbaar.data.model.CustomerBalance
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.akeel.aitbaar.data.local.dao.PaymentDao
import com.akeel.aitbaar.data.local.entity.PaymentEntity


object TransactionRepository {

    private lateinit var dao: com.akeel.aitbaar.data.local.dao.TransactionDao
    private lateinit var paymentDao: PaymentDao

    // 🔹 Initialize database (call once in app)
    fun init(context: Context) {
        val db = AppDatabase.getDatabase(context)
        dao = db.transactionDao()
        paymentDao = db.paymentDao()

    }

    // 🔹 Insert transaction into Room
    suspend fun addTransaction(transaction: Transaction) {
        val entity =
            TransactionEntity(
                customerName = transaction.customerName,
                item = transaction.item,
                amount = transaction.amount,
                date = transaction.date,
                status = transaction.status
            )
        dao.insertTransaction(entity)
    }

    // 🔹 Update transaction status
    suspend fun updateTransactionStatus(id: Int, newStatus: Status) {
        dao.updateTransactionStatus(id, newStatus)
    }


    // 🔹 Get all transactions from Room
    fun getAllTransactions(): Flow<List<Transaction>> {
        return dao.getAllTransactions().map { list ->
            list.map { entity ->
                Transaction(
                    id = entity.id,
                    customerName = entity.customerName,
                    item = entity.item,
                    amount = entity.amount,
                    date = entity.date,
                    status = entity.status
                )

            }
        }
    }

    fun getCustomerBalances(): Flow<List<CustomerBalance>> {
        return dao.getCustomerBalances()
    }

    fun getTransactionsForCustomer(name: String): Flow<List<Transaction>> {
        return dao.getTransactionsForCustomer(name).map { list ->
            list.map { entity ->
                Transaction(
                    id = entity.id,
                    customerName = entity.customerName,
                    item = entity.item,
                    amount = entity.amount,
                    date = entity.date,
                    status = entity.status
                )
            }
        }
    }

    suspend fun addPayment(customerName: String, amount: Int, date: String) {
        val payment = PaymentEntity(
            customerName = customerName,
            amount = amount,
            date = date
        )
        paymentDao.insertPayment(payment)
    }

    fun getPaymentsForCustomer(name: String) =
        paymentDao.getPaymentsForCustomer(name)


}


