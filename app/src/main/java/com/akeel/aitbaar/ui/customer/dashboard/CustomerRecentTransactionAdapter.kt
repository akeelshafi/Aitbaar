package com.akeel.aitbaar.ui.customer.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.data.model.Transaction

class CustomerRecentTransactionAdapter : RecyclerView.Adapter<CustomerRecentTransactionAdapter.ViewHolder>() {

    private var list: List<Transaction> = emptyList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvCustomerName)
        val tvItem: TextView = view.findViewById(R.id.tvItem)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val btnPrimaryAction: TextView = view.findViewById(R.id.btnPrimaryAction)
        val btnSecondaryAction: TextView = view.findViewById(R.id.btnSecondaryAction)    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tx = list[position]

        holder.tvName.text = tx.customerName
        holder.tvItem.text = tx.item
        holder.tvAmount.text = "₹${tx.amount}"
        holder.tvDate.text = tx.date
        holder.btnPrimaryAction.visibility = View.GONE
        holder.btnSecondaryAction.visibility = View.GONE
        when (tx.status) {
            Status.ACCEPTED -> {
                holder.tvStatus.text = "ACCEPTED"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_accepted)
            }
            Status.PENDING -> {
                holder.tvStatus.text = "PENDING"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending)
            }
            Status.REJECTED -> {
                holder.tvStatus.text = "REJECTED"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_rejected)
            }
            Status.PAID -> {
                holder.tvStatus.text = "PAID"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_paid)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun submitList(newList: List<Transaction>) {
        list = newList
        notifyDataSetChanged()
    }
}
