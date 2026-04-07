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
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.akeel.aitbaar.R
import com.akeel.aitbaar.utils.DashboardCache
import com.akeel.aitbaar.utils.ProfileCache
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
    private var isEditMode = false

    // ✅ Gallery
    private val pickImageFromGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val bitmap =
                    MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, it)
                imgVendorProfile.setImageBitmap(bitmap)
                savedImagePath = saveBitmapToInternalStorage(bitmap)
            }
        }

    // ✅ Camera
    private val takePhotoFromCamera =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let {
                imgVendorProfile.setImageBitmap(it)
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

        // 🔹 Views
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

        // 🔹 Dropdowns
        val categories = listOf("Kirana","Medical","Apparel","Electronics","Mobile","Financial Services","Insurance","Digital","Agriculture","Education","Computer","Tour & Travel","Other")
        val types = listOf("Retailer / Shop","Wholesaler","Distributor","Services","Manufacturer","Other")

        actBusinessCategory.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categories))
        actBusinessType.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, types))

        actBusinessCategory.setOnClickListener { actBusinessCategory.showDropDown() }
        actBusinessType.setOnClickListener { actBusinessType.showDropDown() }

        val user = auth.currentUser ?: return view
        val uid = user.uid

        etVendorPhone.setText(user.phoneNumber ?: "")

        // 🔥 EDIT MODE DETECTION
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

                    val imagePath = doc.getString("profileImagePath")
                    savedImagePath = imagePath

                    if (!imagePath.isNullOrEmpty()) {
                        val file = File(imagePath)
                        if (file.exists()) {
                            imgVendorProfile.setImageURI(Uri.fromFile(file))
                        }
                    }

                    // 🔥 CHANGE UI
                    title.text = "Update Profile"
                    btnText.text = "Update Profile"
                }
            }

        // 🔹 Image picker
        btnVendorCamera.setOnClickListener { showImagePickerDialog() }

        // 🔥 SAVE / UPDATE
        btnCreateVendorProfile.setOnClickListener {

            val vendorMap = hashMapOf<String, Any>(
                "uid" to uid,
                "phoneNumber" to (user.phoneNumber ?: ""),
                "role" to "vendor",

                "name" to etOwnerName.text.toString(),
                "shopName" to etBusinessName.text.toString(),
                "shopAddress" to etBusinessAddress.text.toString(),

                "email" to etBusinessEmail.text.toString(),
                "gstNumber" to etGstNumber.text.toString(),
                "businessCategory" to actBusinessCategory.text.toString(),
                "businessType" to actBusinessType.text.toString(),

                "profileImagePath" to (savedImagePath ?: ""),

                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (!isEditMode) {
                vendorMap["createdAt"] = FieldValue.serverTimestamp()
            }

            db.collection("vendors")
                .document(uid)
                .set(vendorMap)
                .addOnSuccessListener {

                    Toast.makeText(
                        requireContext(),
                        if (isEditMode) "Profile Updated ✅" else "Profile Created ✅",
                        Toast.LENGTH_SHORT
                    ).show()

                    // 🔥 CLEAR CACHE
                    ProfileCache.name = null
                    ProfileCache.shop = null
                    ProfileCache.imagePath = null
                    DashboardCache.vendorName =null

                    findNavController().popBackStack()
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
}