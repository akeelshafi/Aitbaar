package com.akeel.aitbaar.ui.vendor.dashboard

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
import com.akeel.aitbaar.ui.vendor.VendorNavHelper
import com.akeel.aitbaar.ui.vendor.transaction.TransactionAdapter
import com.akeel.aitbaar.utils.DashboardCache
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

class VendorDashboardFragment : Fragment(R.layout.fragment_vendor_dashboard) {

    private lateinit var tvWelcomeVendor: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvCustomerCount: TextView

    private lateinit var adapter: TransactionAdapter
    private var transactionsListener: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔹 Init Views
        tvWelcomeVendor = view.findViewById(R.id.tvWelcomeVendor)
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount)
        tvCustomerCount = view.findViewById(R.id.tvCustomerCount)

        val recycler = view.findViewById<RecyclerView>(R.id.rvRecentTransactions)
        val addTransactionButton = view.findViewById<View>(R.id.btnAddTransaction)

        // 🔹 Bottom Nav
        VendorNavHelper.setup(this, view)
        VendorNavHelper.highlight(this, view, R.id.iconHome)

        view.findViewById<ImageView>(R.id.iconHome)
            .setColorFilter(requireContext().getColor(R.color.blue))

        // 🔹 Recycler Setup
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionAdapter(emptyList()) { transaction ->

            val bundle = Bundle().apply {
                putString("transactionId", transaction.id.toString())
            }

            findNavController().navigate(
                R.id.addTransactionFragment,
                bundle
            )
        }
        recycler.adapter = adapter

        // 🔹 Navigation
        addTransactionButton.setOnClickListener {
            findNavController().navigate(R.id.action_vendorDashboardFragment_to_addTransactionFragment)
        }

        view.findViewById<TextView>(R.id.tvViewAllTransaction).setOnClickListener {
            findNavController().navigate(R.id.action_vendorDashboardFragment_to_allTransactionsFragment)
        }

        // 🔥 Load Data
        loadVendorName()
        observeVendorTransactionsRealtime()
    }

    // 🔹 Vendor Name (Firestore)
    private fun loadVendorName() {

        // ✅ Use cache first
        DashboardCache.vendorName?.let {
            tvWelcomeVendor.text = "Welcome, $it"
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("vendors")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->

                val shopName = doc.getString("shopName") ?: "Vendor"

                tvWelcomeVendor.text = "Welcome, $shopName"

                // ✅ Save in cache
                DashboardCache.vendorName = shopName
            }
    }

    // 🔹 Vendor totals + recent list from Firestore (source of truth)
    private fun observeVendorTransactionsRealtime() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // ✅ Use cache first
        if (DashboardCache.totalAmount != null && DashboardCache.customerCount != null) {
            tvTotalAmount.text = "₹${DashboardCache.totalAmount}"
            tvCustomerCount.text = "From ${DashboardCache.customerCount} customers"
        }

        transactionsListener?.remove()
        transactionsListener = FirebaseFirestore.getInstance()
            .collection("transactions")
            .whereEqualTo("vendorId", uid)
            .addSnapshotListener { snapshot, _ ->
                val docs = snapshot?.documents ?: emptyList()
                val parsed = docs.mapNotNull { doc ->
                    val transactionId = doc.getString("transactionId") ?: doc.id
                    val customerName = doc.getString("customerName") ?: return@mapNotNull null
                    val item = doc.getString("item") ?: ""
                    val amount = (doc.getLong("amount") ?: 0L).toInt()
                    val status = runCatching {
                        Status.valueOf(doc.getString("status").orEmpty())
                    }.getOrDefault(Status.PENDING)

                    val createdAt = doc.getTimestamp("createdAt")
                    val date = createdAt?.toDate()?.let {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
                    } ?: ""

                    Transaction(
                        id = transactionId.toIntOrNull() ?: transactionId.hashCode(),
                        customerName = customerName,
                        item = item,
                        amount = amount,
                        date = date,
                        status = status
                    )
                }.sortedByDescending { it.id }

                val accepted = parsed.filter { it.status == Status.ACCEPTED }
                val total = accepted.sumOf { it.amount }
                val count = accepted.map { it.customerName }.distinct().count()

                tvTotalAmount.text = "₹$total"
                tvCustomerCount.text = "From $count customers"
                adapter.submitList(parsed.take(4))

                DashboardCache.totalAmount = total
                DashboardCache.customerCount = count
            }
    }

    override fun onDestroyView() {
        transactionsListener?.remove()
        transactionsListener = null
        super.onDestroyView()
    }
}
