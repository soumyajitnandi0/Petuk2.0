package com.example.petukrestaurantpartner.ui.menu

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.petukrestaurantpartner.ui.menu.MenuItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MenuViewModel : ViewModel() {

    private val databaseReference = FirebaseDatabase.getInstance().reference.child("menu")

    private val _menuItems = MutableLiveData<List<MenuItem>>()
    val menuItems: LiveData<List<MenuItem>> = _menuItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun loadMenuItems(restaurantId: String) {
        _isLoading.value = true

        databaseReference.orderByChild("restaurantId").equalTo(restaurantId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    _isLoading.value = false

                    val items = mutableListOf<MenuItem>()
                    for (itemSnapshot in snapshot.children) {
                        val menuItem = itemSnapshot.getValue(MenuItem::class.java)
                        menuItem?.let {
                            // Make sure itemId is included in case it's not in the data
                            val itemWithId = if (menuItem.itemId == null) {
                                menuItem.copy(itemId = itemSnapshot.key)
                            } else {
                                menuItem
                            }
                            items.add(itemWithId)
                        }
                    }

                    _menuItems.value = items
                }

                override fun onCancelled(error: DatabaseError) {
                    _isLoading.value = false
                    _errorMessage.value = "Failed to load menu items: ${error.message}"
                }
            })
    }

    fun addMenuItem(menuItem: MenuItem, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _isLoading.value = true

        val itemId = databaseReference.push().key ?: return
        val itemWithId = menuItem.copy(itemId = itemId)

        databaseReference.child(itemId).setValue(itemWithId)
            .addOnSuccessListener {
                _isLoading.value = false
                onSuccess()
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                onFailure("Failed to add menu item: ${e.message}")
            }
    }

    fun updateMenuItem(menuItem: MenuItem, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _isLoading.value = true

        menuItem.itemId?.let { itemId ->
            databaseReference.child(itemId).setValue(menuItem)
                .addOnSuccessListener {
                    _isLoading.value = false
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    _isLoading.value = false
                    onFailure("Failed to update menu item: ${e.message}")
                }
        } ?: run {
            _isLoading.value = false
            onFailure("Invalid menu item ID")
        }
    }

    fun updateItemAvailability(itemId: String, isAvailable: Boolean, onFailure: (String) -> Unit) {
        // Fix: Change "available" to "isAvailable" to match the property name
        databaseReference.child(itemId).child("isAvailable").setValue(isAvailable)
            .addOnFailureListener { e ->
                onFailure("Failed to update availability: ${e.message}")
            }
    }

    fun deleteMenuItem(itemId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _isLoading.value = true

        databaseReference.child(itemId).removeValue()
            .addOnSuccessListener {
                _isLoading.value = false
                onSuccess()
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                onFailure("Failed to delete menu item: ${e.message}")
            }
    }
}