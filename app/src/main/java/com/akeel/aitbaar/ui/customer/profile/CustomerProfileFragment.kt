package com.akeel.aitbaar.ui.customer.profile

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.akeel.aitbaar.R
import com.akeel.aitbaar.ui.customer.CustomerDataViewModel
import com.akeel.aitbaar.ui.customer.CustomerNavHelper
import com.google.firebase.auth.FirebaseAuth
import java.io.File

class CustomerProfileFragment : Fragment(R.layout.fragment_customer_profile) {

    private val viewModel: CustomerDataViewModel by activityViewModels()
    private lateinit var auth: FirebaseAuth
    private var loadTriggered = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val tvName = view.findViewById<TextView>(R.id.tvVendorName)
        val tvSubTitle = view.findViewById<TextView>(R.id.tvShopName)
        val imgProfile = view.findViewById<ImageView>(R.id.imgProfile)
        val btnEditProfile = view.findViewById<Button>(R.id.btnEditProfile)

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            tvName.text = state.name.ifBlank { "Customer" }
            tvSubTitle.text = state.phone.ifBlank { "Phone not available" }

            if (state.profileImageBase64.isNotBlank()) {
                decodeBase64ToBitmap(state.profileImageBase64)?.let { imgProfile.setImageBitmap(it) }
                    ?: imgProfile.setImageResource(R.drawable.user)
            } else if (state.profilePath.isNotBlank()) {
                val file = File(state.profilePath)
                if (file.exists()) {
                    imgProfile.setImageURI(android.net.Uri.fromFile(file))
                } else {
                    imgProfile.setImageResource(R.drawable.user)
                }
            } else {
                imgProfile.setImageResource(R.drawable.user)
            }
        }

        if (!loadTriggered) {
            loadTriggered = true
            viewModel.ensureLoaded()
        }

        CustomerNavHelper.setup(this, view)
        CustomerNavHelper.highlight(this, view, R.id.tabProfile)

        view.findViewById<View>(R.id.helpSupportCard).setOnClickListener {
            Toast.makeText(requireContext(), "Help & Support coming soon", Toast.LENGTH_SHORT).show()
        }

        btnEditProfile.setOnClickListener {
            findNavController().navigate(
                R.id.customerCreateProfileFragment,
                null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.customerProfileFragment, false)
                    .build()
            )
        }

        view.findViewById<View>(R.id.logoutCard).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setCancelable(false)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Logout") { _, _ ->
                    auth.signOut()
                    findNavController().navigate(
                        R.id.chooseApp,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true)
                            .build()
                    )
                }
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.ensureLoaded(forceRefresh = true)
    }

    private fun decodeBase64ToBitmap(base64: String) = runCatching {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}