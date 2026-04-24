package com.akeel.aitbaar.ui.customer.vendors

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.data.model.Transaction
import com.akeel.aitbaar.ui.customer.dashboard.CustomerRecentTransactionAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

class AllVendorTransactionsFragment : Fragment(R.layout.fragment_all_vendor_transactions) {

    private var transactionsListener: ListenerRegistration? = null
    private var allTransactions: List<Transaction> = emptyList()
    private lateinit var adapter: CustomerRecentTransactionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchInput = view.findViewById<EditText>(R.id.etSearchCustomer)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rvCustomers)

        adapter = CustomerRecentTransactionAdapter()
        adapter.setActionListeners(
            onAccept = { tx -> updateTransactionDecision(tx.id, Status.ACCEPTED) },
            onReject = { tx -> updateTransactionDecision(tx.id, Status.REJECTED) }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        searchInput.addTextChangedListener { editable ->
            val query = editable?.toString().orEmpty().trim().lowercase(Locale.getDefault())
            val filtered = if (query.isBlank()) {
                allTransactions
            } else {
                allTransactions.filter {
                    it.customerName.lowercase(Locale.getDefault()).contains(query) ||
                        it.item.lowercase(Locale.getDefault()).contains(query)
                }
            }
            adapter.submitList(filtered)
        }

        observeCustomerTransactions()
    }

    private fun observeCustomerTransactions() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        transactionsListener?.remove()
        transactionsListener = db.collection("transactions")
            .whereEqualTo("customerId", uid)
            .addSnapshotListener { snapshot, _ ->
                val docs = snapshot?.documents ?: emptyList()
                val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

                allTransactions = docs.mapNotNull { doc ->
                    val displayName = doc.getString("vendorName")
                        ?: doc.getString("customerName")
                        ?: return@mapNotNull null
                    val item = doc.getString("item").orEmpty()
                    val amount = (doc.getLong("amount") ?: 0L).toInt()
                    val createdAt = doc.getTimestamp("createdAt")
                    val date = createdAt?.toDate()?.let { formatter.format(it) }.orEmpty()
                    val status = runCatching {
                        Status.valueOf(doc.getString("status").orEmpty())
                    }.getOrDefault(Status.PENDING)
                    val remoteId = doc.getString("transactionId") ?: doc.id

                    Transaction(
                        id = remoteId.toIntOrNull() ?: remoteId.hashCode(),
                        customerName = displayName,
                        item = item,
                        amount = amount,
                        date = date,
                        status = status
                    )
                }.sortedByDescending { it.id }

                adapter.submitList(allTransactions)
            }
    }

    private fun updateTransactionDecision(transactionId: Int, newStatus: Status) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("transactions")
            .whereEqualTo("customerId", uid)
            .whereEqualTo("transactionId", transactionId.toString())
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull() ?: return@addOnSuccessListener
                val current = doc.getString("status").orEmpty()
                if (current != Status.PENDING.name) return@addOnSuccessListener

                val updates = hashMapOf<String, Any>(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                when (newStatus) {
                    Status.ACCEPTED -> updates["approvedAt"] = FieldValue.serverTimestamp()
                    Status.REJECTED -> updates["rejectedAt"] = FieldValue.serverTimestamp()
                    else -> Unit
                }

                doc.reference.update(updates)
            }
    }

    override fun onDestroyView() {
        transactionsListener?.remove()
        transactionsListener = null
        super.onDestroyView()
    }
}
