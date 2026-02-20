package com.akeel.aitbaar.ui.vendor.customers

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.data.model.Transaction
import com.akeel.aitbaar.data.repository.TransactionRepository
import com.akeel.aitbaar.ui.vendor.transaction.TransactionAdapter
import kotlinx.coroutines.launch

class CustomerLedgerFragment : Fragment(R.layout.fragment_customer_ledger) {

    private lateinit var adapter: TransactionAdapter
    private lateinit var customerName: String
    private lateinit var recycler: RecyclerView
    private var allTransactions: List<Transaction> = emptyList()
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvCustomerName: TextView


    private fun updateTabUI(selected: TextView, vararg others: TextView) {

        // selected tab → blue
        selected.setTextColor(requireContext().getColor(R.color.blue))

        // other tabs → gray
        others.forEach {
            it.setTextColor(requireContext().getColor(R.color.gray))
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabAll = view.findViewById<TextView>(R.id.tabAll)
        val tabPending = view.findViewById<TextView>(R.id.tabPending)
        val tabAccepted = view.findViewById<TextView>(R.id.tabAccepted)
        val tabRejected = view.findViewById<TextView>(R.id.tabRejected)
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount)



        // 🔹 Get customer name from navigation
        customerName = arguments?.getString("customerName") ?: ""
        tvCustomerName = view.findViewById(R.id.tvCustomerName)
        tvCustomerName.text = customerName

        // 🔹 RecyclerView setup
        recycler = view.findViewById(R.id.rvLedger)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = TransactionAdapter(emptyList())
        recycler.adapter = adapter

        // 🔹 Start observing Room data
        observeTransactions()



        tabAll.setOnClickListener {
            adapter.submitList(allTransactions)
            updateTabUI(tabAll, tabPending, tabAccepted, tabRejected)
        }

        tabPending.setOnClickListener {
            adapter.submitList(allTransactions.filter { it.status == Status.PENDING })
            updateTabUI(tabPending, tabAll, tabAccepted, tabRejected)
        }

        tabAccepted.setOnClickListener {
            adapter.submitList(allTransactions.filter { it.status == Status.ACCEPTED })
            updateTabUI(tabAccepted, tabAll, tabPending, tabRejected)
        }

        tabRejected.setOnClickListener {
            adapter.submitList(allTransactions.filter { it.status == Status.REJECTED })
            updateTabUI(tabRejected, tabAll, tabPending, tabAccepted)
        }
        updateTabUI(tabAll, tabPending, tabAccepted, tabRejected)


    }

    private fun calculateTotal(list: List<Transaction>) {

        val accepted = list
            .filter { it.status == Status.ACCEPTED }
            .sumOf { it.amount }

        val paid = list
            .filter { it.status == Status.PAID }
            .sumOf { it.amount }

        val totalDue = accepted - paid

        tvTotalAmount.text = "₹$totalDue"
    }


    private fun observeTransactions() {
        viewLifecycleOwner.lifecycleScope.launch {
            TransactionRepository
                .getTransactionsForCustomer(customerName)
                .collect { list ->
                    allTransactions = list
                    adapter.submitList(list)

                    calculateTotal(list)   // ⭐ important line
                }

                }
        }
    }


