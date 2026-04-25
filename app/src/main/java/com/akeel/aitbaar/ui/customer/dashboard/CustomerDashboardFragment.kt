package com.akeel.aitbaar.ui.customer.dashboard

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.data.model.Transaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import com.akeel.aitbaar.ui.customer.transactions.RejectReasonBottomSheet
import com.google.firebase.firestore.FieldValue

class CustomerDashboardFragment : Fragment(R.layout.fragment_customer_dashboard) {

    private var transactionsListener: ListenerRegistration? = null
    private val vendorNameCache = mutableMapOf<String, String>()
    private val vendorFetchInFlight = mutableSetOf<String>()
    private var latestDocs: List<DocumentSnapshot> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val tvPhone = view.findViewById<TextView>(R.id.tvPhone)
        val tvTotalDue = view.findViewById<TextView>(R.id.tvTotalDueAmount)
        val tvDueSubtitle = view.findViewById<TextView>(R.id.tvDueSubtitle)
        val tvViewAll = view.findViewById<TextView>(R.id.tvViewAll)
        val imgUser = view.findViewById<ImageView>(R.id.imgUser)
        val rvRecent = view.findViewById<RecyclerView>(R.id.rvVendors)

        val adapter = CustomerRecentTransactionAdapter()
        rvRecent.layoutManager = LinearLayoutManager(requireContext())
        rvRecent.adapter = adapter

        adapter.setActionListeners(
            onAccept = { tx ->
                adapter.markDecisionLocally(tx.id, Status.ACCEPTED)
                updateTransactionDecision(tx.id, Status.ACCEPTED, null)
            },
            onReject = { tx ->
                RejectReasonBottomSheet { reason ->
                    adapter.markDecisionLocally(tx.id, Status.REJECTED)
                    updateTransactionDecision(tx.id, Status.REJECTED, reason)
                }.show(parentFragmentManager, "reject_reason_sheet")
            }
        )
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("customers")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name").orEmpty().ifBlank { "Customer" }
                val phone = doc.getString("phoneNumber").orEmpty()
                val profilePath = doc.getString("profileImagePath").orEmpty()

                tvWelcome.text = "Welcome, $name"
                if (phone.isNotBlank()) tvPhone.text = phone

                if (profilePath.isNotBlank()) {
                    val file = File(profilePath)
                    if (file.exists()) {
                        imgUser.setImageURI(android.net.Uri.fromFile(file))
                    }
                }
            }

        fun preloadVendorNames(vendorIds: Set<String>, onComplete: () -> Unit) {
            if (vendorIds.isEmpty()) {
                onComplete()
                return
            }

            var remaining = vendorIds.size
            fun doneOne() {
                remaining -= 1
                if (remaining == 0) onComplete()
            }

            vendorIds.forEach { vendorId ->
                if (vendorNameCache.containsKey(vendorId) || vendorFetchInFlight.contains(vendorId)) {
                    doneOne()
                    return@forEach
                }

                vendorFetchInFlight.add(vendorId)
                db.collection("vendors")
                    .document(vendorId)
                    .get()
                    .addOnSuccessListener { vendorDoc ->
                        val resolved =
                            vendorDoc.getString("shopName").orEmpty().ifBlank { "Unknown Vendor" }
                        vendorNameCache[vendorId] = resolved
                        vendorFetchInFlight.remove(vendorId)
                        doneOne()
                    }
                    .addOnFailureListener {
                        vendorNameCache[vendorId] = "Unknown Vendor"
                        vendorFetchInFlight.remove(vendorId)
                        doneOne()
                    }
            }
        }

        fun renderTransactions() {
            val missingVendorIds = latestDocs.mapNotNull { doc ->
                val vendorId = doc.getString("vendorId").orEmpty()
                val vendorName = doc.getString("vendorName").orEmpty()
                if (vendorId.isNotBlank() && vendorName.isBlank() && vendorNameCache[vendorId].isNullOrBlank()) {
                    vendorId
                } else null
            }.toSet()

            preloadVendorNames(missingVendorIds) {
                val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

                data class CustomerTxn(
                    val vendorId: String,
                    val createdAtMillis: Long,
                    val transaction: Transaction
                )

                val parsedTransactions = latestDocs.mapNotNull { doc ->
                    val vendorId = doc.getString("vendorId").orEmpty()
                    val displayName = doc.getString("vendorName")
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
                    CustomerTxn(
                        vendorId = vendorId,
                        createdAtMillis = createdAt?.toDate()?.time ?: 0L,
                        transaction = Transaction(
                            id = remoteId.toIntOrNull() ?: remoteId.hashCode(),
                            customerName = displayName,
                            item = item,
                            amount = amount,
                            date = date,
                            status = status
                        )
                    )
                }.sortedByDescending { it.createdAtMillis }

                val transactions = parsedTransactions.map { it.transaction }

                val acceptedTransactions = transactions.filter { it.status == Status.ACCEPTED }
                val totalDue = acceptedTransactions.sumOf { it.amount }

                val distinctVendorCount = parsedTransactions
                    .map { it.vendorId }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .count()

                tvTotalDue.text = "Rs $totalDue"
                tvDueSubtitle.text = "You owe $distinctVendorCount vendors"

                // Home: only 4
                adapter.submitList(transactions.take(4))
            }
        }

        transactionsListener?.remove()
        transactionsListener = db.collection("transactions")
            .whereEqualTo("customerId", uid)
            .addSnapshotListener { snapshot, _ ->
                latestDocs = snapshot?.documents ?: emptyList()
                renderTransactions()
            }

        tvViewAll.setOnClickListener {
            findNavController().navigate(R.id.action_customerDashboardFragment_to_allVendorTransactionsFragment)
        }
    }

    private fun updateTransactionDecision(transactionId: Int, newStatus: Status, reason: String?) {
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
                    Status.REJECTED -> {
                        updates["rejectedAt"] = FieldValue.serverTimestamp()
                        updates["rejectionReason"] = reason ?: "Other"
                    }

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