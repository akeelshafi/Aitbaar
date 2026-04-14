package com.akeel.aitbaar.ui.vendor.customers

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.CustomerBalance
import com.akeel.aitbaar.data.repository.TransactionRepository
import com.akeel.aitbaar.ui.vendor.VendorNavHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class CustomersFragment : Fragment() {

    private lateinit var adapter: CustomerBalanceAdapter
    private var allCustomers: List<CustomerBalance> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_customers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.rvCustomers)
        val etSearchCustomer = view.findViewById<EditText>(R.id.etSearchCustomer)

        adapter = CustomerBalanceAdapter(emptyList()) { customer ->

            val action =
                CustomersFragmentDirections
                    .actionCustomersFragmentToCustomerLedgerFragment(customer.name)

            findNavController().navigate(action)
        }
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // 🔥 Collect real data from Room
        viewLifecycleOwner.lifecycleScope.launch {
            TransactionRepository.getCustomerBalances().collectLatest { list ->
                allCustomers = list
                filterCustomers(etSearchCustomer.text.toString())
            }
        }

        etSearchCustomer.doAfterTextChanged { text ->
            filterCustomers(text?.toString().orEmpty())
        }

        VendorNavHelper.setup(this, view)
        VendorNavHelper.highlight(this, view, R.id.iconCustomer)

        view.findViewById<ImageView>(R.id.iconCustomer)
            .setColorFilter(requireContext().getColor(R.color.blue))




    }

    private fun filterCustomers(query: String) {
        val normalizedQuery = query.trim().lowercase(Locale.getDefault())
        val filteredList = if (normalizedQuery.isEmpty()) {
            allCustomers
        } else {
            allCustomers.filter { customer ->
                customer.name.lowercase(Locale.getDefault()).contains(normalizedQuery)
            }
        }
        adapter.submitList(filteredList)
    }

}
