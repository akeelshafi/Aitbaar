package com.akeel.aitbaar.ui.common.auth

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.akeel.aitbaar.R
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class OtpVerifyFragment : Fragment() {

    private val args: OtpVerifyFragmentArgs by navArgs()

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var countDownTimer: CountDownTimer? = null
    private var canResend = false
    private var isVerifying = false

    private lateinit var btnVerify: CardView
    private lateinit var tvVerifyBtnText: TextView
    private lateinit var tvResend: TextView

    // ✅ updated when OTP is sent
    private var currentVerificationId: String = "PENDING"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_customer_otp_verify, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        currentVerificationId = args.verificationId

        val otp1 = view.findViewById<EditText>(R.id.otp1)
        val otp2 = view.findViewById<EditText>(R.id.otp2)
        val otp3 = view.findViewById<EditText>(R.id.otp3)
        val otp4 = view.findViewById<EditText>(R.id.otp4)
        val otp5 = view.findViewById<EditText>(R.id.otp5)
        val otp6 = view.findViewById<EditText>(R.id.otp6)

        val tvOtpPhone = view.findViewById<TextView>(R.id.tvOtpPhone)
        tvResend = view.findViewById(R.id.tvResend)

        btnVerify = view.findViewById(R.id.btnVerifyOtp)
        tvVerifyBtnText = view.findViewById(R.id.tvVerifyBtnText)

        // ✅ Show phone number
        tvOtpPhone.text = args.phoneNumber

        // ✅ initially resend disabled (until OTP is sent)
        setResendDisabled()

        // ✅ send OTP from this fragment
        sendOtp(args.phoneNumber)

        fun getOtpCode(): String {
            return otp1.text.toString().trim() +
                    otp2.text.toString().trim() +
                    otp3.text.toString().trim() +
                    otp4.text.toString().trim() +
                    otp5.text.toString().trim() +
                    otp6.text.toString().trim()
        }

        // ✅ OTP UX
        setupOtpInputs(otp1, otp2, otp3, otp4, otp5, otp6) { code ->
            verifyOtpAndLogin(code)
        }

        // ✅ Verify button
        btnVerify.setOnClickListener {
            val code = getOtpCode()
            if (code.length != 6) {
                Toast.makeText(requireContext(), "Enter 6-digit OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            verifyOtpAndLogin(code)
        }

        // ✅ Resend click
        tvResend.setOnClickListener {
            if (!canResend) return@setOnClickListener

            // ✅ Disable immediately & restart timer only when code is actually sent
            setResendDisabled()

            val token = OtpSession.resendToken
            if (token == null) {
                // ✅ fallback
                Toast.makeText(requireContext(), "Sending OTP again...", Toast.LENGTH_SHORT).show()
                sendOtp(args.phoneNumber)
            } else {
                resendOtp(args.phoneNumber, token)
            }
        }
    }

    // ✅ Button UI (Verifying inside button)
    private fun showVerifyingOnButton() {
        btnVerify.isEnabled = false
        btnVerify.alpha = 0.7f
        tvVerifyBtnText.text = "Verifying..."
    }

    private fun resetVerifyButton() {
        btnVerify.isEnabled = true
        btnVerify.alpha = 1f
        tvVerifyBtnText.text = "Verify"
    }

    // ✅ Resend UI helpers
    private fun setResendDisabled() {
        canResend = false
        tvResend.isEnabled = false
        tvResend.setTextColor(resources.getColor(R.color.gray, null))
        tvResend.text = "Sending OTP..."
    }

    private fun startResendTimer() {
        canResend = false
        tvResend.isEnabled = false
        tvResend.setTextColor(resources.getColor(R.color.gray, null))

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(60000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                val mm = seconds / 60
                val ss = seconds % 60
                tvResend.text = String.format("%02d:%02d  Resend Code", mm, ss)
            }

            override fun onFinish() {
                canResend = true
                tvResend.isEnabled = true
                tvResend.text = "Resend Code"
                tvResend.setTextColor(resources.getColor(R.color.aitbaar_blue, null))
            }
        }.start()
    }

    // ✅ send OTP (first time or fallback)
    private fun sendOtp(phoneNumber: String) {

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // optional
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(requireContext(), "OTP Failed: ${e.message}", Toast.LENGTH_LONG)
                        .show()

                    // allow resend again
                    countDownTimer?.cancel()
                    tvResend.text = "Resend Code"
                    tvResend.isEnabled = true
                    canResend = true
                    tvResend.setTextColor(resources.getColor(R.color.aitbaar_blue, null))
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    currentVerificationId = verificationId
                    OtpSession.resendToken = token

                    Toast.makeText(requireContext(), "OTP Sent ✅", Toast.LENGTH_SHORT).show()

                    // ✅ timer starts ONLY here (NO restart bug)
                    startResendTimer()
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // ✅ resend OTP with token
    private fun resendOtp(
        phoneNumber: String,
        token: PhoneAuthProvider.ForceResendingToken
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setForceResendingToken(token)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {}

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(requireContext(), "Resend failed: ${e.message}", Toast.LENGTH_LONG)
                        .show()

                    // allow resend again
                    tvResend.text = "Resend Code"
                    tvResend.isEnabled = true
                    canResend = true
                    tvResend.setTextColor(resources.getColor(R.color.aitbaar_blue, null))
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    currentVerificationId = verificationId
                    OtpSession.resendToken = token

                    Toast.makeText(requireContext(), "OTP Resent ✅", Toast.LENGTH_SHORT).show()

                    // ✅ timer starts ONLY here
                    startResendTimer()
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // ✅ Verify OTP -> Login -> Check Profile -> Navigate
    private fun verifyOtpAndLogin(code: String) {
        if (isVerifying) return
        if (code.length != 6) return

        if (currentVerificationId == "PENDING") {
            Toast.makeText(requireContext(), "OTP is being sent... wait ✅", Toast.LENGTH_SHORT).show()
            return
        }

        isVerifying = true
        showVerifyingOnButton()

        val credential = PhoneAuthProvider.getCredential(currentVerificationId, code)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->

                if (!task.isSuccessful) {
                    isVerifying = false
                    resetVerifyButton()
                    Toast.makeText(
                        requireContext(),
                        "OTP Failed ❌ ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnCompleteListener
                }

                val user = auth.currentUser
                if (user == null) {
                    isVerifying = false
                    resetVerifyButton()
                    Toast.makeText(requireContext(), "User not found ❌", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }

                val uid = user.uid
                val role = args.userRole.lowercase()

                if (role == "vendor") {
                    db.collection("vendors").document(uid).get()
                        .addOnSuccessListener { doc ->
                            // ✅ Don’t reset verify button here (avoid flicker)
                            if (doc.exists()) {
                                findNavController().navigate(
                                    R.id.vendorDashboardFragment,
                                    null,
                                    androidx.navigation.NavOptions.Builder()
                                        .setPopUpTo(R.id.nav_graph, true)
                                        .build()
                                )
                            } else {
                                findNavController().navigate(
                                    R.id.action_customerOtpVerifyFragment2_to_vendorCreateProfileFragment
                                )
                            }
                        }
                        .addOnFailureListener { e ->
                            isVerifying = false
                            resetVerifyButton()
                            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG)
                                .show()
                        }
                } else {
                    db.collection("customers").document(uid).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                findNavController().navigate(
                                    R.id.customerDashboardFragment,
                                    null,
                                    androidx.navigation.NavOptions.Builder()
                                        .setPopUpTo(R.id.nav_graph, true)
                                        .build()
                                )
                            } else {
                                findNavController().navigate(
                                    R.id.action_customerOtpVerifyFragment2_to_customerCreateProfileFragment
                                )
                            }
                        }
                        .addOnFailureListener { e ->
                            isVerifying = false
                            resetVerifyButton()
                            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG)
                                .show()
                        }
                }
            }
    }

    // ✅ OTP Boxes UX (auto move + back + paste)
    private fun setupOtpInputs(
        otp1: EditText,
        otp2: EditText,
        otp3: EditText,
        otp4: EditText,
        otp5: EditText,
        otp6: EditText,
        onOtpComplete: (String) -> Unit
    ) {
        val fields = listOf(otp1, otp2, otp3, otp4, otp5, otp6)

        fun getCode(): String {
            return otp1.text.toString().trim() +
                    otp2.text.toString().trim() +
                    otp3.text.toString().trim() +
                    otp4.text.toString().trim() +
                    otp5.text.toString().trim() +
                    otp6.text.toString().trim()
        }

        // ✅ paste in otp1
        otp1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: return
                if (text.length > 1) {
                    val clean = text.filter { it.isDigit() }
                    if (clean.length >= 6) {
                        otp1.setText(clean[0].toString())
                        otp2.setText(clean[1].toString())
                        otp3.setText(clean[2].toString())
                        otp4.setText(clean[3].toString())
                        otp5.setText(clean[4].toString())
                        otp6.setText(clean[5].toString())
                        otp6.requestFocus()
                        val code = getCode()
                        if (code.length == 6) onOtpComplete(code)
                    }
                }
            }
        })

        fun nextMove(current: EditText, next: EditText?) {
            current.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (current.text.toString().length == 1) next?.requestFocus()
                    val code = getCode()
                    if (code.length == 6) onOtpComplete(code)
                }
            })
        }

        nextMove(otp1, otp2)
        nextMove(otp2, otp3)
        nextMove(otp3, otp4)
        nextMove(otp4, otp5)
        nextMove(otp5, otp6)
        nextMove(otp6, null)

        // ✅ backspace move previous
        for (i in 1 until fields.size) {
            val current = fields[i]
            val prev = fields[i - 1]
            current.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    current.text.isEmpty()
                ) {
                    prev.requestFocus()
                }
                false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        countDownTimer = null
    }
}
