package com.akeel.aitbaar.ui.vendor.transaction

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Status
import com.akeel.aitbaar.data.model.Transaction
import com.akeel.aitbaar.data.repository.TransactionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AddTransactionFragment : Fragment(R.layout.fragment_add_transaction) {

    private var selectedCustomerName: String? = null
    private var selectedCustomerUid: String? = null
    private var selectedCustomerPhone: String? = null

    private var isEditMode = false
    private var transactionId: String? = null
    private var currentTransaction: Transaction? = null

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

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
            val customerUid = selectedCustomerUid
            val customerPhone = selectedCustomerPhone
            val item = etItem.text.toString()
            val amountText = etAmount.text.toString()
            val date = tvDate.text.toString()

            if (customerName.isBlank() || customerName.equals("Select Customer", ignoreCase = true)) {
                tvCustomer.error = "Select customer"
                return@setOnClickListener
            }

            if (customerUid.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Select an Aitbaar customer", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (customerPhone.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Customer phone not found", Toast.LENGTH_SHORT).show()
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
                try {
                    if (isEditMode) {
                        TransactionRepository.updateTransaction(transaction)
                    } else {
                        TransactionRepository.addTransaction(transaction)
                        savePendingTransactionToFirestore(
                            transaction = transaction,
                            customerUid = customerUid,
                            customerPhone = customerPhone
                        )
                    }

                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
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
            selectedCustomerUid = bundle.getString("customer_uid")
            selectedCustomerPhone = bundle.getString("customer_phone")

            tvCustomer.text = selectedCustomerName
            tvCustomer.error = null
        }

        view.findViewById<View>(R.id.BtnSelectCustomer).setOnClickListener {
            findNavController().navigate(R.id.action_addTransactionFragment_to_selectCustomerFragment)
        }
    }

    private suspend fun savePendingTransactionToFirestore(
        transaction: Transaction,
        customerUid: String,
        customerPhone: String
    ) {
        val vendorUid = auth.currentUser?.uid
            ?: throw IllegalStateException("Vendor not logged in")

        val payload = hashMapOf<String, Any?>(
            "transactionId" to transaction.id.toString(),
            "vendorId" to vendorUid,
            "vendorName" to "",
            "customerId" to customerUid,
            "customerName" to transaction.customerName,
            "customerPhone" to customerPhone,
            "item" to transaction.item,
            "amount" to transaction.amount,
            "currency" to "INR",
            "status" to Status.PENDING.name,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "approvedAt" to null,
            "rejectedAt" to null,
            "rejectionReason" to null,
            "paidAt" to null
        )

        suspendCancellableCoroutine<Unit> { continuation ->
            db.collection("transactions")
                .document(transaction.id.toString())
                .set(payload)
                .addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(Unit)
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
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
