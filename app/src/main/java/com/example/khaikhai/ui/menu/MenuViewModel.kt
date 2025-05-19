package com.example.khaikhai.ui.menu

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.*
import android.util.Log

class MenuViewModel : ViewModel() {

    private val databaseReference = FirebaseDatabase.getInstance().reference.child("menu")

    private val _menuItems = MutableLiveData<List<MenuItem>>()
    val menuItems: LiveData<List<MenuItem>> = _menuItems

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun loadMenuItems(restaurantId: String) {
        if (restaurantId.isEmpty()) {
            _errorMessage.value = "Restaurant ID not found"
            return
        }

        Log.d("MenuViewModel", "Loading menu items for restaurant ID: $restaurantId")

        // Query menu items by restaurantId field as this is how the restaurant app stores them
        databaseReference.orderByChild("restaurantId").equalTo(restaurantId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists() || !snapshot.hasChildren()) {
                        Log.d("MenuViewModel", "No menu items found for restaurant: $restaurantId")
                        _errorMessage.value = "No menu items found for this restaurant"
                        _menuItems.value = emptyList()
                        return
                    }

                    val menuItemsList = mutableListOf<MenuItem>()

                    for (itemSnapshot in snapshot.children) {
                        val menuItemId = itemSnapshot.key ?: continue

                        try {
                            // Try to map the data directly to MenuItem class
                            val menuItem = itemSnapshot.getValue(MenuItem::class.java)

                            if (menuItem != null) {
                                // Ensure itemId is set if not already present in the data
                                val itemWithId = if (menuItem.itemId == null) {
                                    menuItem.copy(itemId = menuItemId)
                                } else {
                                    menuItem
                                }
                                menuItemsList.add(itemWithId)
                            } else {
                                // Manual mapping as backup
                                val name = itemSnapshot.child("name").getValue(String::class.java) ?: ""
                                val description = itemSnapshot.child("description").getValue(String::class.java) ?: ""
                                val price = itemSnapshot.child("price").getValue(Double::class.java) ?: 0.0
                                val imageUrl = itemSnapshot.child("imageUrl").getValue(String::class.java) ?: ""
                                val category = itemSnapshot.child("category").getValue(String::class.java) ?: ""
                                val isAvailable = itemSnapshot.child("isAvailable").getValue(Boolean::class.java) ?: true

                                val menuItem = MenuItem(
                                    itemId = menuItemId,
                                    name = name,
                                    description = description,
                                    price = price,
                                    imageUrl = imageUrl,
                                    restaurantId = restaurantId,
                                    category = category,
                                    isAvailable = isAvailable
                                )

                                menuItemsList.add(menuItem)
                            }
                        } catch (e: Exception) {
                            Log.e("MenuViewModel", "Error mapping menu item: ${e.message}")
                        }
                    }

                    // Group menu items by category if needed
                    menuItemsList.sortBy { it.category }

                    // Filter out items that are not available
                    val availableItems = menuItemsList.filter { it.isAvailable }

                    Log.d("MenuViewModel", "Loaded ${availableItems.size} available menu items out of ${menuItemsList.size} total")
                    _menuItems.value = availableItems
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MenuViewModel", "Error loading menu items: ${error.message}")
                    _errorMessage.value = "Failed to load menu items: ${error.message}"
                }
            })
    }
}