package com.akeel.aitbaar.ui.vendor.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.data.model.Transaction
import com.akeel.aitbaar.data.repository.TransactionRepository
import kotlinx.coroutines.launch

class AllTransactionsFragment : Fragment() {


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
        return inflater.inflate(R.layout.fragment_all_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


/*
        fun getDummyTransactions(): List<Transaction> {
            return listOf(
                Transaction("Akeel", "Milk + Bread", 5000, "06 Feb 2026", Status.ACCEPTED),
                Transaction("Rafiq", "Rice", 80, "05 Feb 2026", Status.PENDING),
                Transaction("Imran", "Eggs", 60, "04 Feb 2026", Status.REJECTED),
                Transaction("Akeel", "Milk + Bread", 120, "06 Feb 2026", Status.ACCEPTED),
                Transaction("Rafiq", "Rice", 80, "05 Feb 2026", Status.PENDING),
                Transaction("Imran", "Eggs", 60, "04 Feb 2026", Status.REJECTED),
                Transaction("Akeel", "Milk + Bread", 120, "06 Feb 2026", Status.ACCEPTED),
                Transaction("Rafiq", "Rice", 80, "05 Feb 2026", Status.PENDING),
                Transaction("Imran", "Eggs", 60, "04 Feb 2026", Status.REJECTED)
            )
        }
*/


        val recycler = view.findViewById<RecyclerView>(R.id.rvAllTransactions)

        recycler.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            TransactionRepository.getAllTransactions().collect { transactions ->

                // show FULL list here (no take(3))
                recycler.adapter = TransactionAdapter(transactions)
            }
        }

    }

}