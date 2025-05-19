package com.example.khaikhai.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.khaikhai.Address
import com.example.khaikhai.R
import com.google.firebase.database.*
import kotlin.random.Random

class HomeViewModel : ViewModel() {

    // Renamed from _restaurantList to _restaurants for consistency with HomeFragment
    private val _restaurants = MutableLiveData<List<Restaurant>>()
    val restaurants: LiveData<List<Restaurant>> = _restaurants

    // Added isLoading LiveData
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Address-related LiveData
    private val _address = MutableLiveData<String>()
    val address: LiveData<String> = _address

    private val _defaultAddress = MutableLiveData<Address?>()
    val defaultAddress: LiveData<Address?> = _defaultAddress

    // Reference to Firebase database
    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val restaurantsReference: DatabaseReference = databaseReference.child("restaurants")
    private val customersReference: DatabaseReference = databaseReference.child("customers")

    // All restaurants from Firebase
    private var allRestaurants = mutableListOf<Restaurant>()

    // Current category filter
    private var currentCategory: String? = null

    init {
        // Set a default placeholder value initially
        _address.value = "Loading address..."
    }

    // Added loadRestaurants function
    fun loadRestaurants() {
        _isLoading.value = true
        loadRestaurantsFromFirebase()
    }

    // Function to load the user's default address
    fun loadUserDefaultAddress(userEmail: String?) {
        if (userEmail == null) {
            _address.value = "No address selected"
            return
        }

        customersReference.orderByChild("email").equalTo(userEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        _address.value = "No address found"
                        return
                    }

                    // Loop through users (should be just one)
                    for (userSnapshot in snapshot.children) {
                        // Get addresses
                        val addressesSnapshot = userSnapshot.child("addresses")

                        var foundDefaultAddress = false

                        // Loop through addresses to find the default one
                        for (addressSnapshot in addressesSnapshot.children) {
                            val address = addressSnapshot.getValue(Address::class.java)

                            if (address != null && address.isDefault) {
                                // Format the address
                                val formattedAddress = "${address.street}, ${address.city}"
                                _address.value = formattedAddress
                                _defaultAddress.value = address
                                foundDefaultAddress = true
                                break
                            }
                        }

                        // If no default address was found but there are addresses
                        if (!foundDefaultAddress && addressesSnapshot.childrenCount > 0) {
                            // Use the first address
                            val firstAddress = addressesSnapshot.children.first().getValue(Address::class.java)
                            if (firstAddress != null) {
                                val formattedAddress = "${firstAddress.street}, ${firstAddress.city}"
                                _address.value = formattedAddress
                                _defaultAddress.value = firstAddress
                            } else {
                                _address.value = "No address available"
                            }
                        } else if (!foundDefaultAddress) {
                            _address.value = "No address available"
                        }

                        break // Only process the first user
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _address.value = "Error loading address"
                }
            })
    }

    private fun loadRestaurantsFromFirebase() {
        restaurantsReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val restaurantsList = mutableListOf<Restaurant>()

                for (restaurantSnapshot in snapshot.children) {
                    val restaurantId = restaurantSnapshot.key ?: continue
                    val name = restaurantSnapshot.child("restaurantName").getValue(String::class.java) ?: ""
                    val address = restaurantSnapshot.child("restaurantAddress").getValue(String::class.java) ?: ""
                    val phone = restaurantSnapshot.child("restaurantPhone").getValue(String::class.java) ?: ""
                    val email = restaurantSnapshot.child("restaurantEmail").getValue(String::class.java) ?: ""

                    // Get delivery time from Firebase if available, or generate a sensible default
                    val averageDeliveryTimeMinutes = restaurantSnapshot.child("averageDeliveryTime").getValue(Int::class.java) ?: 30
                    val deliveryTimeRange = "${averageDeliveryTimeMinutes-5}-${averageDeliveryTimeMinutes+10} min"

                    // Assign a random cuisine from popular categories (in real app, this would come from restaurant data)
                    val cuisine = restaurantSnapshot.child("cuisineType").getValue(String::class.java)?:"All Indian Food"


                    // Generate a random rating between 3.5 and 5.0
                    val rating = (35 + Random.nextInt(16)) / 10.0f

                    // Generate a random delivery fee between $1.99 and $4.99
                    val deliveryFee = "$${1 + Random.nextInt(4)}.99"

                    // Choose an appropriate icon based on cuisine (in real app, would use restaurant images)
                    val imageResId = when(cuisine.lowercase()) {
                        "pizza" -> R.drawable.logo_res // Replace with appropriate drawable
                        "burgers" -> R.drawable.logo_res
                        "sushi" -> R.drawable.logo_res
                        "dessert" -> R.drawable.logo_res
                        "drinks" -> R.drawable.logo_res
                        else -> R.drawable.logo_res
                    }

                    // Create a Restaurant object with data from Firebase
                    val restaurant = Restaurant(
                        id = restaurantId,  // Use the original Firebase key instead of a hash code
                        name = name,
                        cuisine = cuisine,
                        rating = rating,
                        deliveryTime = deliveryTimeRange,
                        deliveryFee = deliveryFee,
                        imageResId = imageResId,
                        address = address,
                        phone = phone,
                        email = email
                    )
                    restaurantsList.add(restaurant)
                }

                // Sort by rating (highest first)
                restaurantsList.sortByDescending { it.rating }

                // Store all restaurants for filtering
                allRestaurants = restaurantsList

                // Apply current category filter if any
                if (currentCategory != null) {
                    _restaurants.value = allRestaurants.filter { it.cuisine.equals(currentCategory, ignoreCase = true) }
                } else {
                    _restaurants.value = allRestaurants
                }

                // Set loading to false once data is loaded
                _isLoading.value = false
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
                // Log error message
                _isLoading.value = false
            }
        })
    }

    // Function to filter restaurants by category
    fun filterByCategory(category: String) {
        currentCategory = category
        _restaurants.value = allRestaurants.filter { it.cuisine.equals(category, ignoreCase = true) }
    }

    // Clear category filter and show all restaurants
    fun clearCategoryFilter() {
        currentCategory = null
        _restaurants.value = allRestaurants
    }

    // Add these methods to HomeViewModel.kt

    // Search functionality
    fun searchRestaurants(query: String) {
        if (query.isBlank()) {
            clearSearchFilter()
            return
        }

        val searchResults = allRestaurants.filter { restaurant ->
            restaurant.name.contains(query, ignoreCase = true) ||
                    restaurant.cuisine.contains(query, ignoreCase = true)
        }

        _restaurants.value = searchResults
    }

    // Clear search filter and show category-filtered or all restaurants
    fun clearSearchFilter() {
        if (currentCategory != null) {
            filterByCategory(currentCategory!!)
        } else {
            _restaurants.value = allRestaurants
        }
    }
}

// Updated Restaurant data class with additional fields
data class Restaurant(
    val id: String,  // Changed from Int to String
    val name: String,
    val cuisine: String,
    val rating: Float,
    val deliveryTime: String,
    val deliveryFee: String,
    val imageResId: Int,
    val address: String = "",
    val phone: String = "",
    val email: String = ""
)