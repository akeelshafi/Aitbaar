package com.akeel.aitbaar.ui.vendor.profile

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.akeel.aitbaar.R
import com.akeel.aitbaar.ui.vendor.VendorNavHelper
import com.akeel.aitbaar.utils.ProfileCache
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class VendorProfileFragment : Fragment(R.layout.fragment_vendor_profile) {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvVendorName: TextView
    private lateinit var tvShopName: TextView
    private lateinit var imgProfile: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvVendorName = view.findViewById(R.id.tvVendorName)
        tvShopName = view.findViewById(R.id.tvShopName)
        imgProfile = view.findViewById(R.id.imgProfile)

        // 🔥 STEP 1: Show cached data instantly
        ProfileCache.name?.let {
            tvVendorName.text = it
            tvShopName.text = ProfileCache.shop

            // 🔥 Load cached image
            ProfileCache.imagePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    imgProfile.setImageURI(Uri.fromFile(file))
                }
            }
        }

        // 🔥 STEP 2: Fetch only if not cached
        if (ProfileCache.name == null) {
            loadProfileData()
        }

        // 🔹 Bottom Navigation
        VendorNavHelper.setup(this, view)
        VendorNavHelper.highlight(this, view, R.id.iconProfile)

        view.findViewById<ImageView>(R.id.iconProfile)
            .setColorFilter(requireContext().getColor(R.color.blue))

        // 🔹 Help
        view.findViewById<View>(R.id.helpSupportCard).setOnClickListener {
            Toast.makeText(requireContext(), "Help & Support coming soon", Toast.LENGTH_SHORT).show()
        }

        // 🔹 Edit Profile
        view.findViewById<Button>(R.id.btnEditProfile).setOnClickListener {
                findNavController().navigate(
                    R.id.vendorCreateProfileFragment,
                    null,
                    NavOptions.Builder()
                        .setPopUpTo(R.id.vendorProfileFragment, false)
                        .build()
                )
            }

        // 🔹 Logout
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

    // 🔥 Fetch from Firestore
    private fun loadProfileData() {

        val user = auth.currentUser ?: return
        val uid = user.uid

        db.collection("vendors")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    val name = document.getString("name") ?: "No Name"
                    val shop = document.getString("shopName") ?: "No Shop"
                    val imagePath = document.getString("profileImagePath")

                    tvVendorName.text = name
                    tvShopName.text = shop

                    // 🔥 Load image from local storage
                    if (!imagePath.isNullOrEmpty()) {
                        val file = File(imagePath)

                        if (file.exists()) {
                            imgProfile.setImageURI(Uri.fromFile(file))
                        } else {
                            imgProfile.setImageResource(R.drawable.user)
                        }
                    } else {
                        imgProfile.setImageResource(R.drawable.user)
                    }

                    // ✅ Save in cache
                    ProfileCache.name = name
                    ProfileCache.shop = shop
                    ProfileCache.imagePath = imagePath
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }
}