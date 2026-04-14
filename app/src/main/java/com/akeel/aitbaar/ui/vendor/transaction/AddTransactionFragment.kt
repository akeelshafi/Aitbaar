package com.akeel.aitbaar.ui.vendor.transaction

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.data.model.Transaction
import com.akeel.aitbaar.data.repository.TransactionRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddTransactionFragment : Fragment(R.layout.fragment_add_transaction) {

    private var selectedCustomerName: String? = null
    private var isEditMode = false
    private var transactionId: String? = null
    private var currentTransaction: Transaction? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvDate = view.findViewById<TextView>(R.id.tvDate)
        val tvCustomer = view.findViewById<TextView>(R.id.tvSelectCustomer)
        val etItem = view.findViewById<EditText>(R.id.etItem)
        val etAmount = view.findViewById<EditText>(R.id.etAmount)
        val btnSave = view.findViewById<View>(R.id.btnSend)

        val tvTitle = view.findViewById<TextView>(R.id.tvTitleTransaction)
        val btnText = view.findViewById<TextView>(R.id.tvBtnTextTransaction)

        // 🔥 STEP 1: Get transactionId
        transactionId = arguments?.getString("transactionId")

        if (!transactionId.isNullOrEmpty()) {
            isEditMode = true
            loadTransactionData(tvCustomer, etItem, etAmount, tvDate, tvTitle, btnText)
        }

        // 🔥 SAVE / UPDATE
        btnSave.setOnClickListener {

            val customerName = selectedCustomerName ?: tvCustomer.text.toString().trim()
            val item = etItem.text.toString()
            val amountText = etAmount.text.toString()
            val date = tvDate.text.toString()

            if (customerName.isBlank() || customerName.equals("Select Customer", ignoreCase = true)) {
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
                id = currentTransaction?.id ?: System.currentTimeMillis().toInt(),
                customerName = customerName,
                item = item,
                amount = amount,
                date = date,
                status = currentTransaction?.status ?: Status.PENDING
            )

            viewLifecycleOwner.lifecycleScope.launch {

                if (isEditMode) {
                    TransactionRepository.updateTransaction(transaction)
                } else {
                    TransactionRepository.addTransaction(transaction)
                }

                findNavController().popBackStack()
            }
        }

        // 🔹 Default Date
        val calendar = java.util.Calendar.getInstance()
        val formatter = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        tvDate.text = formatter.format(calendar.time)

        // 🔹 Date Picker
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

        // 🔹 Customer Selection
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

    // 🔥 AUTO-FILL FUNCTION
    private fun loadTransactionData(
        tvCustomer: TextView,
        etItem: EditText,
        etAmount: EditText,
        tvDate: TextView,
        tvTitle: TextView,
        btnText: TextView
    ) {

        viewLifecycleOwner.lifecycleScope.launch {

            TransactionRepository.getAllTransactions().collectLatest { list ->

                val transaction = list.find {
                    it.id.toString() == transactionId
                }

                transaction?.let {

                    currentTransaction = it

                    tvCustomer.text = it.customerName
                    selectedCustomerName = it.customerName

                    etItem.setText(it.item)
                    etAmount.setText(it.amount.toString())
                    tvDate.text = it.date

                    // 🔥 CHANGE UI
                    if (it.status == Status.REJECTED) {
                        tvTitle.text = "Correct Transaction"
                    } else {
                        tvTitle.text = "Edit Transaction"
                    }

                    btnText.text = "Update"
                }
            }
        }
    }
}
