package com.akeel.aitbaar.ui.vendor.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.akeel.aitbaar.R

class InviteCustomerFragment : Fragment(R.layout.fragment_invite_customer) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etPhoneNumber = view.findViewById<EditText>(R.id.etPhoneNumber)
        val btnSendInvite = view.findViewById<View>(R.id.btnSendInvite)

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }

        btnSendInvite.setOnClickListener {
            val raw = etPhoneNumber.text.toString().trim()
            val digits = raw.filter { it.isDigit() }

            if (digits.length != 10) {
                etPhoneNumber.error = "Enter valid 10-digit mobile number"
                return@setOnClickListener
            }

            val fullPhone = "+91$digits"
            val inviteLink = "https://aitbaar.app/invite"
            val message = "Hi! Join me on Aitbaar for trusted digital ledger. Download here: $inviteLink"

            // SMS specific (prefill receiver)
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$fullPhone")
                putExtra("sms_body", message)
            }

            // Generic share chooser (WhatsApp, Telegram, etc.)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }

            try {
                // Let user decide app: SMS / WhatsApp / others
                val chooser = Intent.createChooser(shareIntent, "Send invite via")
                startActivity(chooser)
            } catch (e: Exception) {
                // fallback to sms app
                if (smsIntent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(smsIntent)
                } else {
                    Toast.makeText(requireContext(), "No app found to send invite", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}