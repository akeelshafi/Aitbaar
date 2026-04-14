package com.akeel.aitbaar.ui.vendor.transaction

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Customer

class CustomerAdapter(
    private var list: List<Customer>,
    private val onSelectCustomer: (Customer) -> Unit,
    private val onInviteCustomer: (Customer) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_customer, parent, false)
        return CustomerViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val customer = list[position]
        val displayName = customer.aitbaarName ?: customer.name

        holder.name.text = displayName
        holder.phone.text = customer.phone
        holder.initials.text = getInitials(displayName)

        if (customer.isOnAitbaar) {
            holder.status.text = "On Aitbaar"
            holder.status.setBackgroundResource(R.drawable.bg_status_accepted)
            holder.inviteButton.visibility = View.GONE
            holder.itemView.isClickable = true
            holder.itemView.setOnClickListener { onSelectCustomer(customer) }
        } else {
            holder.status.text = "Not on Aitbaar"
            holder.status.setBackgroundResource(R.drawable.bg_status_due)
            holder.inviteButton.visibility = View.VISIBLE
            holder.itemView.setOnClickListener(null)
            holder.itemView.isClickable = false
            holder.inviteButton.setOnClickListener { onInviteCustomer(customer) }
        }
    }

    override fun getItemCount(): Int = list.size

    class CustomerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvCustomerName)
        val phone: TextView = itemView.findViewById(R.id.tvCustomerPhone)
        val status: TextView = itemView.findViewById(R.id.tvAitbaarStatus)
        val inviteButton: Button = itemView.findViewById(R.id.btnInvite)
        val initials: TextView = itemView.findViewById(R.id.tvInitials)
    }

    fun submitList(newList: List<Customer>) {
        list = newList
        notifyDataSetChanged()
    }

    private fun getInitials(name: String): String {
        return name.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2) // first / middle / last support
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifBlank { "?" }
    }
}
