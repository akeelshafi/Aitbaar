package com.akeel.aitbaar.ui.customer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.akeel.aitbaar.data.model.CustomerBalance
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.data.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

data class CustomerUiState(
    val loading: Boolean = true,
    val name: String = "Customer",
    val phone: String = "",
    val profilePath: String = "",
    val profileImageBase64: String = "",
    val recentTransactions: List<Transaction> = emptyList(),
    val allTransactions: List<Transaction> = emptyList(),
    val vendorBalances: List<CustomerBalance> = emptyList(),
    val totalDue: Int = 0,
    val vendorCount: Int = 0,
    val loadedOnce: Boolean = false
)

class CustomerDataViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _uiState = MutableLiveData(CustomerUiState())
    val uiState: LiveData<CustomerUiState> = _uiState

    private var hasLoaded = false
    private var allTransactions: List<CustomerTxRecord> = emptyList()
    private val txDocIdByUiId = mutableMapOf<Int, String>()

    // Realtime listeners
    private var transactionsListener: ListenerRegistration? = null
    private var paymentsListener: ListenerRegistration? = null

    // Cache realtime streams before merge
    private var transactionRecords: List<CustomerTxRecord> = emptyList()
    private var paymentRecords: List<CustomerTxRecord> = emptyList()

    // Profile cache
    private var customerName: String = "Customer"
    private var customerPhone: String = ""
    private var customerProfilePath: String = ""
    private var customerProfileImageBase64: String = ""

    fun ensureLoaded(forceRefresh: Boolean = false) {
        if (hasLoaded && !forceRefresh) return

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        if (forceRefresh) {
            transactionsListener?.remove()
            paymentsListener?.remove()
            transactionsListener = null
            paymentsListener = null
            transactionRecords = emptyList()
            paymentRecords = emptyList()
        }

        hasLoaded = true
        _uiState.value = _uiState.value?.copy(loading = true)

        loadCustomerProfile(uid) { name, phone, profilePath, profileImageBase64 ->
            customerName = name
            customerPhone = phone
            customerProfilePath = profilePath
            customerProfileImageBase64 = profileImageBase64
            publishMergedState()
        }

        attachRealtimeTransactions(uid)
        attachRealtimePayments(uid)
    }

    fun updateTransactionStatus(uiId: Int, newStatus: Status, reason: String?) {
        val docId = txDocIdByUiId[uiId] ?: return
        val current = allTransactions.firstOrNull { it.uiId == uiId } ?: return
        if (current.status != Status.PENDING) return

        val updates = hashMapOf<String, Any>(
            "status" to newStatus.name,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        when (newStatus) {
            Status.ACCEPTED -> updates["approvedAt"] = FieldValue.serverTimestamp()
            Status.REJECTED -> {
                updates["rejectedAt"] = FieldValue.serverTimestamp()
                updates["rejectionReason"] = reason ?: "Other"
            }
            else -> Unit
        }

        // Optimistic UI
        transactionRecords = transactionRecords.map { tx ->
            if (tx.uiId == uiId) tx.copy(status = newStatus) else tx
        }
        publishMergedState()

        db.collection("transactions")
            .document(docId)
            .update(updates)
            .addOnFailureListener {
                // rollback
                transactionRecords = transactionRecords.map { tx ->
                    if (tx.uiId == uiId) tx.copy(status = current.status) else tx
                }
                publishMergedState()
            }
    }

    private fun loadCustomerProfile(
        uid: String,
        onComplete: (name: String, phone: String, profilePath: String, profileImageBase64: String) -> Unit
    ) {
        db.collection("customers")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name").orEmpty().ifBlank { "Customer" }
                val phone = doc.getString("phoneNumber").orEmpty()
                val profilePath = doc.getString("profileImagePath").orEmpty()
                val profileImageBase64 = doc.getString("profileImageBase64").orEmpty()
                onComplete(name, phone, profilePath, profileImageBase64)
            }
            .addOnFailureListener {
                onComplete("Customer", "", "", "")
            }
    }

    private fun attachRealtimeTransactions(uid: String) {
        transactionsListener?.remove()
        transactionsListener = db.collection("transactions")
            .whereEqualTo("customerId", uid)
            .addSnapshotListener { snapshot, _ ->
                val docs = snapshot?.documents.orEmpty()
                if (docs.isEmpty()) {
                    transactionRecords = emptyList()
                    publishMergedState()
                    return@addSnapshotListener
                }

                val vendorNameCache = mutableMapOf<String, String>()
                val missingVendorIds = docs.mapNotNull { doc ->
                    val vendorId = doc.getString("vendorId").orEmpty()
                    val vendorName = doc.getString("vendorName").orEmpty()
                    when {
                        vendorId.isBlank() -> null
                        vendorName.isNotBlank() -> {
                            vendorNameCache[vendorId] = vendorName
                            null
                        }
                        else -> vendorId
                    }
                }.toSet()

                resolveVendorNames(missingVendorIds) { fetchedNames ->
                    vendorNameCache.putAll(fetchedNames)

                    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    transactionRecords = docs.map { doc ->
                        val vendorId = doc.getString("vendorId").orEmpty()
                        val vendorName = doc.getString("vendorName")
                            .orEmpty()
                            .ifBlank { vendorNameCache[vendorId].orEmpty() }
                            .ifBlank { "Unknown Vendor" }

                        val item = doc.getString("item").orEmpty()
                        val amount = (doc.getLong("amount") ?: 0L).toInt()
                        val createdAt = doc.getTimestamp("createdAt")
                        val date = createdAt?.toDate()?.let { formatter.format(it) }.orEmpty()
                        val status = runCatching {
                            Status.valueOf(doc.getString("status").orEmpty())
                        }.getOrDefault(Status.PENDING)
                        val remoteId = doc.getString("transactionId") ?: doc.id
                        val uiId = remoteId.toIntOrNull() ?: doc.id.hashCode()

                        CustomerTxRecord(
                            uiId = uiId,
                            docId = doc.id,
                            vendorId = vendorId,
                            vendorName = vendorName,
                            item = item,
                            amount = amount,
                            date = date,
                            status = status,
                            createdAtMillis = createdAt?.toDate()?.time ?: 0L,
                            isPayment = false
                        )
                    }.sortedByDescending { it.createdAtMillis }

                    publishMergedState()
                }
            }
    }

    private fun attachRealtimePayments(uid: String) {
        paymentsListener?.remove()
        paymentsListener = db.collection("payments")
            .whereEqualTo("customerId", uid)
            .addSnapshotListener { snapshot, _ ->
                val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                paymentRecords = snapshot?.documents.orEmpty().map { doc ->
                    val vendorName = doc.getString("vendorName").orEmpty().ifBlank { "Unknown Vendor" }
                    val amount = (doc.getLong("amount") ?: 0L).toInt()
                    val createdAt = doc.getTimestamp("createdAt")
                    val date = createdAt?.toDate()?.let { formatter.format(it) }.orEmpty()

                    CustomerTxRecord(
                        uiId = doc.id.hashCode(),
                        docId = doc.id,
                        vendorId = doc.getString("vendorId").orEmpty(),
                        vendorName = vendorName,
                        item = "Payment Received",
                        amount = amount,
                        date = date,
                        status = Status.PAID,
                        createdAtMillis = createdAt?.toDate()?.time ?: 0L,
                        isPayment = true
                    )
                }

                publishMergedState()
            }
    }

    private fun publishMergedState() {
        val merged = (transactionRecords + paymentRecords)
            .sortedByDescending { it.createdAtMillis }

        allTransactions = merged
        txDocIdByUiId.clear()
        merged.filter { !it.isPayment }.forEach { tx ->
            txDocIdByUiId[tx.uiId] = tx.docId
        }

        publishState(
            name = customerName,
            phone = customerPhone,
            profilePath = customerProfilePath,
            profileImageBase64 = customerProfileImageBase64
        )
    }

    private fun resolveVendorNames(
        vendorIds: Set<String>,
        onComplete: (Map<String, String>) -> Unit
    ) {
        if (vendorIds.isEmpty()) {
            onComplete(emptyMap())
            return
        }

        val names = mutableMapOf<String, String>()
        var remaining = vendorIds.size

        fun done() {
            remaining -= 1
            if (remaining == 0) onComplete(names)
        }

        vendorIds.forEach { vendorId ->
            db.collection("vendors")
                .document(vendorId)
                .get()
                .addOnSuccessListener { doc ->
                    names[vendorId] = doc.getString("shopName").orEmpty().ifBlank { "Unknown Vendor" }
                    done()
                }
                .addOnFailureListener {
                    names[vendorId] = "Unknown Vendor"
                    done()
                }
        }
    }

    private fun publishState(
        name: String,
        phone: String,
        profilePath: String,
        profileImageBase64: String
    ) {
        val acceptedTransactions = allTransactions.filter { it.status == Status.ACCEPTED }
        val paidTransactions = allTransactions.filter { it.isPayment || it.status == Status.PAID }

        val totalDue = (acceptedTransactions.sumOf { it.amount } - paidTransactions.sumOf { it.amount })
            .coerceAtLeast(0)

        val vendorBalances = allTransactions
            .groupBy { it.vendorName }
            .map { (vendorName, records) ->
                val accepted = records.filter { it.status == Status.ACCEPTED }.sumOf { it.amount }
                val paid = records.filter { it.isPayment || it.status == Status.PAID }.sumOf { it.amount }
                val due = (accepted - paid).coerceAtLeast(0)
                CustomerBalance(name = vendorName, balance = due)
            }
            .sortedBy { it.name.lowercase(Locale.getDefault()) }

        _uiState.value = CustomerUiState(
            loading = false,
            name = name,
            phone = phone,
            profilePath = profilePath,
            profileImageBase64 = profileImageBase64,
            recentTransactions = allTransactions.take(4).map { record ->
                Transaction(
                    id = record.uiId,
                    customerName = record.vendorName,
                    item = record.item,
                    amount = record.amount,
                    date = record.date,
                    status = record.status
                )
            },
            allTransactions = allTransactions.map { record ->
                Transaction(
                    id = record.uiId,
                    customerName = record.vendorName,
                    item = record.item,
                    amount = record.amount,
                    date = record.date,
                    status = record.status
                )
            },
            vendorBalances = vendorBalances,
            totalDue = totalDue,
            vendorCount = allTransactions.map { it.vendorId }.filter { it.isNotBlank() }.distinct().count(),
            loadedOnce = true
        )
    }

    override fun onCleared() {
        super.onCleared()
        transactionsListener?.remove()
        paymentsListener?.remove()
        transactionsListener = null
        paymentsListener = null
    }
}

private data class CustomerTxRecord(
    val uiId: Int,
    val docId: String,
    val vendorId: String,
    val vendorName: String,
    val item: String,
    val amount: Int,
    val date: String,
    val status: Status,
    val createdAtMillis: Long,
    val isPayment: Boolean
)