package com.example.khaikhai.cart

import com.example.khaikhai.ui.menu.MenuItem

data class CartItem(
    val menuItem: MenuItem,
    var quantity: Int = 1
) {
    fun getTotalPrice(): Double {
        return menuItem.price * quantity
    }
}
