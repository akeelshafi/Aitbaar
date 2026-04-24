package com.akeel.aitbaar.ui.customer.vendors

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.data.model.Transaction
import com.akeel.aitbaar.ui.customer.dashboard.CustomerRecentTransactionAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

class AllVendorTransactionsFragment : Fragment(R.layout.fragment_all_vendor_transactions) {

    private var transactionsListener: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvAllTransactions)
        val adapter = CustomerRecentTransactionAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        transactionsListener?.remove()
        transactionsListener = db.collection("transactions")
            .whereEqualTo("customerId", uid)
            .addSnapshotListener { snapshot, _ ->
                val docs = snapshot?.documents ?: emptyList()
                val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

                val allTransactions = docs.mapNotNull { doc ->
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

    override fun onDestroyView() {
        transactionsListener?.remove()
        transactionsListener = null
        super.onDestroyView()
    }
}