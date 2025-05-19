package com.example.petukrestaurantpartner.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.petukrestaurantpartner.Restaurant
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileViewModel : ViewModel() {

    private val _restaurant = MutableLiveData<Restaurant>()
    val restaurant: LiveData<Restaurant> = _restaurant

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _isRestaurantOpen = MutableLiveData<Boolean>()
    val isRestaurantOpen: LiveData<Boolean> = _isRestaurantOpen

    // Status to track if restaurant data has been successfully loaded
    private val _dataLoaded = MutableLiveData<Boolean>()
    val dataLoaded: LiveData<Boolean> = _dataLoaded

    fun loadRestaurantData(restaurantId: String) {
        _isLoading.value = true
        _dataLoaded.value = false

        val databaseReference = FirebaseDatabase.getInstance().getReference("restaurants")
        databaseReference.child(restaurantId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _isLoading.value = false

                if (snapshot.exists()) {
                    val restaurant = snapshot.getValue(Restaurant::class.java)
                    restaurant?.let {
                        _restaurant.value = it
                        _isRestaurantOpen.value = it.isActive
                        _dataLoaded.value = true
                    }
                } else {
                    _errorMessage.value = "Restaurant data not found"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _isLoading.value = false
                _errorMessage.value = "Error loading restaurant data: ${error.message}"
            }
        })
    }

    fun updateRestaurantStatus(restaurantId: String, isOpen: Boolean) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("restaurants")
        databaseReference.child(restaurantId).child("isActive").setValue(isOpen)
            .addOnSuccessListener {
                _isRestaurantOpen.value = isOpen
            }
            .addOnFailureListener {
                _errorMessage.value = "Failed to update restaurant status"
                // Revert the switch state if update fails
                _isRestaurantOpen.value = !isOpen
            }
    }

    fun logout() {
        // Clear any local cached data if needed
        _restaurant.value = null
        _dataLoaded.value = false
    }
}