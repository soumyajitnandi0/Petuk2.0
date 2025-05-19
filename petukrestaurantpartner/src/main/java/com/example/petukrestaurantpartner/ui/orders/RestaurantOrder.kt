package com.example.petukrestaurantpartner.ui.orders

import com.google.firebase.database.PropertyName
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class representing an Order for the restaurant app
 */
data class RestaurantOrder(
    @get:PropertyName("orderId")
    @set:PropertyName("orderId")
    var orderId: String? = null,

    @get:PropertyName("customerId")
    @set:PropertyName("customerId")
    var customerId: String? = null,

    @get:PropertyName("customerName")
    @set:PropertyName("customerName")
    var customerName: String? = null,

    @get:PropertyName("restaurantId")
    @set:PropertyName("restaurantId")
    var restaurantId: String? = null,

    @get:PropertyName("restaurantName")
    @set:PropertyName("restaurantName")
    var restaurantName: String? = null,

    @get:PropertyName("items")
    @set:PropertyName("items")
    var items: List<OrderItem>? = null,

    @get:PropertyName("subtotal")
    @set:PropertyName("subtotal")
    var subtotal: Double = 0.0,

    @get:PropertyName("deliveryFee")
    @set:PropertyName("deliveryFee")
    var deliveryFee: Double = 0.0,

    @get:PropertyName("tax")
    @set:PropertyName("tax")
    var tax: Double = 0.0,

    @get:PropertyName("total")
    @set:PropertyName("total")
    var total: Double = 0.0,

    @get:PropertyName("paymentType")
    @set:PropertyName("paymentType")
    var paymentType: String? = null,

    @get:PropertyName("paymentId")
    @set:PropertyName("paymentId")
    var paymentId: String? = null,

    @get:PropertyName("orderStatus")
    @set:PropertyName("orderStatus")
    var orderStatus: String? = null,

    @get:PropertyName("orderDateTime")
    @set:PropertyName("orderDateTime")
    var orderDateTime: Long = 0,

    @get:PropertyName("deliveryAddress")
    @set:PropertyName("deliveryAddress")
    var deliveryAddress: String? = null
) {
    /**
     * Get formatted order date and time
     */
    fun getFormattedDateTime(): String {
        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return dateFormat.format(Date(orderDateTime))
    }

    /**
     * Get the count of items in the order
     */
    fun getItemCount(): Int {
        return items?.sumOf { it.quantity } ?: 0
    }
}

/**
 * Data class representing an item in an order
 */
data class OrderItem(
    @get:PropertyName("itemId")
    @set:PropertyName("itemId")
    var itemId: String? = null,

    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String? = null,

    @get:PropertyName("price")
    @set:PropertyName("price")
    var price: Double = 0.0,

    @get:PropertyName("quantity")
    @set:PropertyName("quantity")
    var quantity: Int = 0,

    @get:PropertyName("totalPrice")
    @set:PropertyName("totalPrice")
    var totalPrice: Double = 0.0
)