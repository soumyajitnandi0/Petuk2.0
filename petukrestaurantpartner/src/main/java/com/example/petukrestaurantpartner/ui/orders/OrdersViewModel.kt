package com.example.petukrestaurantpartner.ui.orders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import timber.log.Timber

/**
 * ViewModel for the Orders screen
 * Manages data for all three types of orders: live, prepared, and delivered
 */
class OrdersViewModel : ViewModel() {

    // Repository for data operations
    private val repository = RestaurantOrderRepository()

    // Tab navigation (used to switch tabs programmatically)
    private val _tabNavigationEvent = MutableLiveData<Int?>()
    val tabNavigationEvent: LiveData<Int?> = _tabNavigationEvent

    // Forward repository LiveData
    val liveOrders = repository.liveOrders
    val preparedOrders = repository.preparedOrders
    val deliveredOrders = repository.deliveredOrders
    val isLoading = repository.isLoading

    // Error handling with custom error message clearing
    private val _error = MediatorLiveData<String?>()
    val error: LiveData<String?> = _error

    // Status update success event
    private val _statusUpdateSuccess = MutableLiveData<StatusUpdateEvent?>()
    val statusUpdateSuccess: LiveData<StatusUpdateEvent?> = _statusUpdateSuccess

    init {
        // Forward repository errors but allow clearing
        _error.addSource(repository.error) { errorMessage ->
            _error.value = errorMessage
        }

        // Set restaurant ID - in a real app, this would come from user preferences or login
        // For testing, use a hardcoded ID that matches your test data
        repository.setRestaurantId("test_restaurant_id")

        // Fetch orders initially
        fetchOrders()
    }

    /**
     * Fetch all orders from repository
     */
    fun fetchOrders() {
        Timber.d("Fetching orders from repository")
        repository.fetchOrders()
    }

    /**
     * Clear any tab navigation event after it's been handled
     */
    fun clearTabNavigationEvent() {
        _tabNavigationEvent.value = null
    }

    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _error.value = null
    }

    /**
     * Clear status update message
     */
    fun clearStatusUpdateSuccess() {
        _statusUpdateSuccess.value = null
    }

    /**
     * Accept an order - changes status from PLACED to ACCEPTED
     */
    fun acceptOrder(orderId: String) {
        repository.acceptOrder(
            orderId = orderId,
            onSuccess = {
                _statusUpdateSuccess.value = StatusUpdateEvent(
                    message = "Order accepted successfully",
                    showAnimation = false
                )
            },
            onFailure = { errorMessage ->
                _error.value = errorMessage
            }
        )
    }

    /**
     * Reject an order - changes status to REJECTED
     */
    fun rejectOrder(orderId: String) {
        repository.rejectOrder(
            orderId = orderId,
            onSuccess = {
                _statusUpdateSuccess.value = StatusUpdateEvent(
                    message = "Order rejected",
                    showAnimation = false
                )
            },
            onFailure = { errorMessage ->
                _error.value = errorMessage
            }
        )
    }

    /**
     * Mark an order as prepared - changes status to PREPARED
     * Also navigates to the Prepared tab
     */
    fun markOrderPrepared(orderId: String) {
        repository.markOrderPrepared(
            orderId = orderId,
            onSuccess = {
                _statusUpdateSuccess.value = StatusUpdateEvent(
                    message = "Order marked as prepared",
                    showAnimation = true
                )

                // Navigate to prepared tab (index 1)
                _tabNavigationEvent.value = 1
            },
            onFailure = { errorMessage ->
                _error.value = errorMessage
            }
        )
    }

    /**
     * Mark an order as delivered - changes status to DELIVERED
     * Also navigates to the Delivered tab
     */
    fun markOrderDelivered(orderId: String) {
        repository.markOrderDelivered(
            orderId = orderId,
            onSuccess = {
                _statusUpdateSuccess.value = StatusUpdateEvent(
                    message = "Order marked as delivered",
                    showAnimation = true
                )

                // Navigate to delivered tab (index 2)
                _tabNavigationEvent.value = 2
            },
            onFailure = { errorMessage ->
                _error.value = errorMessage
            }
        )
    }

    /**
     * Event class for status updates with animation option
     */
    data class StatusUpdateEvent(
        val message: String,
        val showAnimation: Boolean
    )
}