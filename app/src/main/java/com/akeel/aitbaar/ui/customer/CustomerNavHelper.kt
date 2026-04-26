package com.akeel.aitbaar.ui.customer

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.akeel.aitbaar.R

object CustomerNavHelper {

    fun setup(fragment: Fragment, view: View) {
        val navController = fragment.findNavController()
        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .build()

        view.findViewById<View>(R.id.tabHome)?.setOnClickListener {
            if (navController.currentDestination?.id != R.id.customerDashboardFragment) {
                navController.navigate(R.id.customerDashboardFragment, null, options)
            }
        }

        view.findViewById<View>(R.id.tabCustomers)?.setOnClickListener {
            if (navController.currentDestination?.id != R.id.fragmentVendors) {
                navController.navigate(R.id.fragmentVendors, null, options)
            }
        }

        view.findViewById<View>(R.id.tabConfirm)?.setOnClickListener {
            if (navController.currentDestination?.id != R.id.customerConfirmFragment) {
                navController.navigate(R.id.customerConfirmFragment, null, options)
            }
        }

        view.findViewById<View>(R.id.tabProfile)?.setOnClickListener {
            if (navController.currentDestination?.id != R.id.customerProfileFragment) {
                navController.navigate(R.id.customerProfileFragment, null, options)
            }
        }
    }

    fun highlight(fragment: Fragment, view: View, selectedTabId: Int) {
        val defaultColor = fragment.requireContext().getColor(R.color.gray)
        val selectedColor = fragment.requireContext().getColor(R.color.blue)

        val tabs = listOf(
            R.id.tabHome,
            R.id.tabCustomers,
            R.id.tabConfirm,
            R.id.tabProfile
        )

        tabs.forEach { tabId ->
            val tab = view.findViewById<View>(tabId) ?: return@forEach
            val isSelected = tabId == selectedTabId
            val color = if (isSelected) selectedColor else defaultColor
            tab.alpha = if (isSelected) 1f else 0.72f

            findFirstImage(tab)?.setColorFilter(color)
            findFirstText(tab)?.setTextColor(color)
        }
    }

    private fun findFirstImage(view: View): ImageView? {
        if (view is ImageView) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                val found = findFirstImage(view.getChildAt(index))
                if (found != null) return found
            }
        }
        return null
    }

    private fun findFirstText(view: View): TextView? {
        if (view is TextView) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                val found = findFirstText(view.getChildAt(index))
                if (found != null) return found
            }
        }
        return null
    }
}