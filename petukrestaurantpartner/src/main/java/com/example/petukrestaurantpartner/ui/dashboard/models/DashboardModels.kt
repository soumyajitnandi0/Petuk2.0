package com.example.petukrestaurantpartner.ui.dashboard.models

import java.util.Date

data class Order(
    val orderId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val items: List<OrderItem> = listOf(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double = 0.0,
    val paymentType: String = "",
    val paymentId: String? = null,
    val orderStatus: String = "",
    val orderDateTime: Long = 0L,
    val deliveryAddress: String = ""
) {
    fun getFormattedDate(): String {
        return java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
            .format(Date(orderDateTime))
    }

    // Helper function to check if the order should be included in the dashboard
    fun shouldIncludeInDashboard(): Boolean {
        return orderStatus != "PLACED" && orderStatus != "REJECTED"
    }
}

data class OrderItem(
    val itemId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val totalPrice: Double = 0.0
)

// Class to hold aggregated data about menu items
data class MenuItemStat(
    val itemId: String,
    val name: String,
    val totalQuantity: Int,
    val totalRevenue: Double
)

// Class to hold revenue data points for the line chart
data class RevenueDataPoint(
    val date: String,
    val revenue: Double
)

// Class for dashboard summary stats
data class DashboardSummary(
    val totalOrders: Int,
    val totalRevenue: Double,
    val averageOrderValue: Double,
    val topSellingItems: List<MenuItemStat>,
    val revenueByDate: List<RevenueDataPoint>,
    val recentOrders: List<Order>
)