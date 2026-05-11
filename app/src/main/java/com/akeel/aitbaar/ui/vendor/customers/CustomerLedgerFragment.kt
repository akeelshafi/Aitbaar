package com.akeel.aitbaar.ui.vendor.customers

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.data.model.Transaction
import com.akeel.aitbaar.ui.vendor.VendorDataViewModel
import com.akeel.aitbaar.ui.vendor.payment.PaymentBottomSheet
import com.akeel.aitbaar.ui.vendor.transaction.TransactionAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomerLedgerFragment : Fragment(R.layout.fragment_customer_ledger) {

    private val viewModel: VendorDataViewModel by activityViewModels()

    private lateinit var adapter: TransactionAdapter
    private lateinit var customerName: String

    private var allTransactions: List<Transaction> = emptyList()
    private var totalPaid: Int = 0

    private lateinit var tvTotalAmount: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvEmptyState: TextView

    private var currentTab: Int = 0 // 0=All,1=Pending,2=Accepted,3=Rejected,4=Paid

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

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
        tvEmptyState = view.findViewById(R.id.tvEmptyState)

        customerName = arguments?.getString("customerName").orEmpty()
        tvCustomerName.text = customerName

        val recycler = view.findViewById<RecyclerView>(R.id.rvLedger)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = TransactionAdapter(emptyList()) { transaction ->
            val bundle = Bundle().apply {
                putString("transactionId", transaction.id.toString())
            }
            findNavController().navigate(R.id.addTransactionFragment, bundle)
        }
        recycler.adapter = adapter

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }

        tabAll.setOnClickListener {
            currentTab = 0
            updateTabUI(tabAll, tabPending, tabAccepted, tabRejected, tabPaid)
            renderCurrentTab()
        }

        tabPending.setOnClickListener {
            currentTab = 1
            updateTabUI(tabPending, tabAll, tabAccepted, tabRejected, tabPaid)
            renderCurrentTab()
        }

        tabAccepted.setOnClickListener {
            currentTab = 2
            updateTabUI(tabAccepted, tabAll, tabPending, tabRejected, tabPaid)
            renderCurrentTab()
        }

        tabRejected.setOnClickListener {
            currentTab = 3
            updateTabUI(tabRejected, tabAll, tabPending, tabAccepted, tabPaid)
            renderCurrentTab()
        }

        tabPaid.setOnClickListener {
            currentTab = 4
            updateTabUI(tabPaid, tabAll, tabPending, tabAccepted, tabRejected)
            renderCurrentTab()
        }

        btnMarkPaid.setOnClickListener {
            val acceptedTotal = allTransactions.filter { it.status == Status.ACCEPTED }.sumOf { it.amount }
            val currentBalance = acceptedTotal - totalPaid

            if (currentBalance <= 0) {
                Toast.makeText(requireContext(), "No pending balance", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            PaymentBottomSheet(
                customerName = customerName,
                currentBalance = currentBalance
            ) {}.show(parentFragmentManager, "PaymentBottomSheet")
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            allTransactions = state.allTransactions.filter { it.customerName == customerName }

            // total paid from balance model
            val customerBalance = state.customerBalances.firstOrNull { it.name == customerName }
            val acceptedTotal = allTransactions.filter { it.status == Status.ACCEPTED }.sumOf { it.amount }
            val due = customerBalance?.balance ?: acceptedTotal
            totalPaid = (acceptedTotal - due).coerceAtLeast(0)

            updateTotal()
            renderCurrentTab()
        }

        viewModel.ensureLoaded()

        currentTab = 0
        updateTabUI(tabAll, tabPending, tabAccepted, tabRejected, tabPaid)
        renderCurrentTab()
    }

    private fun renderCurrentTab() {
        val displayList: List<Transaction>
        val emptyText: String

        when (currentTab) {
            1 -> {
                displayList = allTransactions.filter { it.status == Status.PENDING }
                emptyText = "No pending transactions"
            }
            2 -> {
                displayList = allTransactions.filter { it.status == Status.ACCEPTED }
                emptyText = "No accepted transactions"
            }
            3 -> {
                displayList = allTransactions.filter { it.status == Status.REJECTED }
                emptyText = "No rejected transactions"
            }
            4 -> {
                // Paid tab from accepted-paid summary, keep list empty when no paid events available
                displayList = emptyList()
                emptyText = "No paid transactions"
            }
            else -> {
                displayList = allTransactions
                emptyText = "No transactions for this customer"
            }
        }

        adapter.submitList(sortByDate(displayList))
        tvEmptyState.text = emptyText
        tvEmptyState.visibility = if (displayList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun sortByDate(list: List<Transaction>): List<Transaction> {
        return list.sortedByDescending {
            try {
                dateFormat.parse(it.date)
            } catch (_: Exception) {
                Date(0)
            }
        }
    }

    private fun updateTotal() {
        val acceptedTotal = allTransactions.filter { it.status == Status.ACCEPTED }.sumOf { it.amount }
        val totalDue = (acceptedTotal - totalPaid).coerceAtLeast(0)
        tvTotalAmount.text = "₹ $totalDue"
    }

    private fun updateTabUI(selected: TextView, vararg others: TextView) {
        selected.setTextColor(requireContext().getColor(R.color.blue))
        others.forEach { it.setTextColor(requireContext().getColor(R.color.gray)) }
    }
}