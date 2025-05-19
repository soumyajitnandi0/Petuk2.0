package com.example.khaikhai.ui.menu

data class MenuItem(
    val itemId: String? = null,
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val restaurantId: String? = "",
    val category: String = "",
    val isAvailable: Boolean = true
) {
    // Empty constructor for Firebase
    constructor() : this(null, "", "", 0.0, "", "", "", true)
}
