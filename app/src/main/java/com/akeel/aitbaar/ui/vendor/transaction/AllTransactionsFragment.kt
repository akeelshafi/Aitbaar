package com.akeel.aitbaar.ui.vendor.transaction

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.ui.vendor.VendorDataViewModel

class AllTransactionsFragment : Fragment(R.layout.fragment_all_transactions) {

    private val viewModel: VendorDataViewModel by activityViewModels()
    private lateinit var adapter: TransactionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.rvAllTransactions)
        val tvEmptyState = view.findViewById<TextView>(R.id.tvEmptyState)

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

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.allTransactions)
            tvEmptyState.text = "No transactions yet"
            tvEmptyState.visibility = if (state.allTransactions.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.ensureLoaded()
    }
}