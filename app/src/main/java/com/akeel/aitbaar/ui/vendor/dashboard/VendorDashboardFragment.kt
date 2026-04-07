package com.akeel.aitbaar.ui.vendor.dashboard

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.repository.TransactionRepository
import com.akeel.aitbaar.ui.vendor.VendorNavHelper
import com.akeel.aitbaar.ui.vendor.transaction.TransactionAdapter
import com.akeel.aitbaar.utils.DashboardCache
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VendorDashboardFragment : Fragment(R.layout.fragment_vendor_dashboard) {

    private lateinit var tvWelcomeVendor: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvCustomerCount: TextView

    private lateinit var adapter: TransactionAdapter

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
        observeCustomerBalances()
        observeRecentTransactions()
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

    // 🔹 Total + Customer Count (Room)
    private fun observeCustomerBalances() {

        // ✅ Use cache first
        if (DashboardCache.totalAmount != null && DashboardCache.customerCount != null) {

            tvTotalAmount.text = "₹${DashboardCache.totalAmount}"
            tvCustomerCount.text = "From ${DashboardCache.customerCount} customers"
        }

        viewLifecycleOwner.lifecycleScope.launch {
            TransactionRepository.getCustomerBalances().collectLatest { list ->

                val total = list.sumOf { it.balance }
                val count = list.count { it.balance > 0 }

                tvTotalAmount.text = "₹$total"
                tvCustomerCount.text = "From $count customers"

                // ✅ Save in cache
                DashboardCache.totalAmount = total
                DashboardCache.customerCount = count
            }
        }
    }

    // 🔹 Recent Transactions
    private fun observeRecentTransactions() {

        viewLifecycleOwner.lifecycleScope.launch {
            TransactionRepository.getAllTransactions().collectLatest { transactions ->

                val recent = transactions.take(4)
                adapter.submitList(recent)
            }
        }
    }
}