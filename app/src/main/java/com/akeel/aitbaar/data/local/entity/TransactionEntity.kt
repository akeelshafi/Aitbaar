package com.akeel.aitbaar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akeel.aitbaar.data.model.Status

@Entity(tableName = "transactions")
data class TransactionEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val customerName: String,
    val item: String,
    val amount: Int,
    val date: String,
    val status: Status
)