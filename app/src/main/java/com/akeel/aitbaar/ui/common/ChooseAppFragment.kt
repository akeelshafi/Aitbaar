package com.akeel.aitbaar.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.akeel.aitbaar.R

class ChooseAppFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_choose_app, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnCustomer = view.findViewById<CardView>(R.id.btnCustomer)
        val btnVendor = view.findViewById<CardView>(R.id.btnVendor)

        btnCustomer.setOnClickListener {
            val action = ChooseAppFragmentDirections
                .actionChooseAppToCustomerPhoneLoginFragment(userRole = "customer")
            findNavController().navigate(action)
        }

        btnVendor.setOnClickListener {
            val action = ChooseAppFragmentDirections
                .actionChooseAppToCustomerPhoneLoginFragment(userRole = "vendor")
            findNavController().navigate(action)
        }



    }

}