package com.akeel.aitbaar.data.model

data class Transaction(
    val id: Int = 0,
    val customerName: String,
    val item: String,
    val amount: Int,
    val date: String,
    val status: Status
)

enum class Status {
    PENDING,
    ACCEPTED,
    REJECTED
}

