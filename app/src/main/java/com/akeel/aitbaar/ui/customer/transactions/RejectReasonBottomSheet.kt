package com.akeel.aitbaar.ui.customer.transactions

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.akeel.aitbaar.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RejectReasonBottomSheet(
    private val onReasonSelected: (String) -> Unit
) : BottomSheetDialogFragment(R.layout.bottom_sheet_reject_reason) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val option1 = view.findViewById<TextView>(R.id.tvReasonAmount)
        val option2 = view.findViewById<TextView>(R.id.tvReasonItem)
        val option3 = view.findViewById<TextView>(R.id.tvReasonNotMine)
        val option4 = view.findViewById<TextView>(R.id.tvReasonOther)

        option1.setOnClickListener {
            onReasonSelected("Wrong amount")
            dismiss()
        }
        option2.setOnClickListener {
            onReasonSelected("Wrong item/description")
            dismiss()
        }
        option3.setOnClickListener {
            onReasonSelected("This transaction is not mine")
            dismiss()
        }
        option4.setOnClickListener {
            onReasonSelected("Other")
            dismiss()
        }
    }
}