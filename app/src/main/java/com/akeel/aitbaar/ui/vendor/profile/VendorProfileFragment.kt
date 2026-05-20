package com.akeel.aitbaar.ui.vendor.profile

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.net.Uri
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
import com.akeel.aitbaar.ui.vendor.VendorDataViewModel
import com.akeel.aitbaar.ui.vendor.VendorNavHelper
import com.google.firebase.auth.FirebaseAuth
import java.io.File

class VendorProfileFragment : Fragment(R.layout.fragment_vendor_profile) {

    private val viewModel: VendorDataViewModel by activityViewModels()
    private lateinit var auth: FirebaseAuth

    private lateinit var tvVendorName: TextView
    private lateinit var tvShopName: TextView
    private lateinit var imgProfile: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        tvVendorName = view.findViewById(R.id.tvVendorName)
        tvShopName = view.findViewById(R.id.tvShopName)
        imgProfile = view.findViewById(R.id.imgProfile)

        VendorNavHelper.setup(this, view)
        VendorNavHelper.highlight(this, view, R.id.iconProfile)

        view.findViewById<ImageView>(R.id.iconProfile)
            .setColorFilter(requireContext().getColor(R.color.blue))

        view.findViewById<View>(R.id.helpSupportCard).setOnClickListener {
            Toast.makeText(requireContext(), "Help & Support coming soon", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.inviteCustomerCard).setOnClickListener {
            findNavController().navigate(R.id.action_vendorProfileFragment_to_inviteCustomerFragment)
        }

        view.findViewById<Button>(R.id.btnEditProfile).setOnClickListener {
            findNavController().navigate(
                R.id.vendorCreateProfileFragment,
                null,
                NavOptions.Builder().setPopUpTo(R.id.vendorProfileFragment, false).build()
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
                        NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                    )
                }
                .show()
        }

        // Observe shared vendor state
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            tvVendorName.text = state.vendorName
            tvShopName.text = state.shopName

            when {
                state.profileImageBase64.isNotBlank() -> {
                    decodeBase64ToBitmap(state.profileImageBase64)?.let { bmp ->
                        imgProfile.setImageBitmap(bmp)
                    } ?: imgProfile.setImageResource(R.drawable.user)
                }
                state.profileImagePath.isNotBlank() -> {
                    val file = File(state.profileImagePath)
                    if (file.exists()) imgProfile.setImageURI(Uri.fromFile(file))
                    else imgProfile.setImageResource(R.drawable.user)
                }
                else -> imgProfile.setImageResource(R.drawable.user)
            }
        }

        // Initial load once for vendor module
        viewModel.ensureLoaded()

        // Refresh ONLY when edit profile returns success flag
        val backStackEntry = findNavController().currentBackStackEntry
        backStackEntry?.savedStateHandle
            ?.getLiveData<Boolean>("profile_updated")
            ?.observe(viewLifecycleOwner) { updated ->
                if (updated == true) {
                    viewModel.ensureLoaded(forceRefresh = true)
                    backStackEntry.savedStateHandle["profile_updated"] = false
                }
            }
    }

    private fun decodeBase64ToBitmap(base64: String) = runCatching {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}