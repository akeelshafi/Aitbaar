package com.akeel.aitbaar.ui.vendor.customers

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.CustomerBalance
import com.akeel.aitbaar.ui.vendor.VendorDataViewModel
import com.akeel.aitbaar.ui.vendor.VendorNavHelper
import java.util.Locale

class CustomersFragment : Fragment(R.layout.fragment_customers) {

    private val viewModel: VendorDataViewModel by activityViewModels()
    private lateinit var adapter: CustomerBalanceAdapter
    private var allCustomers: List<CustomerBalance> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.rvCustomers)
        val etSearchCustomer = view.findViewById<EditText>(R.id.etSearchCustomer)
        val tvEmptyState = view.findViewById<TextView>(R.id.tvEmptyState)

        adapter = CustomerBalanceAdapter(emptyList()) { customer ->
            val action = CustomersFragmentDirections
                .actionCustomersFragmentToCustomerLedgerFragment(customer.name)
            findNavController().navigate(action)
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        etSearchCustomer.doAfterTextChanged { text ->
            filterCustomers(text?.toString().orEmpty(), tvEmptyState)
        }

        VendorNavHelper.setup(this, view)
        VendorNavHelper.highlight(this, view, R.id.iconCustomer)
        view.findViewById<ImageView>(R.id.iconCustomer)
            .setColorFilter(requireContext().getColor(R.color.blue))

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            allCustomers = state.customerBalances
            filterCustomers(etSearchCustomer.text.toString(), tvEmptyState)
        }

        viewModel.ensureLoaded()
    }

    private fun filterCustomers(query: String, tvEmptyState: TextView) {
        val normalizedQuery = query.trim().lowercase(Locale.getDefault())
        val filteredList = if (normalizedQuery.isEmpty()) {
            allCustomers
        } else {
            allCustomers.filter {
                it.name.lowercase(Locale.getDefault()).contains(normalizedQuery)
            }
        }

        adapter.submitList(filteredList)
        tvEmptyState.text = "No customers yet"
        tvEmptyState.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }
}