package com.akeel.aitbaar.ui.customer.confirm

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.data.model.Transaction
import com.akeel.aitbaar.ui.customer.CustomerDataViewModel
import com.akeel.aitbaar.ui.customer.CustomerNavHelper
import com.akeel.aitbaar.ui.customer.dashboard.CustomerRecentTransactionAdapter
import com.akeel.aitbaar.ui.customer.transactions.RejectReasonBottomSheet
import kotlin.math.abs

class ConfirmFragment : Fragment(R.layout.fragment_confirm2) {

    private val viewModel: CustomerDataViewModel by activityViewModels()
    private val adapter = CustomerRecentTransactionAdapter()
    private var allTransactions = emptyList<Transaction>()
    private var selectedIndex = 0 // 0 = Pending, 1 = Accepted, 2 = Rejected
    private var isIndicatorInitialized = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvConfirm = view.findViewById<RecyclerView>(R.id.rvConfirmTransactions)
        val tabPending = view.findViewById<TextView>(R.id.tabPending)
        val tabAccepted = view.findViewById<TextView>(R.id.tabAccepted)
        val tabRejected = view.findViewById<TextView>(R.id.tabRejected)
        val tabIndicator = view.findViewById<View>(R.id.tabIndicator)
        val tvEmptyState = view.findViewById<TextView>(R.id.tvEmptyState)

        rvConfirm.layoutManager = LinearLayoutManager(requireContext())
        rvConfirm.adapter = adapter

        adapter.setActionListeners(
            onAccept = { tx ->
                adapter.markDecisionLocally(tx.id, Status.ACCEPTED)
                viewModel.updateTransactionStatus(tx.id, Status.ACCEPTED, null)
            },
            onReject = { tx ->
                RejectReasonBottomSheet { reason ->
                    adapter.markDecisionLocally(tx.id, Status.REJECTED)
                    viewModel.updateTransactionStatus(tx.id, Status.REJECTED, reason)
                }.show(parentFragmentManager, "reject_reason_sheet")
            }
        )

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

            val filtered = when (index) {
                0 -> {
                    tabPending.setTextColor(requireContext().getColor(R.color.blue))
                    tabAccepted.setTextColor(requireContext().getColor(R.color.gray))
                    tabRejected.setTextColor(requireContext().getColor(R.color.gray))
                    moveIndicatorTo(tabPending, shouldAnimateIndicator)
                    tvEmptyState.text = "No pending transactions"
                    allTransactions.filter { it.status == Status.PENDING }
                }

                1 -> {
                    tabPending.setTextColor(requireContext().getColor(R.color.gray))
                    tabAccepted.setTextColor(requireContext().getColor(R.color.blue))
                    tabRejected.setTextColor(requireContext().getColor(R.color.gray))
                    moveIndicatorTo(tabAccepted, shouldAnimateIndicator)
                    tvEmptyState.text = "No accepted transactions"
                    allTransactions.filter { it.status == Status.ACCEPTED }
                }

                else -> {
                    tabPending.setTextColor(requireContext().getColor(R.color.gray))
                    tabAccepted.setTextColor(requireContext().getColor(R.color.gray))
                    tabRejected.setTextColor(requireContext().getColor(R.color.blue))
                    moveIndicatorTo(tabRejected, shouldAnimateIndicator)
                    tvEmptyState.text = "No rejected transactions"
                    allTransactions.filter { it.status == Status.REJECTED }
                }
            }

            adapter.submitList(filtered)
            tvEmptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            isIndicatorInitialized = true
        }

        val gestureDetector = GestureDetector(
            requireContext(),
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
                            selectTab((selectedIndex + 1).coerceAtMost(2))
                        } else {
                            selectTab((selectedIndex - 1).coerceAtLeast(0))
                        }
                        return true
                    }
                    return false
                }
            }
        )

        rvConfirm.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            allTransactions = state.allTransactions
            selectTab(selectedIndex, animate = false)
        }
        viewModel.ensureLoaded()

        tabPending.setOnClickListener { selectTab(0) }
        tabAccepted.setOnClickListener { selectTab(1) }
        tabRejected.setOnClickListener { selectTab(2) }

        tabPending.post { selectTab(0, animate = false) }

        CustomerNavHelper.setup(this, view)
        CustomerNavHelper.highlight(this, view, R.id.tabConfirm)
    }
}