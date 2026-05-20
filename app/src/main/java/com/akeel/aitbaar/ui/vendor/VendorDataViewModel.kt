package com.akeel.aitbaar.ui.vendor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.akeel.aitbaar.data.model.CustomerBalance
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.data.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

data class VendorUiState(
    val loading: Boolean = true,
    val vendorName: String = "Vendor",
    val shopName: String = "Aitbaar Business",
    val profileImagePath: String = "",
    val profileImageBase64: String = "",
    val recentTransactions: List<Transaction> = emptyList(),
    val allTransactions: List<Transaction> = emptyList(),
    val customerBalances: List<CustomerBalance> = emptyList(),
    val totalAcceptedAmount: Int = 0,
    val acceptedCustomerCount: Int = 0,
    val loadedOnce: Boolean = false
)

class VendorDataViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _uiState = MutableLiveData(VendorUiState())
    val uiState: LiveData<VendorUiState> = _uiState

    private var hasLoaded = false
    private var txListener: ListenerRegistration? = null
    private var paymentsListener: ListenerRegistration? = null

    private var vendorName: String = "Vendor"
    private var shopName: String = "Aitbaar Business"
    private var profileImagePath: String = ""
    private var profileImageBase64: String = ""

    private var txList: List<Transaction> = emptyList()
    private var paymentTxList: List<Transaction> = emptyList()
    private var paymentsByCustomer: Map<String, Int> = emptyMap()

    fun ensureLoaded(forceRefresh: Boolean = false) {
        if (hasLoaded && !forceRefresh) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        if (forceRefresh) {
            txListener?.remove()
            paymentsListener?.remove()
            txList = emptyList()
            paymentTxList = emptyList()
            paymentsByCustomer = emptyMap()
        }

        hasLoaded = true
        _uiState.value = _uiState.value?.copy(loading = true)

        // Load profile once
        db.collection("vendors").document(uid).get()
            .addOnSuccessListener { doc ->
                vendorName = doc.getString("name").orEmpty().ifBlank { "Vendor" }
                shopName = doc.getString("shopName").orEmpty().ifBlank { "Aitbaar Business" }
                profileImagePath = doc.getString("profileImagePath").orEmpty()
                profileImageBase64 = doc.getString("profileImageBase64").orEmpty()
                publish()
            }
            .addOnFailureListener {
                vendorName = "Vendor"
                shopName = "Aitbaar Business"
                profileImagePath = ""
                profileImageBase64 = ""
                publish()
            }

        txListener?.remove()
        txListener = db.collection("transactions")
            .whereEqualTo("vendorId", uid)
            .addSnapshotListener { snapshot, _ ->
                val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                txList = snapshot?.documents.orEmpty().map { doc ->
                    val remoteId = doc.getString("transactionId") ?: doc.id
                    val status = runCatching {
                        Status.valueOf(doc.getString("status").orEmpty())
                    }.getOrDefault(Status.PENDING)

                    val originalItem = doc.getString("item").orEmpty()
                    val rejectionReason = doc.getString("rejectionReason").orEmpty().trim()
                    val itemForUi = if (status == Status.REJECTED && rejectionReason.isNotBlank()) {
                        "$originalItem\nReason: $rejectionReason"
                    } else {
                        originalItem
                    }

                    val createdAt = doc.getTimestamp("createdAt")
                    Transaction(
                        id = remoteId.toIntOrNull() ?: doc.id.hashCode(),
                        customerName = doc.getString("customerName").orEmpty(),
                        item = itemForUi,
                        amount = (doc.getLong("amount") ?: 0L).toInt(),
                        date = createdAt?.toDate()?.let { formatter.format(it) }.orEmpty(),
                        status = status
                    )
                }.sortedByDescending { it.id }
                publish()
            }

        paymentsListener?.remove()
        paymentsListener = db.collection("payments")
            .whereEqualTo("vendorId", uid)
            .addSnapshotListener { snapshot, _ ->
                val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val docs = snapshot?.documents.orEmpty()

                paymentsByCustomer = docs
                    .groupBy { it.getString("customerName").orEmpty() }
                    .mapValues { (_, paymentDocs) ->
                        paymentDocs.sumOf { (it.getLong("amount") ?: 0L).toInt() }
                    }

                paymentTxList = docs.map { doc ->
                    val createdAt = doc.getTimestamp("createdAt")
                    Transaction(
                        id = doc.id.hashCode(),
                        customerName = doc.getString("customerName").orEmpty(),
                        item = "Payment Received",
                        amount = (doc.getLong("amount") ?: 0L).toInt(),
                        date = createdAt?.toDate()?.let { formatter.format(it) }.orEmpty(),
                        status = Status.PAID
                    )
                }.sortedByDescending { it.id }

                publish()
            }
    }

    private fun publish() {
        val accepted = txList.filter { it.status == Status.ACCEPTED }
        val totalAccepted = accepted.sumOf { it.amount }
        val acceptedCustomerCount = accepted.map { it.customerName }.distinct().count()

        val allCustomerNames = txList.map { it.customerName }.filter { it.isNotBlank() }.distinct()
        val balances = allCustomerNames.map { name ->
            val acceptedAmount = accepted.filter { it.customerName == name }.sumOf { it.amount }
            val paidAmount = paymentsByCustomer[name] ?: 0
            CustomerBalance(
                name = name,
                balance = (acceptedAmount - paidAmount).coerceAtLeast(0)
            )
        }.sortedBy { it.name.lowercase(Locale.getDefault()) }

        val mergedTransactions = (txList + paymentTxList).sortedByDescending { it.id }

        _uiState.value = VendorUiState(
            loading = false,
            vendorName = vendorName,
            shopName = shopName,
            profileImagePath = profileImagePath,
            profileImageBase64 = profileImageBase64,
            recentTransactions = mergedTransactions.take(4),
            allTransactions = mergedTransactions,
            customerBalances = balances,
            totalAcceptedAmount = totalAccepted,
            acceptedCustomerCount = acceptedCustomerCount,
            loadedOnce = true
        )
    }

    override fun onCleared() {
        super.onCleared()
        txListener?.remove()
        paymentsListener?.remove()
    }
}