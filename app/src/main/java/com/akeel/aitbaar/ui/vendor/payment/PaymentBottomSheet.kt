package com.akeel.aitbaar.ui.vendor.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.repository.TransactionRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentBottomSheet(
    private val customerName: String,
    private val currentBalance: Int,
    private val onPaymentAdded: () -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_payment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val btnClose = view.findViewById<ImageView>(R.id.btnClose)
        val etAmount = view.findViewById<EditText>(R.id.etAmount)
        val tvDate = view.findViewById<TextView>(R.id.tvDate)
        val btnConfirm = view.findViewById<TextView>(R.id.btnConfirm)

        etAmount.setText(currentBalance.toString())

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        tvDate.text = dateFormat.format(Date())

        btnClose.setOnClickListener { dismiss() }

        btnConfirm.setOnClickListener {
            val amount = etAmount.text.toString().trim().toIntOrNull()

            if (amount == null || amount <= 0) {
                etAmount.error = "Enter valid amount"
                return@setOnClickListener
            }

            if (amount > currentBalance) {
                etAmount.error = "Amount exceeds balance"
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                // Local save
                TransactionRepository.addPayment(
                    customerName = customerName,
                    amount = amount,
                    date = tvDate.text.toString()
                )

                // Cloud save
                syncPaymentToCloud(amount) { success ->
                    if (!success) {
                        Toast.makeText(
                            requireContext(),
                            "Payment saved locally (cloud sync failed)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    onPaymentAdded.invoke()
                    dismiss()
                }
            }
        }
    }

    private fun syncPaymentToCloud(amount: Int, onDone: (Boolean) -> Unit) {
        val vendorId = FirebaseAuth.getInstance().currentUser?.uid
        if (vendorId.isNullOrBlank()) {
            onDone(false)
            return
        }

        val db = FirebaseFirestore.getInstance()

        db.collection("transactions")
            .whereEqualTo("vendorId", vendorId)
            .whereEqualTo("customerName", customerName)
            .get()
            .addOnSuccessListener { snapshot ->
                // find latest transaction in app code (no orderBy => no composite index need)
                val latestTx = snapshot.documents.maxByOrNull {
                    it.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                }

                val customerId = latestTx?.getString("customerId").orEmpty()
                val vendorName = latestTx?.getString("vendorName").orEmpty()

                if (customerId.isBlank()) {
                    Toast.makeText(
                        requireContext(),
                        "Cloud sync failed: customerId missing in transaction",
                        Toast.LENGTH_LONG
                    ).show()
                    onDone(false)
                    return@addOnSuccessListener
                }

                val paymentDoc = hashMapOf(
                    "vendorId" to vendorId,
                    "vendorName" to vendorName,
                    "customerId" to customerId,
                    "customerName" to customerName,
                    "amount" to amount,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                db.collection("payments")
                    .add(paymentDoc)
                    .addOnSuccessListener { onDone(true) }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Payment cloud write failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        onDone(false)
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Transaction lookup failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                onDone(false)
            }
    }
}