package com.akeel.aitbaar.ui.common.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.akeel.aitbaar.R

class PhoneLoginFragment : Fragment() {

    private val args: PhoneLoginFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_customer_phone_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etPhone = view.findViewById<EditText>(R.id.etPhoneNumber)
        val btnSendOtp = view.findViewById<CardView>(R.id.btnSendOtp)

        btnSendOtp.setOnClickListener {

            val phone = etPhone.text.toString().trim()

            if (phone.length != 10) {
                Toast.makeText(requireContext(), "Enter valid 10 digit number", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val fullPhone = "+91$phone"

            // ✅ Just navigate fast to OTP screen
            val action = PhoneLoginFragmentDirections
                .actionCustomerPhoneLoginFragmentToCustomerOtpVerifyFragment2(
                    verificationId = "PENDING",
                    phoneNumber = fullPhone,
                    userRole = args.userRole
                )

            findNavController().navigate(action)
        }
    }
}
