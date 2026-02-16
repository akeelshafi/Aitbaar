package com.akeel.aitbaar.ui.vendor.transaction

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Customer

class CustomerAdapter(private val fragment: Fragment, private val list: List<Customer>) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_customer, parent, false)
        return CustomerViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val customer = list[position]
        holder.name.text = customer.name
        holder.phone.text = customer.phone

        // 🔥 Row click → send result back
        holder.itemView.setOnClickListener {

            fragment.parentFragmentManager.setFragmentResult(
                "customer_request", android.os.Bundle().apply {
                    putString("customer_name", customer.name)
                })

            // Go back to Add Transaction screen
            fragment.findNavController().popBackStack()
        }
    }

    override fun getItemCount(): Int = list.size


    class CustomerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvCustomerName)
        val phone: TextView = itemView.findViewById(R.id.tvCustomerPhone)
    }

}
