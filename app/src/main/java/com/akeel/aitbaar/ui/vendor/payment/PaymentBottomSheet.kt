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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

        // 🔹 Set today’s date automatically
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val todayDate = dateFormat.format(Date())
        tvDate.text = todayDate

        // 🔹 Close button
        btnClose.setOnClickListener {
            dismiss()
        }

        btnConfirm.setOnClickListener {

            val amountText = etAmount.text.toString().trim()

            val amount = amountText.toIntOrNull()

            if (amount == null || amount <= 0) {
                etAmount.error = "Enter valid amount"
                return@setOnClickListener
            }

            if (amount > currentBalance) {
                etAmount.error = "Amount exceeds balance"
                return@setOnClickListener
            }

            // Save payment
            viewLifecycleOwner.lifecycleScope.launch {
                TransactionRepository.addPayment(
                    customerName = customerName,
                    amount = amount,
                    date = tvDate.text.toString()
                )
                syncPaymentToCloud(amount) {
                    onPaymentAdded.invoke()
                    dismiss()
                }
            }
        }
    }

    private fun syncPaymentToCloud(amount: Int, onDone: () -> Unit) {
        val vendorId = FirebaseAuth.getInstance().currentUser?.uid
        if (vendorId.isNullOrBlank()) {
            onDone()
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("transactions")
            .whereEqualTo("vendorId", vendorId)
            .whereEqualTo("customerName", customerName)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val first = snapshot.documents.firstOrNull()
                val customerId = first?.getString("customerId").orEmpty()
                val vendorName = first?.getString("vendorName").orEmpty()

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
                    .addOnSuccessListener {
                        onDone()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Payment saved locally", Toast.LENGTH_SHORT).show()
                        onDone()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Payment saved locally", Toast.LENGTH_SHORT).show()
                onDone()
            }
    }
}