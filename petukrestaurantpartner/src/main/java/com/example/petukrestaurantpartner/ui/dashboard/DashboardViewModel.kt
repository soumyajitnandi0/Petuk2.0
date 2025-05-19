package com.example.petukrestaurantpartner.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.petukrestaurantpartner.ui.dashboard.models.*
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class DashboardViewModel : ViewModel() {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val ordersRef: DatabaseReference = database.getReference("orders")

    private val _dashboardSummary = MutableLiveData<DashboardSummary>()
    val dashboardSummary: LiveData<DashboardSummary> = _dashboardSummary

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    // Date filters
    private var startDate: Calendar? = null
    private var endDate: Calendar? = null

    init {
        // Set default date range to last 30 days
        setDefaultDateRange()
        loadDashboardData()
    }

    private fun setDefaultDateRange() {
        // End date is today
        endDate = Calendar.getInstance()

        // Start date is 30 days ago
        startDate = Calendar.getInstance()
        startDate?.add(Calendar.DAY_OF_MONTH, -30)
    }

    fun setDateRange(start: Calendar, end: Calendar) {
        startDate = start
        endDate = end
        loadDashboardData()
    }

    fun loadDashboardData() {
        _isLoading.value = true

        // Fetch all orders
        ordersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val allOrders = mutableListOf<Order>()

                    for (orderSnapshot in snapshot.children) {
                        val order = orderSnapshot.getValue(Order::class.java)

                        if (order != null &&
                            order.shouldIncludeInDashboard() &&
                            isOrderInDateRange(order)) {
                            allOrders.add(order)
                        }
                    }

                    // Process the orders to generate dashboard data
                    processDashboardData(allOrders)

                } catch (e: Exception) {
                    _error.value = "Error processing orders: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _error.value = "Database error: ${error.message}"
                _isLoading.value = false
            }
        })
    }

    private fun isOrderInDateRange(order: Order): Boolean {
        if (startDate == null || endDate == null) return true

        val orderDate = Calendar.getInstance()
        orderDate.timeInMillis = order.orderDateTime

        // Clone end date and set to end of day
        val endOfDay = endDate?.clone() as Calendar
        endOfDay.set(Calendar.HOUR_OF_DAY, 23)
        endOfDay.set(Calendar.MINUTE, 59)
        endOfDay.set(Calendar.SECOND, 59)

        return !orderDate.before(startDate) && !orderDate.after(endOfDay)
    }

    private fun processDashboardData(orders: List<Order>) {
        if (orders.isEmpty()) {
            _dashboardSummary.value = DashboardSummary(
                totalOrders = 0,
                totalRevenue = 0.0,
                averageOrderValue = 0.0,
                topSellingItems = emptyList(),
                revenueByDate = emptyList(),
                recentOrders = emptyList()
            )
            return
        }

        // Calculate total orders and revenue
        val totalOrders = orders.size
        val totalRevenue = orders.sumOf { it.total }
        val averageOrderValue = if (totalOrders > 0) totalRevenue / totalOrders else 0.0

        // Process menu items data
        val menuItemsMap = HashMap<String, MenuItemStat>()

        orders.forEach { order ->
            order.items.forEach { item ->
                val existing = menuItemsMap[item.itemId]
                if (existing != null) {
                    menuItemsMap[item.itemId] = existing.copy(
                        totalQuantity = existing.totalQuantity + item.quantity,
                        totalRevenue = existing.totalRevenue + item.totalPrice
                    )
                } else {
                    menuItemsMap[item.itemId] = MenuItemStat(
                        itemId = item.itemId,
                        name = item.name,
                        totalQuantity = item.quantity,
                        totalRevenue = item.totalPrice
                    )
                }
            }
        }

        // Get top selling items by quantity
        val topSellingItems = menuItemsMap.values.sortedByDescending { it.totalQuantity }.take(5)

        // Process revenue by date
        val revenueByDateMap = HashMap<String, Double>()
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

        orders.forEach { order ->
            val dateStr = dateFormat.format(Date(order.orderDateTime))
            revenueByDateMap[dateStr] = (revenueByDateMap[dateStr] ?: 0.0) + order.total
        }

        // Convert to sorted list of data points
        val revenueDataPoints = revenueByDateMap.map { (date, revenue) ->
            RevenueDataPoint(date, revenue)
        }.sortedBy {
            // Parse date for proper sorting
            SimpleDateFormat("dd MMM", Locale.getDefault()).parse(it.date)?.time ?: 0L
        }

        // Get recent orders, sorted by date descending
        val recentOrders = orders.sortedByDescending { it.orderDateTime }.take(5)

        // Update LiveData with the processed data
        _dashboardSummary.value = DashboardSummary(
            totalOrders = totalOrders,
            totalRevenue = totalRevenue,
            averageOrderValue = averageOrderValue,
            topSellingItems = topSellingItems,
            revenueByDate = revenueDataPoints,
            recentOrders = recentOrders
        )
    }

    fun getFormattedStartDate(): String {
        return startDate?.let {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it.time)
        } ?: "Not set"
    }

    fun getFormattedEndDate(): String {
        return endDate?.let {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it.time)
        } ?: "Not set"
    }
}