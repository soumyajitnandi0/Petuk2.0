package com.example.khaikhai

data class Customer(
    val id: String? = null,
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val profilePhotoUrl: String = "",
    val bio: String? = null,
    val addresses: List<Address>? = null,
    val favorites: List<String>? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Required empty constructor for Firebase
    constructor() : this(null, "", "", "", "", "", null, null, null)

    // Remove the manual copy() function - Kotlin data class already generates it
}

data class Address(
    val id: String? = null,
    val label: String = "",
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val country: String = "",
    var isDefault: Boolean = false
) {
    // Required empty constructor for Firebase
    constructor() : this(null, "", "", "", "", "", "", false)
}