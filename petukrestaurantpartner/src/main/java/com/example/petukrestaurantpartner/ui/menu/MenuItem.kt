package com.example.petukrestaurantpartner.ui.menu


data class MenuItem(
    val itemId: String? = null,
    val restaurantId: String? = null,
    val restaurantName: String? = null,
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val isAvailable: Boolean = false
) {
    // Empty constructor needed for Firebase
    constructor() : this(null, null, null, "", "", 0.0, "", true)
}