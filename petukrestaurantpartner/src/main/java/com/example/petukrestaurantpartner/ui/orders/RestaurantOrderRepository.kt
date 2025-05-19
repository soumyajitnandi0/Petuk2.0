package com.example.petukrestaurantpartner.ui.orders


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.petukrestaurantpartner.ui.orders.RestaurantOrder
import com.google.firebase.database.*
import timber.log.Timber

/**
 * Repository class for handling restaurant orders from Firebase Realtime Database
 */
class RestaurantOrderRepository {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference.child("orders")

    // Restaurant ID would typically come from user profile or settings
    // For now, we'll use a hardcoded value or get it from SharedPreferences
    private var currentRestaurantId: String? = null

    // LiveData for different order types
    private val _liveOrders = MutableLiveData<List<RestaurantOrder>>()
    val liveOrders: LiveData<List<RestaurantOrder>> = _liveOrders

    private val _preparedOrders = MutableLiveData<List<RestaurantOrder>>()
    val preparedOrders: LiveData<List<RestaurantOrder>> = _preparedOrders

    private val _deliveredOrders = MutableLiveData<List<RestaurantOrder>>()
    val deliveredOrders: LiveData<List<RestaurantOrder>> = _deliveredOrders

    // Error handling
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Set the current restaurant ID - should be called during app initialization
     */
    fun setRestaurantId(restaurantId: String) {
        currentRestaurantId = restaurantId
    }

    /**
     * Fetch all orders for the current restaurant
     */
    fun fetchOrders() {
        if (currentRestaurantId == null) {
            _error.value = "Restaurant ID not set. Please login again."
            return
        }

        _isLoading.value = true

        // Query orders for the current restaurant
        database.orderByChild("restaurantId").equalTo("-OQ2VO17Z8GtcM8QIYU4")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val allOrders = mutableListOf<RestaurantOrder>()

                        for (orderSnapshot in snapshot.children) {
                            val order = orderSnapshot.getValue(RestaurantOrder::class.java)
                            order?.let {
                                // Make sure orderId is set (from the key if needed)
                                if (it.orderId.isNullOrEmpty()) {
                                    it.orderId = orderSnapshot.key
                                }
                                allOrders.add(it)
                            }
                        }

                        // Sort orders by timestamp (newest first)
                        allOrders.sortByDescending { it.orderDateTime }

                        // Filter orders by status
                        val live = allOrders.filter {
                            it.orderStatus == "PLACED" ||
                                    it.orderStatus == "ACCEPTED" ||
                                    it.orderStatus == "COOKING"
                        }

                        val prepared = allOrders.filter {
                            it.orderStatus == "PREPARED" ||
                                    it.orderStatus == "OUT_FOR_DELIVERY"
                        }

                        val delivered = allOrders.filter {
                            it.orderStatus == "DELIVERED" ||
                                    it.orderStatus == "COMPLETED"
                        }

                        // Update LiveData
                        _liveOrders.value = live
                        _preparedOrders.value = prepared
                        _deliveredOrders.value = delivered

                        Timber.d("Orders fetched: ${allOrders.size} total, ${live.size} live, ${prepared.size} prepared, ${delivered.size} delivered")

                        _error.value = null
                    } catch (e: Exception) {
                        Timber.e(e, "Error processing orders")
                        _error.value = "Failed to process orders: ${e.message}"
                    } finally {
                        _isLoading.value = false
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.e("Order fetch cancelled: ${error.message}")
                    _error.value = "Failed to fetch orders: ${error.message}"
                    _isLoading.value = false
                }
            })
    }

    /**
     * Update order status in Firebase
     */
    fun updateOrderStatus(orderId: String, newStatus: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        _isLoading.value = true

        Timber.d("Updating order $orderId status to $newStatus")

        // Create a map of values to update
        val updates = hashMapOf<String, Any>(
            "orderStatus" to newStatus
        )

        // Update only the status field
        database.child(orderId).updateChildren(updates)
            .addOnSuccessListener {
                Timber.d("Order status updated successfully")
                onSuccess()
                fetchOrders() // Refresh orders after update
            }
            .addOnFailureListener { e ->
                Timber.e(e, "Failed to update order status")
                onFailure("Failed to update order: ${e.message}")
                _isLoading.value = false
            }
    }

    /**
     * Accept an order - changes status from PLACED to ACCEPTED
     */
    fun acceptOrder(orderId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        updateOrderStatus(orderId, "ACCEPTED", onSuccess, onFailure)
    }

    /**
     * Reject an order - changes status to REJECTED
     */
    fun rejectOrder(orderId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        updateOrderStatus(orderId, "REJECTED", onSuccess, onFailure)
    }

    /**
     * Mark an order as prepared - changes status to PREPARED
     */
    fun markOrderPrepared(orderId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        updateOrderStatus(orderId, "PREPARED", onSuccess, onFailure)
    }

    /**
     * Mark an order as delivered - changes status to DELIVERED
     */
    fun markOrderDelivered(orderId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        updateOrderStatus(orderId, "DELIVERED", onSuccess, onFailure)
    }
}