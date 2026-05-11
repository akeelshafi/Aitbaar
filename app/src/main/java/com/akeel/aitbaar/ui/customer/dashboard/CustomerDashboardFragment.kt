package com.akeel.aitbaar.ui.customer.dashboard

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.ui.customer.CustomerDataViewModel
import com.akeel.aitbaar.ui.customer.CustomerNavHelper
import com.akeel.aitbaar.ui.customer.transactions.RejectReasonBottomSheet

class CustomerDashboardFragment : Fragment(R.layout.fragment_customer_dashboard) {

    private val viewModel: CustomerDataViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val tvPhone = view.findViewById<TextView>(R.id.tvPhone)
        val tvTotalDue = view.findViewById<TextView>(R.id.tvTotalDueAmount)
        val tvDueSubtitle = view.findViewById<TextView>(R.id.tvDueSubtitle)
        val tvViewAll = view.findViewById<TextView>(R.id.tvViewAll)
        val tvEmptyState = view.findViewById<TextView>(R.id.tvEmptyState)
        val imgUser = view.findViewById<ImageView>(R.id.imgUser)
        val rvRecent = view.findViewById<RecyclerView>(R.id.rvVendors)
        val btnPayNow = view.findViewById<CardView>(R.id.btnPayNow)

        val adapter = CustomerRecentTransactionAdapter()
        rvRecent.layoutManager = LinearLayoutManager(requireContext())
        rvRecent.adapter = adapter

        btnPayNow.setOnClickListener {
            Toast.makeText(context, "Payment Feature will be live Soon...", Toast.LENGTH_SHORT).show()
        }

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
            tvWelcome.text = "Welcome, ${state.name}"
            if (state.phone.isNotBlank()) tvPhone.text = state.phone

            if (state.profileImageBase64.isNotBlank()) {
                decodeBase64ToBitmap(state.profileImageBase64)?.let { imgUser.setImageBitmap(it) }
                    ?: imgUser.setImageResource(R.drawable.user)
            } else if (state.profilePath.isNotBlank()) {
                imgUser.setImageURI(android.net.Uri.fromFile(java.io.File(state.profilePath)))
            } else {
                imgUser.setImageResource(R.drawable.user)
            }

            tvTotalDue.text = "Rs ${state.totalDue}"
            tvDueSubtitle.text = "You owe ${state.vendorCount} vendors"
            adapter.submitList(state.recentTransactions)

            tvEmptyState.text = "No recent transactions"
            tvEmptyState.visibility = if (state.recentTransactions.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.ensureLoaded()

        tvViewAll.setOnClickListener {
            findNavController().navigate(R.id.action_customerDashboardFragment_to_allVendorTransactionsFragment)
        }

        CustomerNavHelper.setup(this, view)
        CustomerNavHelper.highlight(this, view, R.id.tabHome)
    }

    private fun decodeBase64ToBitmap(base64: String) = runCatching {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}