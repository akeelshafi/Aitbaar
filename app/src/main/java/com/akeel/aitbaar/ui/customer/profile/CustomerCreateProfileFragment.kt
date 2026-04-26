package com.akeel.aitbaar.ui.customer.profile

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
import com.google.firebase.firestore.SetOptions
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

class CustomerCreateProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var savedImagePath: String? = null
    private var selectedBitmap: Bitmap? = null
    private var existingBase64: String = ""
    private var existingLocalPath: String = ""
    private var isEditMode: Boolean = false
    private lateinit var imgProfile: ImageView

    // ✅ Gallery picker
    private val pickImageFromGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                val bitmap =
                    MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
                imgProfile.setImageBitmap(bitmap)
                selectedBitmap = bitmap
                savedImagePath = saveBitmapToInternalStorage(bitmap)
            }
        }

    // ✅ Camera capture
    private val takePhotoFromCamera =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            if (bitmap != null) {
                imgProfile.setImageBitmap(bitmap)
                selectedBitmap = bitmap
                savedImagePath = saveBitmapToInternalStorage(bitmap)
            }
        }

    // ✅ Request camera permission
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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_customer_create_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        imgProfile = view.findViewById(R.id.imgProfile)
        val btnCamera = view.findViewById<CardView>(R.id.btnCamera)

        val etFullName = view.findViewById<EditText>(R.id.etFullName)
        val etPhone = view.findViewById<EditText>(R.id.etPhone)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etAddress = view.findViewById<EditText>(R.id.etAddress)
        val btnCreateProfile = view.findViewById<CardView>(R.id.btnCreateProfile)
        val tvButtonText = view.findViewById<android.widget.TextView>(R.id.tvButtonText)

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "User not logged in ❌", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ Auto phone
        etPhone.setText(user.phoneNumber ?: "")

        loadExistingProfile(
            uid = user.uid,
            etFullName = etFullName,
            etPhone = etPhone,
            etEmail = etEmail,
            etAddress = etAddress,
            tvButtonText = tvButtonText
        )

        // ✅ Choose Image
        btnCamera.setOnClickListener { showImagePickerDialog() }

        // ✅ Save Profile
        btnCreateProfile.setOnClickListener {

            val name = etFullName.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val email = etEmail.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Enter full name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (address.isEmpty()) {
                Toast.makeText(requireContext(), "Enter address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = user.uid
            val phoneNumber = user.phoneNumber ?: ""
            val localImagePath = savedImagePath ?: existingLocalPath
            val profileImageBase64 = selectedBitmap?.let { bitmapToBase64(it) } ?: existingBase64
            saveCustomerProfile(
                uid = uid,
                phoneNumber = phoneNumber,
                name = name,
                address = address,
                email = email,
                localImagePath = localImagePath,
                profileImageBase64 = profileImageBase64
            )
        }
    }

    // ✅ Camera / Gallery Dialog
    private fun showImagePickerDialog() {
        val options = arrayOf("Camera", "Gallery")

        AlertDialog.Builder(requireContext())
            .setTitle("Choose Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> requestCameraPermission.launch(android.Manifest.permission.CAMERA)
                    1 -> pickImageFromGallery.launch("image/*")
                }
            }
            .show()
    }

    // ✅ Save image locally
    private fun saveBitmapToInternalStorage(bitmap: Bitmap): String {
        val user = auth.currentUser ?: return ""

        val fileName = "customer_profile_${user.uid}.jpg"
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

    private fun saveCustomerProfile(
        uid: String,
        phoneNumber: String,
        name: String,
        address: String,
        email: String,
        localImagePath: String,
        profileImageBase64: String
    ) {
        val customerMap = hashMapOf(
            "uid" to uid,
            "phoneNumber" to phoneNumber,
            "role" to "customer",
            "name" to name,
            "address" to address,
            "email" to email,
            "profileImagePath" to localImagePath,
            "profileImageBase64" to profileImageBase64,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!isEditMode) {
            customerMap["createdAt"] = FieldValue.serverTimestamp()
        }

        db.collection("customers")
            .document(uid)
            .set(customerMap, SetOptions.merge())
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                Toast.makeText(
                    requireContext(),
                    if (isEditMode) "Profile Updated ✅" else "Customer Profile Saved ✅",
                    Toast.LENGTH_SHORT
                ).show()

                findNavController().navigate(
                    R.id.customerDashboardFragment,
                    null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .build()
                )
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
        } else {
            bitmap
        }

        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 45, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun loadExistingProfile(
        uid: String,
        etFullName: EditText,
        etPhone: EditText,
        etEmail: EditText,
        etAddress: EditText,
        tvButtonText: android.widget.TextView
    ) {
        db.collection("customers")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                isEditMode = true
                tvButtonText.text = "Update Profile"

                etFullName.setText(doc.getString("name").orEmpty())
                etPhone.setText(doc.getString("phoneNumber").orEmpty())
                etEmail.setText(doc.getString("email").orEmpty())
                etAddress.setText(doc.getString("address").orEmpty())

                existingLocalPath = doc.getString("profileImagePath").orEmpty()
                existingBase64 = doc.getString("profileImageBase64").orEmpty()

                when {
                    existingBase64.isNotBlank() -> {
                        decodeBase64ToBitmap(existingBase64)?.let {
                            imgProfile.setImageBitmap(it)
                        }
                    }
                    existingLocalPath.isNotBlank() -> {
                        val file = File(existingLocalPath)
                        if (file.exists()) {
                            imgProfile.setImageURI(Uri.fromFile(file))
                        }
                    }
                }
            }
    }

    private fun decodeBase64ToBitmap(base64: String) = runCatching {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}