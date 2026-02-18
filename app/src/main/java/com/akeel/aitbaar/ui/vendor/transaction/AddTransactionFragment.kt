package com.akeel.aitbaar.ui.vendor.transaction

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.data.model.Transaction
import com.akeel.aitbaar.data.repository.TransactionRepository
import kotlinx.coroutines.launch

class AddTransactionFragment : Fragment() {

    private var selectedCustomerName: String? = null


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
        return inflater.inflate(R.layout.fragment_add_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvDate = view.findViewById<TextView>(R.id.tvDate)
        val tvCustomer = view.findViewById<TextView>(R.id.tvSelectCustomer)
        val etItem = view.findViewById<EditText>(R.id.etItem)
        val etAmount = view.findViewById<EditText>(R.id.etAmount)
        val btnSave = view.findViewById<View>(R.id.btnSend)

        btnSave.setOnClickListener {

            val customerName = tvCustomer.text.toString()
            val item = etItem.text.toString()
            val amountText = etAmount.text.toString()
            val date = tvDate.text.toString()

            if (selectedCustomerName.isNullOrEmpty()) {
                tvCustomer.error = "Select customer"
                return@setOnClickListener
            }

            if (item.isBlank()) {
                etItem.error = "Enter item"
                return@setOnClickListener
            }

            if (amountText.isBlank()) {
                etAmount.error = "Enter amount"
                return@setOnClickListener
            }

            val amount = amountText.toInt()

            val transaction = Transaction(
                customerName = selectedCustomerName!!,
                item = item,
                amount = amount,
                date = date,
                status = Status.PENDING
            )

            // 🔥 FIX — call suspend inside coroutine
            viewLifecycleOwner.lifecycleScope.launch {
                TransactionRepository.addTransaction(transaction)

                // go back after saving
                findNavController().popBackStack()
            }
        }


        // Set today's date when screen opens
        val calendar = java.util.Calendar.getInstance()
        val formatter = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        tvDate.text = formatter.format(calendar.time)


        tvDate.setOnClickListener {

            val cal = java.util.Calendar.getInstance()

            val datePicker = android.app.DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->

                    val selectedCal = java.util.Calendar.getInstance()
                    selectedCal.set(year, month, dayOfMonth)

                    tvDate.text = formatter.format(selectedCal.time)
                },
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            )

            datePicker.show()
        }



        parentFragmentManager.setFragmentResultListener(
            "customer_request",
            viewLifecycleOwner
        ) { _, bundle ->

            selectedCustomerName = bundle.getString("customer_name")
            tvCustomer.text = selectedCustomerName
            tvCustomer.error = null

        }

        view.findViewById<View>(R.id.BtnSelectCustomer).setOnClickListener {
            findNavController().navigate(R.id.action_addTransactionFragment_to_selectCustomerFragment)


        }

    }

}