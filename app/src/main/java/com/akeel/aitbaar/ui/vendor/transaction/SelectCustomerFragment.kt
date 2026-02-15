package com.akeel.aitbaar.ui.vendor.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Customer

class SelectCustomerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_select_customer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.rvCustomers)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = CustomerAdapter(this, getDummyCustomers())

        // Back button click
        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }
    }

    // Dummy customers list (temporary until Firebase)
    private fun getDummyCustomers(): List<Customer> {
        return listOf(
            Customer("Akeel", "+91 9876543210"),
            Customer("Rafiq", "+91 9123456780"),
            Customer("Imran", "+91 9988776655"),
            Customer("Yasir", "+91 9012345678"),
            Customer("Sajid", "+91 9090909090")
        )
    }
}