package com.example.khaikhai.cart

import android.content.Context
import android.content.SharedPreferences
import com.example.khaikhai.ui.menu.MenuItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CartManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(CART_PREFS, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val CART_PREFS = "cart_prefs"
        private const val CART_ITEMS = "cart_items"
        private const val RESTAURANT_ID = "restaurant_id"
        private const val RESTAURANT_NAME = "restaurant_name"
    }

    // Get current restaurant ID
    fun getRestaurantId(): String? {
        return sharedPreferences.getString(RESTAURANT_ID, null)
    }

    // Get current restaurant name
    fun getRestaurantName(): String? {
        return sharedPreferences.getString(RESTAURANT_NAME, null)
    }

    // Set current restaurant
    private fun setRestaurant(restaurantId: String, restaurantName: String) {
        sharedPreferences.edit()
            .putString(RESTAURANT_ID, restaurantId)
            .putString(RESTAURANT_NAME, restaurantName)
            .apply()
    }

    // Get all cart items
    fun getCartItems(): List<CartItem> {
        val json = sharedPreferences.getString(CART_ITEMS, null) ?: return emptyList()
        val type = object : TypeToken<List<CartItem>>() {}.type
        return gson.fromJson(json, type)
    }

    // Save cart items
    private fun saveCartItems(cartItems: List<CartItem>) {
        val json = gson.toJson(cartItems)
        sharedPreferences.edit()
            .putString(CART_ITEMS, json)
            .apply()
    }

    // Add item to cart
    fun addItem(menuItem: MenuItem, restaurantId: String, restaurantName: String): AddToCartResult {
        val currentRestaurantId = getRestaurantId()

        // If cart is not empty and item is from a different restaurant
        if (currentRestaurantId != null && currentRestaurantId != restaurantId && getCartItems().isNotEmpty()) {
            return AddToCartResult.DifferentRestaurant(getRestaurantName() ?: "")
        }

        // If cart is empty or first item, set the restaurant
        if (currentRestaurantId == null || getCartItems().isEmpty()) {
            setRestaurant(restaurantId, restaurantName)
        }

        val cartItems = getCartItems().toMutableList()

        // Check if item already exists in cart
        val existingItemIndex = cartItems.indexOfFirst { it.menuItem.itemId == menuItem.itemId }

        if (existingItemIndex != -1) {
            // Increment quantity if item already exists
            val existingItem = cartItems[existingItemIndex]
            cartItems[existingItemIndex] = existingItem.copy(quantity = existingItem.quantity + 1)
        } else {
            // Add new item to cart
            cartItems.add(CartItem(menuItem, 1))
        }

        saveCartItems(cartItems)
        return AddToCartResult.Success
    }

    // Update item quantity
    fun updateItemQuantity(menuItemId: String, quantity: Int) {
        if (quantity <= 0) {
            removeItem(menuItemId)
            return
        }

        val cartItems = getCartItems().toMutableList()
        val itemIndex = cartItems.indexOfFirst { it.menuItem.itemId == menuItemId }

        if (itemIndex != -1) {
            cartItems[itemIndex] = cartItems[itemIndex].copy(quantity = quantity)
            saveCartItems(cartItems)
        }
    }

    // Remove item from cart
    fun removeItem(menuItemId: String) {
        val cartItems = getCartItems().toMutableList()
        cartItems.removeAll { it.menuItem.itemId == menuItemId }

        saveCartItems(cartItems)

        // If cart is empty, clear restaurant info
        if (cartItems.isEmpty()) {
            clearCart()
        }
    }

    // Clear cart
    fun clearCart() {
        sharedPreferences.edit()
            .remove(CART_ITEMS)
            .remove(RESTAURANT_ID)
            .remove(RESTAURANT_NAME)
            .apply()
    }

    // Get cart total
    fun getCartTotal(): Double {
        return getCartItems().sumOf { it.getTotalPrice() }
    }

    // Get cart item count
    fun getCartItemCount(): Int {
        return getCartItems().sumOf { it.quantity }
    }
}

sealed class AddToCartResult {
    object Success : AddToCartResult()
    data class DifferentRestaurant(val currentRestaurantName: String) : AddToCartResult()
}
