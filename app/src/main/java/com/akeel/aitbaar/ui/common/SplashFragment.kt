package com.akeel.aitbaar.ui.common

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.akeel.aitbaar.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // ✅ small delay for splash feel (optional)
        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginAndNavigate()
        }, 1000)
    }

    private fun checkLoginAndNavigate() {
        val user = auth.currentUser

        // ✅ Not logged in → Choose role screen
        if (user == null) {
            findNavController().navigate(R.id.action_splashFragment_to_chooseApp)
            return
        }

        val uid = user.uid

        // ✅ First check Vendor
        db.collection("vendors").document(uid).get()
            .addOnSuccessListener { vendorDoc ->
                if (vendorDoc.exists()) {
                    // ✅ go Vendor Dashboard
                    findNavController().navigate(
                        R.id.vendorDashboardFragment,
                        null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.splashFragment, true)
                            .build()
                    )
                } else {
                    // ✅ Check Customer
                    db.collection("customers").document(uid).get()
                        .addOnSuccessListener { customerDoc ->
                            if (customerDoc.exists()) {
                                // ✅ go Customer Dashboard
                                findNavController().navigate(
                                    R.id.customerDashboardFragment,
                                    null,
                                    androidx.navigation.NavOptions.Builder()
                                        .setPopUpTo(R.id.splashFragment, true)
                                        .build()
                                )
                            } else {
                                // ✅ Fallback → Choose App
                                findNavController().navigate(R.id.action_splashFragment_to_chooseApp)
                            }
                        }
                        .addOnFailureListener {
                            // ✅ If error → Choose App
                            findNavController().navigate(R.id.action_splashFragment_to_chooseApp)
                        }
                }
            }
            .addOnFailureListener {
                // ✅ If error → Choose App
                findNavController().navigate(R.id.action_splashFragment_to_chooseApp)
            }
    }
}
