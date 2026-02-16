package com.akeel.aitbaar.ui.vendor

import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.NavOptions
import com.akeel.aitbaar.R

object VendorNavHelper {

    // 🔹 Setup bottom navigation click handling
    fun setup(fragment: Fragment, view: View) {

        val navController = fragment.findNavController()

        val options = NavOptions.Builder()
            .setPopUpTo(R.id.vendorDashboardFragment, false)
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .build()

        view.findViewById<View>(R.id.tabHome)?.setOnClickListener {
            navController.navigate(R.id.vendorDashboardFragment, null, options)
        }

        view.findViewById<View>(R.id.tabCustomers)?.setOnClickListener {
            navController.navigate(R.id.customersFragment, null, options)
        }

        view.findViewById<View>(R.id.tabConfirm)?.setOnClickListener {
            navController.navigate(R.id.confirmFragment, null, options)
        }

        view.findViewById<View>(R.id.tabProfile)?.setOnClickListener {
            navController.navigate(R.id.vendorProfileFragment, null, options)
        }
    }

    // 🔹 Highlight selected tab correctly
    fun highlight(fragment: Fragment, view: View, selectedIconId: Int) {

        val defaultColor = fragment.requireContext().getColor(R.color.gray)
        val selectedColor = fragment.requireContext().getColor(R.color.blue)

        val icons = listOf(
            R.id.iconHome,
            R.id.iconCustomer,
            R.id.iconConfirm,
            R.id.iconProfile
        )

        // reset all to gray
        icons.forEach { id ->
            view.findViewById<ImageView>(id)?.setColorFilter(defaultColor)
        }

        // highlight selected
        view.findViewById<ImageView>(selectedIconId)?.setColorFilter(selectedColor)
    }
}
