package com.akeel.aitbaar.ui.customer.profile

import android.app.AlertDialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import java.io.File
import java.io.FileOutputStream

class CustomerCreateProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var savedImagePath: String? = null
    private lateinit var imgProfile: ImageView

    // ✅ Gallery picker
    private val pickImageFromGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                val bitmap =
                    MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
                imgProfile.setImageBitmap(bitmap)
                savedImagePath = saveBitmapToInternalStorage(bitmap)
            }
        }

    // ✅ Camera capture
    private val takePhotoFromCamera =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            if (bitmap != null) {
                imgProfile.setImageBitmap(bitmap)
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

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "User not logged in ❌", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ Auto phone
        etPhone.setText(user.phoneNumber ?: "")

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

            val customerMap = hashMapOf(
                "uid" to uid,
                "phoneNumber" to phoneNumber,
                "role" to "customer",
                "name" to name,
                "address" to address,
                "email" to email, // optional
                "profileImagePath" to (savedImagePath ?: ""),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.collection("customers")
                .document(uid)
                .set(customerMap)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Customer Profile Saved ✅", Toast.LENGTH_SHORT)
                        .show()

                    // ✅ Go Dashboard + remove auth/profile screens from backstack
                    findNavController().navigate(
                        R.id.customerDashboardFragment,
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
}
