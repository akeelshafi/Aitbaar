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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class CustomerDashboardFragment : Fragment(R.layout.fragment_customer_dashboard) {

    private var transactionsListener: ListenerRegistration? = null

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

        transactionsListener?.remove()
        transactionsListener = db.collection("transactions")
            .whereEqualTo("customerId", uid)
            .addSnapshotListener { snapshot, _ ->
                val docs = snapshot?.documents ?: emptyList()
                val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

                data class CustomerTxn(
                    val vendorId: String,
                    val createdAtMillis: Long,
                    val transaction: Transaction
                )

                val parsedTransactions = docs.mapNotNull { doc ->
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
                    val vendorId = doc.getString("vendorId").orEmpty()

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

                adapter.submitList(transactions.take(4))
            }

        tvViewAll.setOnClickListener {
            findNavController().navigate(R.id.action_customerDashboardFragment_to_allVendorTransactionsFragment)
        }
    }

    override fun onDestroyView() {
        transactionsListener?.remove()
        transactionsListener = null
        super.onDestroyView()
    }
}
