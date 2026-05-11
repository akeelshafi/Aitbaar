package com.akeel.aitbaar.ui.vendor.dashboard

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.ui.vendor.VendorDataViewModel
import com.akeel.aitbaar.ui.vendor.VendorNavHelper
import com.akeel.aitbaar.ui.vendor.transaction.TransactionAdapter

class VendorDashboardFragment : Fragment(R.layout.fragment_vendor_dashboard) {

    private val viewModel: VendorDataViewModel by activityViewModels()
    private lateinit var adapter: TransactionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvWelcomeVendor = view.findViewById<TextView>(R.id.tvWelcomeVendor)
        val tvTotalAmount = view.findViewById<TextView>(R.id.tvTotalAmount)
        val tvCustomerCount = view.findViewById<TextView>(R.id.tvCustomerCount)
        val tvEmptyState = view.findViewById<TextView>(R.id.tvEmptyState)

        val recycler = view.findViewById<RecyclerView>(R.id.rvRecentTransactions)
        val addTransactionButton = view.findViewById<View>(R.id.btnAddTransaction)

        VendorNavHelper.setup(this, view)
        VendorNavHelper.highlight(this, view, R.id.iconHome)

        view.findViewById<ImageView>(R.id.iconHome)
            .setColorFilter(requireContext().getColor(R.color.blue))

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionAdapter(emptyList()) { transaction ->
            val bundle = Bundle().apply {
                putString("transactionId", transaction.id.toString())
            }
            findNavController().navigate(R.id.addTransactionFragment, bundle)
        }
        recycler.adapter = adapter

        addTransactionButton.setOnClickListener {
            findNavController().navigate(R.id.action_vendorDashboardFragment_to_addTransactionFragment)
        }

        view.findViewById<TextView>(R.id.tvViewAllTransaction).setOnClickListener {
            findNavController().navigate(R.id.action_vendorDashboardFragment_to_allTransactionsFragment)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            tvWelcomeVendor.text = "Welcome, ${state.vendorName}"
            tvTotalAmount.text = "₹${state.totalAcceptedAmount}"
            tvCustomerCount.text = "From ${state.acceptedCustomerCount} customers"

            adapter.submitList(state.recentTransactions)

            tvEmptyState.text = "No recent transactions"
            tvEmptyState.visibility = if (state.recentTransactions.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.ensureLoaded()
    }
}