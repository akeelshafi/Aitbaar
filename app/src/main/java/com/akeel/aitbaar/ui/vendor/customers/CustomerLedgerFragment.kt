package com.akeel.aitbaar.ui.vendor.customers

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.local.entity.PaymentEntity
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.data.model.Transaction
import com.akeel.aitbaar.data.repository.TransactionRepository
import com.akeel.aitbaar.ui.vendor.transaction.TransactionAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CustomerLedgerFragment : Fragment(R.layout.fragment_customer_ledger) {

    private lateinit var adapter: TransactionAdapter
    private lateinit var customerName: String
    private lateinit var recycler: RecyclerView

    private var allTransactions: List<Transaction> = emptyList()
    private var allPayments: List<PaymentEntity> = emptyList()
    private var totalPaid: Int = 0

    private lateinit var tvTotalAmount: TextView
    private lateinit var tvCustomerName: TextView

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    private fun updateTabUI(selected: TextView, vararg others: TextView) {
        selected.setTextColor(requireContext().getColor(R.color.blue))
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
        val tabPaid = view.findViewById<TextView>(R.id.tabPaid)
        val btnMarkPaid = view.findViewById<TextView>(R.id.btnMarkPaid)

        tvTotalAmount = view.findViewById(R.id.tvTotalAmount)
        tvCustomerName = view.findViewById(R.id.tvCustomerName)

        customerName = arguments?.getString("customerName") ?: ""
        tvCustomerName.text = customerName

        viewLifecycleOwner.lifecycleScope.launch {
            TransactionRepository.addTransaction(
                Transaction(
                    customerName = customerName,
                    item = "Dummy Test Transaction",
                    amount = 100,
                    date = "21 Feb 2026",
                    status = Status.ACCEPTED
                )
            )
            TransactionRepository.addTransaction(
                Transaction(
                    customerName = customerName,
                    item = "Dummy Test Transaction",
                    amount = 100,
                    date = "21 Feb 2026",
                    status = Status.REJECTED
                )
            )
        }

        recycler = view.findViewById(R.id.rvLedger)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = TransactionAdapter(emptyList())
        recycler.adapter = adapter

        observeTransactions()
        observePayments()

        // 🔹 ALL TAB
        tabAll.setOnClickListener {
            adapter.submitList(sortByDate(allTransactions))
            updateTabUI(tabAll, tabPending, tabAccepted, tabRejected, tabPaid)
        }

        // 🔹 Pending
        tabPending.setOnClickListener {
            val list = allTransactions.filter { it.status == Status.PENDING }
            adapter.submitList(sortByDate(list))
            updateTabUI(tabPending, tabAll, tabAccepted, tabRejected, tabPaid)
        }

        // 🔹 Accepted
        tabAccepted.setOnClickListener {
            val list = allTransactions.filter { it.status == Status.ACCEPTED }
            adapter.submitList(sortByDate(list))
            updateTabUI(tabAccepted, tabAll, tabPending, tabRejected, tabPaid)
        }

        // 🔹 Rejected
        tabRejected.setOnClickListener {
            val list = allTransactions.filter { it.status == Status.REJECTED }
            adapter.submitList(sortByDate(list))
            updateTabUI(tabRejected, tabAll, tabPending, tabAccepted, tabPaid)
        }

        // 🔹 Paid
        tabPaid.setOnClickListener {
            val paymentDisplayList = allPayments.map {
                Transaction(
                    id = it.id,
                    customerName = it.customerName,
                    item = "Payment Received",
                    amount = it.amount,
                    date = it.date,
                    status = Status.PAID
                )
            }

            adapter.submitList(sortByDate(paymentDisplayList))
            updateTabUI(tabPaid, tabAll, tabPending, tabAccepted, tabRejected)
        }

        // 🔹 Mark as Paid
        btnMarkPaid.setOnClickListener {

            val acceptedTotal = allTransactions
                .filter { it.status == Status.ACCEPTED }
                .sumOf { it.amount }

            val currentBalance = acceptedTotal - totalPaid

            if (currentBalance <= 0) {
                Toast.makeText(requireContext(), "No pending balance", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val bottomSheet = PaymentBottomSheet(
                customerName = customerName,
                currentBalance = currentBalance
            ) {}

            bottomSheet.show(parentFragmentManager, "PaymentBottomSheet")
        }

        // 🔥 Default highlight on open
        updateTabUI(tabAll, tabPending, tabAccepted, tabRejected, tabPaid)
    }

    private fun observeTransactions() {
        viewLifecycleOwner.lifecycleScope.launch {
            TransactionRepository
                .getTransactionsForCustomer(customerName)
                .collectLatest { list ->
                    allTransactions = list
                    adapter.submitList(sortByDate(allTransactions))
                    updateTotal()
                }
        }
    }

    private fun observePayments() {
        viewLifecycleOwner.lifecycleScope.launch {
            TransactionRepository
                .getPaymentsForCustomer(customerName)
                .collectLatest { payments ->
                    allPayments = payments
                    totalPaid = payments.sumOf { it.amount }
                    updateTotal()
                }
        }
    }

    private fun sortByDate(list: List<Transaction>): List<Transaction> {
        return list.sortedByDescending {
            try {
                dateFormat.parse(it.date)
            } catch (e: Exception) {
                Date(0)
            }
        }
    }

    private fun updateTotal() {
        val acceptedTotal = allTransactions
            .filter { it.status == Status.ACCEPTED }
            .sumOf { it.amount }

        val totalDue = acceptedTotal - totalPaid
        tvTotalAmount.text = "₹ $totalDue"
    }
}