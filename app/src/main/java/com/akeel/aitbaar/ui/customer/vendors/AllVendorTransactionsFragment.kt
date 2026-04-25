package com.akeel.aitbaar.ui.customer.vendors

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.ui.customer.CustomerDataViewModel
import com.akeel.aitbaar.ui.customer.dashboard.CustomerRecentTransactionAdapter
import com.akeel.aitbaar.ui.customer.transactions.RejectReasonBottomSheet

class AllVendorTransactionsFragment : Fragment(R.layout.fragment_all_vendor_transactions) {

    private val viewModel: CustomerDataViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvAllTransactions)
        val adapter = CustomerRecentTransactionAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

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

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.allTransactions)
        }

        viewModel.ensureLoaded()
    }
}