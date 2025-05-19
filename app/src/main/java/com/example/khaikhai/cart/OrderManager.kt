package com.example.khaikhai.cart

import android.content.Context
import android.content.SharedPreferences
import com.example.khaikhai.Customer
import com.example.khaikhai.ui.wallet.WalletManager
import com.google.firebase.database.*
import java.util.Date

class OrderManager(private val context: Context) {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val ordersRef: DatabaseReference = database.getReference("orders")
    private val customersRef: DatabaseReference = database.getReference("customers")
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val walletManager = WalletManager(context)

    // Get customer ID and name from Firebase using email
    private fun getCustomerInfo(userEmail: String, callback: (customerId: String, customerName: String) -> Unit) {
        // Query the database for the user by their email
        customersRef.orderByChild("email").equalTo(userEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Get the first match from the snapshot
                        val customerSnapshot = snapshot.children.first()
                        val customerId = customerSnapshot.key ?: "guest_user"
                        val customer = customerSnapshot.getValue(Customer::class.java)

                        if (customer != null) {
                            // If customer data is found, pass it to the callback
                            callback(customerId, customer.fullName ?: "Unknown User")
                        } else {
                            callback("guest_user", "Unknown User")
                        }
                    } else {
                        // If no matching user is found, return a guest user
                        callback("guest_user", "Unknown User")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // In case of a database error, return a guest user
                    callback("guest_user", "Unknown User")
                }
            })
    }

    // Save order to Firebase
    fun saveOrder(
        cartItems: List<CartItem>,
        restaurantId: String,
        restaurantName: String,
        subtotal: Double,
        deliveryFee: Double,
        tax: Double,
        total: Double,
        paymentType: String,
        paymentId: String? = null,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            // Generate a unique order ID
            val orderId = ordersRef.push().key ?: return

            // Get customer email from SharedPreferences
            val userEmail = sharedPreferences.getString("user_email", "") ?: ""

            // Get customer ID and name from Firebase using the email
            getCustomerInfo(userEmail) { customerId, customerName ->
                // Map cart items to order items and filter out null items
                val orderItems = cartItems.mapNotNull { cartItem ->
                    cartItem.menuItem.itemId?.let {
                        OrderItem(
                            itemId = it,
                            name = cartItem.menuItem.name,
                            price = cartItem.menuItem.price,
                            quantity = cartItem.quantity,
                            totalPrice = cartItem.getTotalPrice()
                        )
                    }
                }

                // Create order object
                val order = Order(
                    orderId = orderId,
                    customerId = customerId,
                    customerName = customerName,
                    restaurantId = restaurantId,
                    restaurantName = restaurantName,
                    items = orderItems,
                    subtotal = subtotal,
                    deliveryFee = deliveryFee,
                    tax = tax,
                    total = total,
                    paymentType = paymentType,
                    paymentId = paymentId,
                    orderStatus = "PLACED",
                    orderDateTime = Date().time,
                    deliveryAddress = getDeliveryAddress() // Get address from SharedPreferences
                )

                // Save order to Firebase
                ordersRef.child(orderId).setValue(order)
                    .addOnSuccessListener {
                        // Order saved successfully
                        onSuccess(orderId)

                        // Setup a listener for order status changes
                        setupOrderStatusListener(orderId, customerId, restaurantName, total)
                    }
                    .addOnFailureListener { e ->
                        // Failed to save order
                        onFailure(e)
                    }
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Setup listener to track order status changes
    private fun setupOrderStatusListener(
        orderId: String,
        customerId: String,
        restaurantName: String,
        total: Double
    ) {
        val orderStatusRef = ordersRef.child(orderId).child("orderStatus")

        orderStatusRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)

                // If status is not null and not PLACED and not REJECTED, add cashback
                if (status != null && status != "PLACED" && status != "REJECTED") {
                    // Check if cashback has already been added for this order
                    val cashbackAddedRef = ordersRef.child(orderId).child("cashbackAdded")

                    cashbackAddedRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(cashbackSnapshot: DataSnapshot) {
                            val cashbackAdded = cashbackSnapshot.getValue(Boolean::class.java) ?: false

                            // Only add cashback if it hasn't been added already
                            if (!cashbackAdded) {
                                // Add 5% cashback to user's wallet
                                walletManager.addCashback(
                                    customerId = customerId,
                                    orderId = orderId,
                                    restaurantName = restaurantName,
                                    orderAmount = total,
                                    onSuccess = {
                                        // Mark that cashback has been added for this order
                                        cashbackAddedRef.setValue(true)
                                    },
                                    onFailure = { e ->
                                        // Log error but don't notify user
                                        e.printStackTrace()
                                    }
                                )
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Log error but don't notify user
                            error.toException().printStackTrace()
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Log error but don't notify user
                error.toException().printStackTrace()
            }
        })
    }

    // Helper method to get delivery address from SharedPreferences
    private fun getDeliveryAddress(): String {
        return sharedPreferences.getString("delivery_address", "Default Address") ?: "Default Address"
    }

    // Get all orders for current user
    fun getUserOrders(
        onSuccess: (List<Order>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Get customer email from SharedPreferences
        val userEmail = sharedPreferences.getString("user_email", "") ?: ""

        getCustomerInfo(userEmail) { customerId, _ ->
            ordersRef.orderByChild("customerId").equalTo(customerId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val ordersList = mutableListOf<Order>()

                        for (orderSnapshot in snapshot.children) {
                            val order = orderSnapshot.getValue(Order::class.java)
                            order?.let { ordersList.add(it) }
                        }

                        onSuccess(ordersList)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        onFailure(Exception(error.message))
                    }
                })
        }
    }

    // Get specific order by ID
    fun getOrderById(
        orderId: String,
        onSuccess: (Order?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        ordersRef.child(orderId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val order = snapshot.getValue(Order::class.java)
                    onSuccess(order)
                }

                override fun onCancelled(error: DatabaseError) {
                    onFailure(Exception(error.message))
                }
            })
    }

    // Update order status (for restaurant or admin use)
    fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        ordersRef.child(orderId).child("orderStatus").setValue(newStatus)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}