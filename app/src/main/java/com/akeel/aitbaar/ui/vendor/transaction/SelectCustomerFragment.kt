package com.akeel.aitbaar.ui.vendor.transaction

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.akeel.aitbaar.R
import com.akeel.aitbaar.data.model.Customer
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class SelectCustomerFragment : Fragment() {

    companion object {
        private const val TAG = "SelectCustomerFragment"
    }

    private lateinit var adapter: CustomerAdapter
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var allContacts: List<Customer> = emptyList()

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
                    Bundle().apply { putString("customer_name", customer.aitbaarName ?: customer.name) }
                )
                findNavController().popBackStack()
            },
            onInviteCustomer = { customer ->
                sendInvite(customer)
            }
        )
        recycler.adapter = adapter

        requestContactsAndLoad()
        etSearchCustomer.doAfterTextChanged { text ->
            filterContacts(text?.toString().orEmpty())
        }

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
            allContacts = emptyList()
            adapter.submitList(emptyList())
            return
        }

        // Prefer directory collection (recommended production setup).
        db.collection("customerDirectory")
            .get()
            .addOnSuccessListener { query ->
                val registeredByPhone = mapRegisteredUsersByPhone(query.documents)
                allContacts = mapContacts(contacts, registeredByPhone)
                filterContacts("")
            }
            .addOnFailureListener { directoryError ->
                // Backward compatibility: if directory isn't configured yet, try legacy customers collection.
                db.collection("customers")
                    .get()
                    .addOnSuccessListener { legacyQuery ->
                        val registeredByPhone = mapRegisteredUsersByPhone(legacyQuery.documents)
                        allContacts = mapContacts(contacts, registeredByPhone)
                        filterContacts("")
                    }
                    .addOnFailureListener { customersError ->
                        Log.e(TAG, "Directory lookup failed", directoryError)
                        Log.e(TAG, "Legacy customers lookup failed", customersError)
                        Toast.makeText(
                            requireContext(),
                            "Showing contacts only. Check Firestore rules and authentication.",
                            Toast.LENGTH_LONG
                        ).show()
                        allContacts = contacts
                        filterContacts("")
                    }
            }
    }

    private fun filterContacts(query: String) {
        val normalized = query.trim().lowercase(Locale.getDefault())
        val filtered = if (normalized.isBlank()) {
            allContacts
        } else {
            allContacts.filter { contact ->
                val displayName = (contact.aitbaarName ?: contact.name).lowercase(Locale.getDefault())
                val phone = contact.phone.lowercase(Locale.getDefault())
                displayName.contains(normalized) || phone.contains(normalized)
            }
        }
        adapter.submitList(filtered)
    }

    private fun mapRegisteredUsersByPhone(
        documents: List<com.google.firebase.firestore.DocumentSnapshot>
    ): Map<String, String> {
        return documents
            .mapNotNull { doc ->
                val phone = normalizePhone(doc.getString("phoneNumber").orEmpty())
                if (phone.isBlank()) return@mapNotNull null
                phone to doc.getString("name").orEmpty()
            }
            .toMap()
    }

    private fun mapContacts(
        contacts: List<Customer>,
        registeredByPhone: Map<String, String>
    ): List<Customer> {
        return contacts.map { contact ->
            val normalized = normalizePhone(contact.phone)
            val aitbaarName = registeredByPhone[normalized]
            contact.copy(
                isOnAitbaar = aitbaarName != null,
                aitbaarName = aitbaarName
            )
        }.sortedWith(
            compareByDescending<Customer> { it.isOnAitbaar }
                .thenBy { (it.aitbaarName ?: it.name).lowercase() }
        )
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
