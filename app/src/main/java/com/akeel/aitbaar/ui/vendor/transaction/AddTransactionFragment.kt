package com.akeel.aitbaar.ui.vendor.transaction

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
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

    private var currentCustomerUid: String? = null
    private var currentCustomerPhone: String? = null
    private var currentVendorName: String = "Vendor"

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
        val btnBack =  view.findViewById<ImageView>(R.id.btnBack)

        transactionId = arguments?.getString("transactionId")
        if (!transactionId.isNullOrEmpty()) {
            isEditMode = true
            loadTransactionData(tvCustomer, etItem, etAmount, tvDate, tvTitle, btnText)
            loadCloudMetaForEdit(transactionId!!)
        }

        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        btnSave.setOnClickListener {
            val customerName = selectedCustomerName ?: tvCustomer.text.toString().trim()
            val customerUid =
                if (isEditMode) (currentCustomerUid ?: selectedCustomerUid) else selectedCustomerUid
            val customerPhone = if (isEditMode) (currentCustomerPhone
                ?: selectedCustomerPhone) else selectedCustomerPhone
            val item = etItem.text.toString().trim()
            val amountText = etAmount.text.toString().trim()
            val date = tvDate.text.toString()

            if (customerName.isBlank() || customerName.equals(
                    "Select Customer",
                    ignoreCase = true
                )
            ) {
                tvCustomer.error = "Select customer"
                return@setOnClickListener
            }

            // ADD mode requires explicit customer selection
            if (!isEditMode && customerUid.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Select an Aitbaar customer", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (!isEditMode && customerPhone.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Customer phone not found", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (item.isBlank()) {
                etItem.error = "Enter item"
                return@setOnClickListener
            }

            val amount = amountText.toIntOrNull()
            if (amount == null || amount <= 0) {
                etAmount.error = "Enter valid amount"
                return@setOnClickListener
            }

            val transaction = Transaction(
                id = currentTransaction?.id ?: System.currentTimeMillis().toInt(),
                customerName = customerName,
                item = item,
                amount = amount,
                date = date,
                // edit/correct always sends back for customer re-verification
                status = if (isEditMode) Status.PENDING else (currentTransaction?.status
                    ?: Status.PENDING)
            )

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    if (isEditMode) {
                        TransactionRepository.updateTransaction(transaction)
                        updateTransactionInFirestore(
                            transaction = transaction,
                            customerUid = customerUid.orEmpty(),
                            customerPhone = customerPhone.orEmpty()
                        )
                    } else {
                        TransactionRepository.addTransaction(transaction)
                        savePendingTransactionToFirestore(
                            transaction = transaction,
                            customerUid = customerUid.orEmpty(),
                            customerPhone = customerPhone.orEmpty()
                        )
                    }

                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }

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
            selectedCustomerUid = bundle.getString("customer_uid")
            selectedCustomerPhone = bundle.getString("customer_phone")

            tvCustomer.text = selectedCustomerName
            tvCustomer.error = null
        }

        view.findViewById<View>(R.id.BtnSelectCustomer).setOnClickListener {
            if (isEditMode) {
                Toast.makeText(
                    requireContext(),
                    "Customer cannot be changed in edit mode",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            findNavController().navigate(R.id.action_addTransactionFragment_to_selectCustomerFragment)
        }
    }
    private fun loadCloudMetaForEdit(txId: String) {
        db.collection("transactions")
            .document(txId)
            .get()
            .addOnSuccessListener { doc ->
                currentCustomerUid = doc.getString("customerId")
                currentCustomerPhone = doc.getString("customerPhone")
                currentVendorName = doc.getString("vendorName").orEmpty().ifBlank { "Vendor" }
            }
    }

    private suspend fun savePendingTransactionToFirestore(
        transaction: Transaction,
        customerUid: String,
        customerPhone: String
    ) {
        val vendorUid = auth.currentUser?.uid
            ?: throw IllegalStateException("Vendor not logged in")

        val vendorName = fetchVendorName(vendorUid)

        val payload = hashMapOf<String, Any?>(
            "transactionId" to transaction.id.toString(),
            "vendorId" to vendorUid,
            "vendorName" to vendorName,
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

    private suspend fun updateTransactionInFirestore(
        transaction: Transaction,
        customerUid: String,
        customerPhone: String
    ) {
        val vendorUid = auth.currentUser?.uid
            ?: throw IllegalStateException("Vendor not logged in")

        val vendorName =
            if (currentVendorName.isNotBlank()) currentVendorName else fetchVendorName(vendorUid)

        val updates = hashMapOf<String, Any?>(
            "vendorId" to vendorUid,
            "vendorName" to vendorName,
            "customerId" to customerUid,
            "customerName" to transaction.customerName,
            "customerPhone" to customerPhone,
            "item" to transaction.item,
            "amount" to transaction.amount,
            "status" to Status.PENDING.name,
            "updatedAt" to FieldValue.serverTimestamp(),
            "approvedAt" to null,
            "rejectedAt" to null,
            "rejectionReason" to null,
            "date" to transaction.date
        )

        suspendCancellableCoroutine<Unit> { continuation ->
            db.collection("transactions")
                .document(transaction.id.toString())
                .update(updates)
                .addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(Unit)
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
        }
    }

    private suspend fun fetchVendorName(vendorUid: String): String {
        return suspendCancellableCoroutine { continuation ->
            db.collection("vendors")
                .document(vendorUid)
                .get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("shopName")
                        ?.takeIf { it.isNotBlank() }
                        ?: "Vendor"
                    if (continuation.isActive) continuation.resume(name)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume("Vendor")
                }
        }
    }

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
                val transaction = list.find { it.id.toString() == transactionId }
                transaction?.let {
                    currentTransaction = it
                    tvCustomer.text = it.customerName
                    selectedCustomerName = it.customerName
                    etItem.setText(it.item)
                    etAmount.setText(it.amount.toString())
                    tvDate.text = it.date

                    tvTitle.text = if (it.status == Status.REJECTED) {
                        "Correct Transaction"
                    } else {
                        "Edit Transaction"
                    }
                    btnText.text = "Update"
                }
            }
        }
    }
}