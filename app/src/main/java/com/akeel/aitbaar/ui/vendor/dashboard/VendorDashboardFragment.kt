package com.akeel.aitbaar.ui.vendor.dashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.repository.TransactionRepository
import com.akeel.aitbaar.ui.vendor.VendorNavHelper
import com.akeel.aitbaar.ui.vendor.transaction.TransactionAdapter
import kotlinx.coroutines.launch

class VendorDashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_vendor_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        VendorNavHelper.setup(this, view)
        VendorNavHelper.highlight(this, view, R.id.iconHome)


        view.findViewById<ImageView>(R.id.iconHome)
            .setColorFilter(requireContext().getColor(R.color.blue))



        val recycler = view.findViewById<RecyclerView>(R.id.rvRecentTransactions)
        val addTransactionButton = view.findViewById<View>(R.id.btnAddTransaction)

        // Navigate to Add Transaction screen
        addTransactionButton.setOnClickListener {
            findNavController().navigate(R.id.action_vendorDashboardFragment_to_addTransactionFragment)
        }


        // Navigate to All Transactions screen
        view.findViewById<TextView>(R.id.tvViewAllTransaction).setOnClickListener {
            findNavController().navigate(R.id.action_vendorDashboardFragment_to_allTransactionsFragment)
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            TransactionRepository.getAllTransactions().collect { transactions ->

                // Show only latest 3 on dashboard
                val recentTransactions = transactions.take(4)

                recycler.adapter = TransactionAdapter(recentTransactions)
            }
        }

    }

    // -------- ALL DUMMY TRANSACTIONS --------
    /* private fun getDummyTransactions(): List<Transaction> {
         return listOf(
             Transaction("Akeel", "Milk + Bread", 5000, "06 Feb 2026", Status.ACCEPTED),
             Transaction("Rafiq", "Rice", 80, "05 Feb 2026", Status.PENDING),
             Transaction("Imran", "Eggs", 60, "04 Feb 2026", Status.REJECTED),
             Transaction("Akeel", "Milk + Bread", 120, "03 Feb 2026", Status.ACCEPTED),
             Transaction("Rafiq", "Rice", 80, "02 Feb 2026", Status.PENDING),
             Transaction("Imran", "Eggs", 60, "01 Feb 2026", Status.REJECTED),
             Transaction("Akeel", "Milk + Bread", 120, "31 Jan 2026", Status.ACCEPTED),
             Transaction("Rafiq", "Rice", 80, "30 Jan 2026", Status.PENDING),
             Transaction("Imran", "Eggs", 60, "29 Jan 2026", Status.REJECTED)
         )
     }

     // -------- ONLY RECENT 5 FOR DASHBOARD --------
     private fun getRecentTransactions(): List<Transaction> {
         val all = getDummyTransactions()
         return if (all.size > 5) {
             all.take(5)
         } else {
             all
         }
     }*/
}
