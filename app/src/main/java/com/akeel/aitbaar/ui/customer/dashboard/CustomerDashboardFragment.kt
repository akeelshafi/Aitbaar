package com.akeel.aitbaar.ui.customer.dashboard

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.ui.vendor.dashboard.VendorAdapter
import com.akeel.aitbaar.ui.vendor.dashboard.VendorModel

class CustomerDashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_customer_dashboard, container, false)

        val rvVendors = view.findViewById<RecyclerView>(R.id.rvVendors)

        // ✅ Dummy Vendor List (Testing)
        val vendorList = listOf(
            VendorModel(
                "AT",
                "Ahmad Traders",
                "Rs 3,200 Due | Today",
                "Rs 3,200",
                Color.parseColor("#FFD7A3")
            ),
            VendorModel("FS", "Fatima Store", "Rs 4,300 Overdue | 2 days ago", "Rs 4,300", Color.parseColor("#B7D6C0")),
            VendorModel("SE", "Suleman Electronics", "Rs 1,000 Due Tomorrow", "Rs 1,000", Color.parseColor("#A8C6FF")),
            VendorModel("SE", "Suleman Electronics", "Rs 1,000 Due Tomorrow", "Rs 1,000", Color.parseColor("#A8C6FF")),
            VendorModel("SE", "Suleman Electronics", "Rs 1,000 Due Tomorrow", "Rs 1,000", Color.parseColor("#A8C6FF")),
            VendorModel("SE", "Suleman Electronics", "Rs 1,000 Due Tomorrow", "Rs 1,000", Color.parseColor("#A8C6FF")),
            VendorModel("SE", "Suleman Electronics", "Rs 1,000 Due Tomorrow", "Rs 1,000", Color.parseColor("#A8C6FF")),
            VendorModel("SE", "Suleman Electronics", "Rs 1,000 Due Tomorrow", "Rs 1,000", Color.parseColor("#A8C6FF")),
            VendorModel("SE", "Suleman Electronics", "Rs 1,000 Due Tomorrow", "Rs 1,000", Color.parseColor("#A8C6FF")),
            VendorModel("SE", "Suleman Electronics", "Rs 1,000 Due Tomorrow", "Rs 1,000", Color.parseColor("#A8C6FF")),
            VendorModel("SE", "Suleman Electronics", "Rs 1,000 Due Tomorrow", "Rs 1,000", Color.parseColor("#A8C6FF")),
            VendorModel("SE", "Suleman Electronics", "Rs 1,000 Due Tomorrow", "Rs 1,000", Color.parseColor("#A8C6FF")),
            VendorModel("SE", "Suleman Electronics", "Rs 1,000 Due Tomorrow", "Rs 1,000", Color.parseColor("#A8C6FF")),
            VendorModel("SE", "Suleman Electronics", "Rs 1,000 Due Tomorrow", "Rs 1,000", Color.parseColor("#A8C6FF")),
            VendorModel("SE", "Suleman Electronics", "Rs 1,000 Due Tomorrow", "Rs 1,000", Color.parseColor("#A8C6FF")),
            VendorModel("SE", "Suleman Electronics", "Rs 1,000 Due Tomorrow", "Rs 1,000", Color.parseColor("#A8C6FF")),
            VendorModel("SE", "Suleman Electronics", "Rs 1,000 Due Tomorrow", "Rs 1,000", Color.parseColor("#A8C6FF")),
        )

        // ✅ LayoutManager + Adapter
        rvVendors.layoutManager = LinearLayoutManager(requireContext())
        rvVendors.adapter = VendorAdapter(vendorList)

        return view
    }
}
