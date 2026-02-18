package com.akeel.aitbaar.ui.vendor.transaction

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.data.model.Transaction

class TransactionAdapter(
    private var list: List<Transaction>
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

     class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name = view.findViewById<TextView>(R.id.tvCustomerName)
        val item = view.findViewById<TextView>(R.id.tvItem)
        val amount = view.findViewById<TextView>(R.id.tvAmount)
        val date = view.findViewById<TextView>(R.id.tvDate)
        val status = view.findViewById<TextView>(R.id.tvStatus)
        val button = view.findViewById<TextView>(R.id.btnAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = list[position]

        holder.name.text = transaction.customerName
        holder.item.text = transaction.item
        holder.amount.text = "₹${transaction.amount}"
        holder.date.text = transaction.date

        when (transaction.status) {

            Status.ACCEPTED -> {
                holder.status.text = "ACCEPTED"
                holder.status.setBackgroundResource(R.drawable.bg_status_accepted)
                holder.button.visibility = View.GONE
            }

            Status.PENDING -> {
                holder.status.text = "PENDING"
                holder.status.setBackgroundResource(R.drawable.bg_status_pending)
                holder.button.visibility = View.VISIBLE
                holder.button.text = "Edit"
            }

            Status.REJECTED -> {
                holder.status.text = "REJECTED"
                holder.status.setBackgroundResource(R.drawable.bg_status_rejected)
                holder.button.visibility = View.VISIBLE
                holder.button.text = "Correct"
            }

            Status.PAID -> {
                holder.status.text = "PAID"
                holder.status.setBackgroundResource(R.drawable.bg_status_paid)
                holder.button.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun submitList(newList: List<Transaction>) {
        list = newList
        notifyDataSetChanged()
    }
}