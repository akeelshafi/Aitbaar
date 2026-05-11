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

        ProfileCache.name?.let {
            tvVendorName.text = it
            tvShopName.text = ProfileCache.shop
            val base64 = ProfileCache.imageBase64.orEmpty()
            val path = ProfileCache.imagePath.orEmpty()

            when {
                base64.isNotBlank() -> decodeBase64ToBitmap(base64)?.let { bmp ->
                    imgProfile.setImageBitmap(bmp)
                }
                path.isNotBlank() -> {
                    val file = File(path)
                    if (file.exists()) imgProfile.setImageURI(Uri.fromFile(file))
                }
            }
        }

        if (ProfileCache.name == null) loadProfileData()

        VendorNavHelper.setup(this, view)
        VendorNavHelper.highlight(this, view, R.id.iconProfile)

        view.findViewById<ImageView>(R.id.iconProfile)
            .setColorFilter(requireContext().getColor(R.color.blue))

        view.findViewById<View>(R.id.helpSupportCard).setOnClickListener {
            Toast.makeText(requireContext(), "Help & Support coming soon", Toast.LENGTH_SHORT).show()
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
    }

    private fun loadProfileData() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("vendors")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) return@addOnSuccessListener

                val name = document.getString("name").orEmpty().ifBlank { "No Name" }
                val shop = document.getString("shopName").orEmpty().ifBlank { "No Shop" }
                val imagePath = document.getString("profileImagePath").orEmpty()
                val imageBase64 = document.getString("profileImageBase64").orEmpty()

                tvVendorName.text = name
                tvShopName.text = shop

                when {
                    imageBase64.isNotBlank() -> {
                        decodeBase64ToBitmap(imageBase64)?.let { imgProfile.setImageBitmap(it) }
                            ?: imgProfile.setImageResource(R.drawable.user)
                    }
                    imagePath.isNotBlank() -> {
                        val file = File(imagePath)
                        if (file.exists()) imgProfile.setImageURI(Uri.fromFile(file))
                        else imgProfile.setImageResource(R.drawable.user)
                    }
                    else -> imgProfile.setImageResource(R.drawable.user)
                }

                ProfileCache.name = name
                ProfileCache.shop = shop
                ProfileCache.imagePath = imagePath
                ProfileCache.imageBase64 = imageBase64
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun decodeBase64ToBitmap(base64: String) = runCatching {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}