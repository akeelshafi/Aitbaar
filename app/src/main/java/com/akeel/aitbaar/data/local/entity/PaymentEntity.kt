package com.akeel.aitbaar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class PaymentEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val customerName: String,

    val amount: Int,

    val date: String
)