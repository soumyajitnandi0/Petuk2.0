package com.example.petukrestaurantpartner

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Restaurant(
    val id: String? = null,
    val restaurantName: String? = null,
    val restaurantAddress: String? = null,
    val restaurantPhone: String? = null,
    val restaurantEmail: String? = null,
    val restaurantPassword: String? = null,
    val cuisineType: String? = null,
    val averageDeliveryTime: Int = 30, // Default 30 minutes
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable {
    // No-arg constructor needed for Firebase
    constructor() : this(null, null, null, null, null, null)
}