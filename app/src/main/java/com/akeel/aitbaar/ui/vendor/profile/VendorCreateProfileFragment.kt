package com.akeel.aitbaar.ui.vendor.profile

import android.Manifest
import android.app.AlertDialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.akeel.aitbaar.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream

class VendorCreateProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var imgVendorProfile: ImageView
    private var savedImagePath: String? = null

    // ✅ Gallery picker
    private val pickImageFromGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                val bitmap =
                    MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
                imgVendorProfile.setImageBitmap(bitmap)
                savedImagePath = saveBitmapToInternalStorage(bitmap)
            }
        }

    // ✅ Camera capture
    private val takePhotoFromCamera =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            if (bitmap != null) {
                imgVendorProfile.setImageBitmap(bitmap)
                savedImagePath = saveBitmapToInternalStorage(bitmap)
            }
        }

    // ✅ Camera permission
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                takePhotoFromCamera.launch(null)
            } else {
                Toast.makeText(requireContext(), "Camera permission denied ❌", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_vendor_create_profile, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // ✅ Image + Camera
        imgVendorProfile = view.findViewById(R.id.imgProfile)
        val btnVendorCamera = view.findViewById<CardView>(R.id.btnVendorCamera)

        // ✅ Inputs
        val etOwnerName = view.findViewById<EditText>(R.id.etOwnerName)
        val etVendorPhone = view.findViewById<EditText>(R.id.etVendorPhone)
        val etBusinessEmail = view.findViewById<EditText>(R.id.etBusinessEmail)
        val etBusinessName = view.findViewById<EditText>(R.id.etBusinessName)
        val etBusinessAddress = view.findViewById<EditText>(R.id.etBusinessAddress)
        val etGstNumber = view.findViewById<EditText>(R.id.etGstNumber)

        val actBusinessCategory = view.findViewById<AutoCompleteTextView>(R.id.actBusinessCategory)
        val actBusinessType = view.findViewById<AutoCompleteTextView>(R.id.actBusinessType)

        val btnCreateVendorProfile = view.findViewById<CardView>(R.id.btnCreateVendorProfile)

        // ✅ Dropdown lists
        val categories = listOf(
            "Kirana", "Medical", "Apparel", "Electronics", "Mobile",
            "Financial Services", "Insurance", "Digital", "Agriculture",
            "Education", "Computer", "Tour & Travel", "Other"
        )

        val types = listOf(
            "Retailer / Shop", "Wholesaler", "Distributor", "Services",
            "Manufacturer", "Other"
        )

        // ✅ Set adapters
        actBusinessCategory.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories)
        )
        actBusinessType.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, types)
        )

        // ✅ Show dropdown on click
        actBusinessCategory.setOnClickListener { actBusinessCategory.showDropDown() }
        actBusinessType.setOnClickListener { actBusinessType.showDropDown() }

        // ✅ Current user
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "User not logged in ❌", Toast.LENGTH_SHORT).show()
            return view
        }

        etVendorPhone.setText(user.phoneNumber ?: "")

        // ✅ Camera click
        btnVendorCamera.setOnClickListener { showImagePickerDialog() }

        // ✅ Save Vendor Profile
        btnCreateVendorProfile.setOnClickListener {

            val ownerName = etOwnerName.text.toString().trim()
            val businessName = etBusinessName.text.toString().trim()
            val businessAddress = etBusinessAddress.text.toString().trim()

            val businessEmail = etBusinessEmail.text.toString().trim()
            val gstNumber = etGstNumber.text.toString().trim()
            val category = actBusinessCategory.text.toString().trim()
            val type = actBusinessType.text.toString().trim()

            if (ownerName.isEmpty()) {
                Toast.makeText(requireContext(), "Enter Owner Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (businessName.isEmpty()) {
                Toast.makeText(requireContext(), "Enter Business Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (businessAddress.isEmpty()) {
                Toast.makeText(requireContext(), "Enter Business Address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = user.uid
            val phoneNumber = user.phoneNumber ?: ""

            val vendorMap = hashMapOf(
                "uid" to uid,
                "phoneNumber" to phoneNumber,
                "role" to "vendor",

                "name" to ownerName,
                "shopName" to businessName,
                "shopAddress" to businessAddress,

                "email" to businessEmail,
                "gstNumber" to gstNumber,
                "businessCategory" to category,
                "businessType" to type,

                "profileImagePath" to (savedImagePath ?: ""),

                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.collection("vendors")
                .document(uid)
                .set(vendorMap)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Vendor Profile Saved ✅", Toast.LENGTH_SHORT)
                        .show()

                    // ✅ Go Dashboard + remove auth/profile screens from backstack
                    findNavController().navigate(
                        R.id.vendorDashboardFragment,
                        null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true)
                            .build()
                    )
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
        }

        return view
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Camera", "Gallery")

        AlertDialog.Builder(requireContext())
            .setTitle("Choose Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> requestCameraPermission.launch(Manifest.permission.CAMERA)
                    1 -> pickImageFromGallery.launch("image/*")
                }
            }
            .show()
    }

    private fun saveBitmapToInternalStorage(bitmap: Bitmap): String {
        val user = auth.currentUser ?: return ""

        val fileName = "vendor_profile_${user.uid}.jpg"
        val file = File(requireContext().filesDir, fileName)

        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Image Save Failed: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }

        return file.absolutePath
    }
}
