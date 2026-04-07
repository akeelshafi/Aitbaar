package com.akeel.aitbaar.data.model

data class Customer(
    val name: String,
    val phone: String,
    val isOnAitbaar: Boolean = false,
    val aitbaarName: String? = null
)
