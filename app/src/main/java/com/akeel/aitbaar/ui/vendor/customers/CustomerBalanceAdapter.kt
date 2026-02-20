package com.akeel.aitbaar.ui.vendor.customers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.CustomerBalance

class CustomerBalanceAdapter(
    private var list: List<CustomerBalance>, private val onClick: (CustomerBalance) -> Unit
) : RecyclerView.Adapter<CustomerBalanceAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvCustomerName)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.tvName.text = item.name
        holder.tvAmount.text = "₹${item.balance}"

        if (item.balance == 0) {
            holder.tvStatus.text = "CLEAR"
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_clear)
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.black))
        } else {
            holder.tvStatus.text = "DUE"
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_due)
            holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.white))
        }

        holder.itemView.setOnClickListener {
            onClick(item)
        }


    }

    override fun getItemCount(): Int = list.size

    fun submitList(newList: List<CustomerBalance>) {
        list = newList
        notifyDataSetChanged()
    }
}
