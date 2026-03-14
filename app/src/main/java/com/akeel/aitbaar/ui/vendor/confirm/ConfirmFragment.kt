package com.akeel.aitbaar.ui.vendor.confirm

import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.data.model.Transaction
import com.akeel.aitbaar.data.repository.TransactionRepository
import com.akeel.aitbaar.ui.vendor.VendorNavHelper
import com.akeel.aitbaar.ui.vendor.transaction.TransactionAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ConfirmFragment : Fragment(R.layout.fragment_confirm) {

    private lateinit var adapter: TransactionAdapter
    private var allTransactions: List<Transaction> = emptyList()

    private lateinit var tabPending: TextView
    private lateinit var tabAccepted: TextView
    private lateinit var tabRejected: TextView
    private lateinit var indicator: View
    private lateinit var recycler: RecyclerView

    private var currentTabIndex = 0 // 0=Pending, 1=Accepted, 2=Rejected

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabPending = view.findViewById(R.id.tabPending)
        tabAccepted = view.findViewById(R.id.tabAccepted)
        tabRejected = view.findViewById(R.id.tabRejected)
        indicator = view.findViewById(R.id.tabIndicator)
        recycler = view.findViewById(R.id.rvConfirmTransactions)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionAdapter(emptyList())
        recycler.adapter = adapter

        observeTransactions()

        // 🔥 Default Tab
        switchTab(0, animate = false)

        // 🔹 Tab Clicks
        tabPending.setOnClickListener { switchTab(0) }
        tabAccepted.setOnClickListener { switchTab(1) }
        tabRejected.setOnClickListener { switchTab(2) }

        // 🔥 Swipe Support
        recycler.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {

            override fun onSwipeLeft() {
                if (currentTabIndex < 2) {
                    switchTab(currentTabIndex + 1)
                }
            }

            override fun onSwipeRight() {
                if (currentTabIndex > 0) {
                    switchTab(currentTabIndex - 1)
                }
            }
        })

        // 🔹 Bottom Nav Highlight
        VendorNavHelper.setup(this, view)
        VendorNavHelper.highlight(this, view, R.id.iconConfirm)

        view.findViewById<ImageView>(R.id.iconConfirm)
            .setColorFilter(requireContext().getColor(R.color.blue))
    }

    private fun observeTransactions() {
        viewLifecycleOwner.lifecycleScope.launch {
            TransactionRepository
                .getAllTransactions()
                .collectLatest { list ->
                    allTransactions = list
                    switchTab(currentTabIndex, animate = false)
                }
        }
    }

    // 🔥 Main Tab Switching Logic
    private fun switchTab(index: Int, animate: Boolean = true) {

        currentTabIndex = index

        when (index) {
            0 -> {
                adapter.submitList(allTransactions.filter { it.status == Status.PENDING })
                updateTabUI(tabPending, tabAccepted, tabRejected)
                moveIndicator(tabPending, animate)
            }

            1 -> {
                adapter.submitList(allTransactions.filter { it.status == Status.ACCEPTED })
                updateTabUI(tabAccepted, tabPending, tabRejected)
                moveIndicator(tabAccepted, animate)
            }

            2 -> {
                adapter.submitList(allTransactions.filter { it.status == Status.REJECTED })
                updateTabUI(tabRejected, tabPending, tabAccepted)
                moveIndicator(tabRejected, animate)
            }
        }
    }

    private fun updateTabUI(selected: TextView, vararg others: TextView) {
        selected.setTextColor(requireContext().getColor(R.color.blue))
        others.forEach {
            it.setTextColor(requireContext().getColor(R.color.gray))
        }
    }

    private fun moveIndicator(tab: TextView, animate: Boolean) {

        tab.post {
            val centerX = tab.x + tab.width / 2
            val targetX = centerX - indicator.width / 2

            if (animate) {
                indicator.animate()
                    .x(targetX)
                    .setDuration(250)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            } else {
                indicator.x = targetX
            }
        }
    }
}