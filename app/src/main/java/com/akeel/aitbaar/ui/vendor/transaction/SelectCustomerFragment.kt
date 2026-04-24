package com.akeel.aitbaar.ui.vendor.transaction

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Customer
import com.google.firebase.firestore.FirebaseFirestore

class SelectCustomerFragment : Fragment() {

    private data class RegisteredCustomer(
        val uid: String,
        val name: String,
        val phone: String
    )

    private lateinit var adapter: CustomerAdapter
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val contactPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                loadContactsAndMapAitbaarUsers()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Contacts permission denied. Unable to show real contacts.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_select_customer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.rvCustomers)
        val etSearchCustomer = view.findViewById<android.widget.EditText>(R.id.etSearchCustomer)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = CustomerAdapter(
            list = emptyList(),
            onSelectCustomer = { customer ->
                parentFragmentManager.setFragmentResult(
                    "customer_request",
                    Bundle().apply {
                        putString("customer_name", customer.aitbaarName ?: customer.name)
                        putString("customer_uid", customer.uid)
                        putString("customer_phone", customer.phone)
                    }
                )
                findNavController().popBackStack()
            },
            onInviteCustomer = { customer ->
                sendInvite(customer)
            }
        )
        recycler.adapter = adapter

        etSearchCustomer.isEnabled = false

        requestContactsAndLoad()

        // Back button click
        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun requestContactsAndLoad() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            loadContactsAndMapAitbaarUsers()
        } else {
            contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun loadContactsAndMapAitbaarUsers() {
        val contacts = loadPhoneContacts()
        if (contacts.isEmpty()) {
            Toast.makeText(requireContext(), "No contacts found", Toast.LENGTH_SHORT).show()
            adapter.submitList(emptyList())
            return
        }

        db.collection("customers")
            .get()
            .addOnSuccessListener { query ->
                val registeredByPhone = query.documents
                    .mapNotNull { doc ->
                        val rawPhone = doc.getString("phoneNumber").orEmpty()
                        val normalizedPhone = normalizePhone(rawPhone)
                        if (normalizedPhone.isBlank()) return@mapNotNull null

                        val uid = doc.id
                        val name = doc.getString("name").orEmpty()
                        val phone = rawPhone.ifBlank { normalizedPhone }
                        normalizedPhone to RegisteredCustomer(uid, name, phone)
                    }
                    .toMap()

                val mapped = contacts.map { contact ->
                    val normalized = normalizePhone(contact.phone)
                    val registered = registeredByPhone[normalized]
                    contact.copy(
                        uid = registered?.uid,
                        phone = registered?.phone ?: contact.phone,
                        isOnAitbaar = registered != null,
                        aitbaarName = registered?.name
                    )
                }.sortedWith(
                    compareByDescending<Customer> { it.isOnAitbaar }
                        .thenBy { (it.aitbaarName ?: it.name).lowercase() }
                )

                adapter.submitList(mapped)
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Could not check Aitbaar users. Showing contacts only.",
                    Toast.LENGTH_SHORT
                ).show()
                adapter.submitList(contacts)
            }
    }

    private fun loadPhoneContacts(): List<Customer> {
        val result = mutableListOf<Customer>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val cursor = requireContext().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex)?.trim().orEmpty()
                val phone = it.getString(phoneIndex)?.trim().orEmpty()
                if (name.isNotBlank() && phone.isNotBlank()) {
                    result.add(Customer(name = name, phone = phone))
                }
            }
        }

        return result.distinctBy { normalizePhone(it.phone) }
    }

    private fun normalizePhone(raw: String): String {
        return raw.replace(Regex("[^+\\d]"), "")
    }

    private fun sendInvite(customer: Customer) {
        val appLink = "https://aitbaar.app/invite"
        val smsBody = "Join me on Aitbaar for trusted digital ledger: $appLink"
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${customer.phone}")
            putExtra("sms_body", smsBody)
        }

        if (smsIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(smsIntent)
        } else {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, smsBody)
            }
            startActivity(Intent.createChooser(shareIntent, "Invite via"))
        }
    }
}
