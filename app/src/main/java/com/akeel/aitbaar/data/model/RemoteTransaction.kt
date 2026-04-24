package com.akeel.aitbaar.data.model

import com.google.firebase.Timestamp

data class RemoteTransaction(
    val transactionId: String = "",
    val vendorId: String = "",
    val vendorName: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val item: String = "",
    val amount: Int = 0,
    val currency: String = DEFAULT_CURRENCY,
    val status: Status = Status.PENDING,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val approvedAt: Timestamp? = null,
    val rejectedAt: Timestamp? = null,
    val rejectionReason: String? = null,
    val paidAt: Timestamp? = null
) {
    companion object {
        const val DEFAULT_CURRENCY = "INR"
    }
}
