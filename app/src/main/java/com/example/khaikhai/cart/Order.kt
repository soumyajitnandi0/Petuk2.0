package com.example.khaikhai.cart

import java.io.Serializable

data class Order(
    val orderId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val items: List<OrderItem?> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double = 0.0,
    val paymentType: String = "",
    val paymentId: String? = null,
    val orderStatus: String = "PLACED",
    val orderDateTime: Long = 0,
    val deliveryAddress: String = ""
) : Serializable

data class OrderItem(
    val itemId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val totalPrice: Double = 0.0
) : Serializable