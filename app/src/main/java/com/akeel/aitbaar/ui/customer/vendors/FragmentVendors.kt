package com.akeel.aitbaar.ui.customer.vendors

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.CustomerBalance
import com.akeel.aitbaar.ui.customer.CustomerDataViewModel
import com.akeel.aitbaar.ui.customer.CustomerNavHelper
import com.akeel.aitbaar.ui.vendor.customers.CustomerBalanceAdapter
import java.util.Locale

class FragmentVendors : Fragment(R.layout.fragment_vendors) {

    private val viewModel: CustomerDataViewModel by activityViewModels()
    private lateinit var adapter: CustomerBalanceAdapter
    private var allVendors: List<CustomerBalance> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvVendors = view.findViewById<RecyclerView>(R.id.rvCustomers)
        val etSearchVendor = view.findViewById<EditText>(R.id.etSearchCustomer)

        adapter = CustomerBalanceAdapter(emptyList()) { vendor ->
            val action =
                FragmentVendorsDirections
                    .actionFragmentVendorsToCustomerVendorLedgerFragment(vendor.name)
            findNavController().navigate(action)
        }
        rvVendors.layoutManager = LinearLayoutManager(requireContext())
        rvVendors.adapter = adapter

        etSearchVendor.doAfterTextChanged { text ->
            filterVendors(text?.toString().orEmpty())
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            allVendors = if (state.vendorBalances.isNotEmpty()) {
                state.vendorBalances
            } else {
                state.allTransactions
                    .map { it.customerName }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .map { vendorName -> CustomerBalance(vendorName, 0) }
            }
            filterVendors(etSearchVendor.text?.toString().orEmpty())
        }

        viewModel.ensureLoaded()

        CustomerNavHelper.setup(this, view)
        CustomerNavHelper.highlight(this, view, R.id.tabCustomers)
    }

    private fun filterVendors(query: String) {
        val normalizedQuery = query.trim().lowercase(Locale.getDefault())
        val filteredList = if (normalizedQuery.isBlank()) {
            allVendors
        } else {
            allVendors.filter { vendor ->
                vendor.name.lowercase(Locale.getDefault()).contains(normalizedQuery)
            }
        }
        adapter.submitList(filteredList)
    }
}