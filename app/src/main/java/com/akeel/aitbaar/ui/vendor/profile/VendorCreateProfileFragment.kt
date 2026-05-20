package com.akeel.aitbaar.ui.vendor.profile

import android.Manifest
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.akeel.aitbaar.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class VendorCreateProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var imgVendorProfile: ImageView
    private var savedImagePath: String? = null
    private var selectedBitmap: Bitmap? = null
    private var existingBase64: String = ""
    private var isEditMode = false

    private val pickImageFromGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, it)
                imgVendorProfile.setImageBitmap(bitmap)
                selectedBitmap = bitmap
                savedImagePath = saveBitmapToInternalStorage(bitmap)
            }
        }

    private val takePhotoFromCamera =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let {
                imgVendorProfile.setImageBitmap(it)
                selectedBitmap = it
                savedImagePath = saveBitmapToInternalStorage(it)
            }
        }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) takePhotoFromCamera.launch(null)
            else Toast.makeText(requireContext(), "Camera permission denied ❌", Toast.LENGTH_SHORT).show()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_vendor_create_profile, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        imgVendorProfile = view.findViewById(R.id.imgProfile)

        val etOwnerName = view.findViewById<EditText>(R.id.etOwnerName)
        val etVendorPhone = view.findViewById<EditText>(R.id.etVendorPhone)
        val etBusinessEmail = view.findViewById<EditText>(R.id.etBusinessEmail)
        val etBusinessName = view.findViewById<EditText>(R.id.etBusinessName)
        val etBusinessAddress = view.findViewById<EditText>(R.id.etBusinessAddress)
        val etGstNumber = view.findViewById<EditText>(R.id.etGstNumber)

        val actBusinessCategory = view.findViewById<AutoCompleteTextView>(R.id.actBusinessCategory)
        val actBusinessType = view.findViewById<AutoCompleteTextView>(R.id.actBusinessType)

        val btnCreateVendorProfile = view.findViewById<CardView>(R.id.btnCreateVendorProfile)
        val btnText = view.findViewById<TextView>(R.id.tvBtnText)
        val title = view.findViewById<TextView>(R.id.tvVendorCreateProfileTitle)

        val btnVendorCamera = view.findViewById<CardView>(R.id.btnVendorCamera)

        val categories = listOf(
            "Kirana", "Medical", "Apparel", "Electronics", "Mobile",
            "Financial Services", "Insurance", "Digital", "Agriculture",
            "Education", "Computer", "Tour & Travel", "Other"
        )
        val types = listOf("Retailer / Shop", "Wholesaler", "Distributor", "Services", "Manufacturer", "Other")

        actBusinessCategory.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories)
        )
        actBusinessType.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, types)
        )

        actBusinessCategory.setOnClickListener { actBusinessCategory.showDropDown() }
        actBusinessType.setOnClickListener { actBusinessType.showDropDown() }

        val user = auth.currentUser ?: return view
        val uid = user.uid

        etVendorPhone.setText(user.phoneNumber ?: "")

        db.collection("vendors").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    isEditMode = true

                    etOwnerName.setText(doc.getString("name"))
                    etBusinessName.setText(doc.getString("shopName"))
                    etBusinessAddress.setText(doc.getString("shopAddress"))
                    etBusinessEmail.setText(doc.getString("email"))
                    etGstNumber.setText(doc.getString("gstNumber"))

                    actBusinessCategory.setText(doc.getString("businessCategory"), false)
                    actBusinessType.setText(doc.getString("businessType"), false)

                    val imagePath = doc.getString("profileImagePath").orEmpty()
                    existingBase64 = doc.getString("profileImageBase64").orEmpty()
                    savedImagePath = imagePath

                    when {
                        existingBase64.isNotBlank() -> {
                            decodeBase64ToBitmap(existingBase64)?.let {
                                imgVendorProfile.setImageBitmap(it)
                            }
                        }
                        imagePath.isNotBlank() -> {
                            val file = File(imagePath)
                            if (file.exists()) imgVendorProfile.setImageURI(Uri.fromFile(file))
                        }
                    }

                    title.text = "Update Profile"
                    btnText.text = "Update Profile"
                }
            }

        btnVendorCamera.setOnClickListener { showImagePickerDialog() }

        btnCreateVendorProfile.setOnClickListener {
            val ownerName = etOwnerName.text.toString().trim()
            val businessEmail = etBusinessEmail.text.toString().trim()
            val businessName = etBusinessName.text.toString().trim()
            val businessAddress = etBusinessAddress.text.toString().trim()
            val gstNumber = etGstNumber.text.toString().trim()
            val businessCategory = actBusinessCategory.text.toString().trim()
            val businessType = actBusinessType.text.toString().trim()

            var hasError = false

            if (ownerName.isEmpty()) {
                etOwnerName.error = "Owner name is required"
                hasError = true
            }
            if (businessName.isEmpty()) {
                etBusinessName.error = "Business name is required"
                hasError = true
            }
            if (businessAddress.isEmpty()) {
                etBusinessAddress.error = "Business address is required"
                hasError = true
            }
            if (businessCategory.isEmpty()) {
                actBusinessCategory.error = "Business category is required"
                hasError = true
            } else actBusinessCategory.error = null

            if (businessType.isEmpty()) {
                actBusinessType.error = "Business type is required"
                hasError = true
            } else actBusinessType.error = null

            if (businessEmail.isNotEmpty() &&
                !android.util.Patterns.EMAIL_ADDRESS.matcher(businessEmail).matches()
            ) {
                etBusinessEmail.error = "Enter a valid email"
                hasError = true
            }

            if (hasError) {
                Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val profileImageBase64 = selectedBitmap?.let { bitmapToBase64(it) } ?: existingBase64

            val vendorMap = hashMapOf<String, Any>(
                "uid" to uid,
                "phoneNumber" to (user.phoneNumber ?: ""),
                "role" to "vendor",

                "name" to ownerName,
                "shopName" to businessName,
                "shopAddress" to businessAddress,

                "email" to businessEmail,
                "gstNumber" to gstNumber,
                "businessCategory" to businessCategory,
                "businessType" to businessType,

                "profileImagePath" to (savedImagePath ?: ""),
                "profileImageBase64" to profileImageBase64,

                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (!isEditMode) vendorMap["createdAt"] = FieldValue.serverTimestamp()

            db.collection("vendors")
                .document(uid)
                .set(vendorMap)
                .addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        if (isEditMode) "Profile Updated ✅" else "Profile Created ✅",
                        Toast.LENGTH_SHORT
                    ).show()

                    if (isEditMode) {
                        findNavController().previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("profile_updated", true)
                        findNavController().popBackStack()
                    } else {
                        findNavController().navigate(
                            R.id.vendorDashboardFragment,
                            null,
                            NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                        )
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
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
        val file = File(requireContext().filesDir, "vendor_profile_${user.uid}.jpg")
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
        return file.absolutePath
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxSide = 512
        val scaled = if (bitmap.width > maxSide || bitmap.height > maxSide) {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val (newW, newH) = if (ratio > 1f) {
                maxSide to (maxSide / ratio).toInt().coerceAtLeast(1)
            } else {
                (maxSide * ratio).toInt().coerceAtLeast(1) to maxSide
            }
            Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        } else bitmap

        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 45, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun decodeBase64ToBitmap(base64: String) = runCatching {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}