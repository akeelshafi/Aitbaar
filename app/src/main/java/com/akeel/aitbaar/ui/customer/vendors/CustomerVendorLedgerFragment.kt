package com.akeel.aitbaar.ui.customer.vendors

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
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
import com.akeel.aitbaar.ui.customer.CustomerDataViewModel
import com.akeel.aitbaar.ui.vendor.transaction.TransactionAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class CustomerVendorLedgerFragment : Fragment(R.layout.fragment_customer_vendor_ledger) {

    private val viewModel: CustomerDataViewModel by activityViewModels()
    private lateinit var adapter: TransactionAdapter
    private lateinit var vendorName: String
    private var vendorTransactions: List<Transaction> = emptyList()
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private var selectedIndex = 0 // 0 all, 1 pending, 2 accepted, 3 rejected, 4 paid
    private var isIndicatorInitialized = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabAll = view.findViewById<TextView?>(R.id.tabAll)
        val tabPending = view.findViewById<TextView?>(R.id.tabPending)
        val tabAccepted = view.findViewById<TextView?>(R.id.tabAccepted)
        val tabRejected = view.findViewById<TextView?>(R.id.tabRejected)
        val tabPaid = view.findViewById<TextView?>(R.id.tabPaid)
        val tabIndicator = view.findViewById<View?>(R.id.tabIndicator)
        val btnMarkPaid = view.findViewById<View?>(R.id.btnMarkPaid)
        val tvTotalAmount = view.findViewById<TextView?>(R.id.tvTotalAmount)
        val tvCustomerName = view.findViewById<TextView?>(R.id.tvCustomerName)
        val recycler = view.findViewById<RecyclerView?>(R.id.rvLedger)

        if (tabAll == null || tabPending == null || tabAccepted == null || tabRejected == null ||
            tabPaid == null || tabIndicator == null || tvTotalAmount == null || tvCustomerName == null || recycler == null
        ) {
            Toast.makeText(requireContext(), "Ledger UI missing views. Rebuild app once.", Toast.LENGTH_SHORT).show()
            return
        }

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        btnMarkPaid?.visibility = View.GONE

        vendorName = arguments?.getString("vendorName").orEmpty()
        tvCustomerName.text = vendorName

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionAdapter(
            list = emptyList(),
            showActionButtons = false
        ) { }
        recycler.adapter = adapter

        val tabs = listOf(tabAll, tabPending, tabAccepted, tabRejected, tabPaid)

        fun moveIndicatorTo(tab: TextView, animate: Boolean) {
            val x = tab.x + (tab.width - tabIndicator.width) / 2f
            if (animate) {
                tabIndicator.animate().x(x).setDuration(150).start()
            } else {
                tabIndicator.x = x
            }
        }

        fun selectTab(index: Int, animate: Boolean = true) {
            selectedIndex = index
            val shouldAnimateIndicator = animate && isIndicatorInitialized
            tabs.forEachIndexed { i, textView ->
                textView.setTextColor(
                    requireContext().getColor(
                        if (i == selectedIndex) R.color.blue else R.color.gray
                    )
                )
            }
            moveIndicatorTo(tabs[selectedIndex], shouldAnimateIndicator)

            val filtered = when (selectedIndex) {
                1 -> vendorTransactions.filter { it.status == Status.PENDING }
                2 -> vendorTransactions.filter { it.status == Status.ACCEPTED }
                3 -> vendorTransactions.filter { it.status == Status.REJECTED }
                4 -> vendorTransactions.filter { it.status == Status.PAID }
                else -> vendorTransactions
            }
            adapter.submitList(sortByDate(filtered))
            isIndicatorInitialized = true
        }

        val gestureDetector = GestureDetector(requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    val start = e1 ?: return false
                    val diffX = e2.x - start.x
                    val diffY = e2.y - start.y
                    if (abs(diffX) > abs(diffY) && abs(diffX) > 90 && abs(velocityX) > 90) {
                        if (diffX < 0) {
                            selectTab((selectedIndex + 1).coerceAtMost(tabs.lastIndex))
                        } else {
                            selectTab((selectedIndex - 1).coerceAtLeast(0))
                        }
                        return true
                    }
                    return false
                }
            }
        )

        recycler.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            vendorTransactions = state.allTransactions.filter { it.customerName == vendorName }
            val accepted = vendorTransactions.filter { it.status == Status.ACCEPTED }.sumOf { it.amount }
            val paid = vendorTransactions.filter { it.status == Status.PAID }.sumOf { it.amount }
            tvTotalAmount.text = "₹ ${(accepted - paid).coerceAtLeast(0)}"
            selectTab(selectedIndex, animate = false)
        }
        viewModel.ensureLoaded()

        tabAll.setOnClickListener { selectTab(0) }
        tabPending.setOnClickListener { selectTab(1) }
        tabAccepted.setOnClickListener { selectTab(2) }
        tabRejected.setOnClickListener { selectTab(3) }
        tabPaid.setOnClickListener { selectTab(4) }

        tabAll.post { selectTab(0, animate = false) }
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
}