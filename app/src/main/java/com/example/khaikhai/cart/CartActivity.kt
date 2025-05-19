package com.example.khaikhai.cart

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.khaikhai.cart.OrderManager
import com.example.khaikhai.R
import com.example.khaikhai.checkout.CheckoutActivity
import com.google.android.material.snackbar.Snackbar
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener

class CartActivity : AppCompatActivity(), PaymentResultWithDataListener {

    private lateinit var cartManager: CartManager
    private lateinit var orderManager: OrderManager
    private lateinit var adapter: CartAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyCartView: TextView
    private lateinit var emptyCartImageView: ImageView
    private lateinit var restaurantNameTextView: TextView
    private lateinit var totalPriceTextView: TextView
    private lateinit var totalItemsTextView: TextView
    private lateinit var checkoutButton: Button
    private lateinit var clearCartButton: Button
    private lateinit var orderSummaryCard: CardView
    private lateinit var checkoutLoadingView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        // Initialize Razorpay Checkout
        Checkout.preload(applicationContext)

        // Set up toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Your Cart"

        // Initialize views
        recyclerView = findViewById(R.id.cartRecyclerView)
        emptyCartView = findViewById(R.id.emptyCartView)
        emptyCartImageView = findViewById(R.id.emptyCartImageView)
        restaurantNameTextView = findViewById(R.id.restaurantNameTextView)
        totalPriceTextView = findViewById(R.id.totalPriceTextView)
        totalItemsTextView = findViewById(R.id.totalItemsTextView)
        checkoutButton = findViewById(R.id.checkoutButton)
        clearCartButton = findViewById(R.id.clearCartButton)
        orderSummaryCard = findViewById(R.id.orderSummaryCard)
        checkoutLoadingView = findViewById(R.id.checkoutLoadingView)

        // Initialize CartManager and OrderManager
        cartManager = CartManager(this)
        orderManager = OrderManager(this)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CartAdapter(
            cartItems = cartManager.getCartItems(),
            onQuantityChanged = { menuItemId, quantity ->
                cartManager.updateItemQuantity(menuItemId, quantity)
                updateUI()
                // Add animation
                val animation = AnimationUtils.loadAnimation(this, R.anim.quantity_update)
                totalPriceTextView.startAnimation(animation)
            },
            onItemRemoved = { menuItemId ->
                val removedItem = cartManager.getCartItems().find { it.menuItem.itemId == menuItemId }
                cartManager.removeItem(menuItemId)
                updateUI()

                // Show undo snackbar
                removedItem?.let {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "${it.menuItem.name} removed",
                        Snackbar.LENGTH_LONG
                    ).setAction("UNDO") {
                        removedItem.menuItem.restaurantId?.let { it1 ->
                            cartManager.addItem(removedItem.menuItem,
                                it1, cartManager.getRestaurantName() ?: "")
                        }
                        updateUI()
                    }.show()
                }
            }
        )
        recyclerView.adapter = adapter

        // Set up swipe to delete
        setupSwipeToDelete()

        // Set up clear cart button
        clearCartButton.setOnClickListener {
            showClearCartConfirmationDialog()
        }

        // Set up checkout button to launch CheckoutActivity
        checkoutButton.setOnClickListener {
            // Show loading animation
            checkoutButton.isEnabled = false
            checkoutLoadingView.visibility = View.VISIBLE

            // Simulate processing before navigating (just for visual feedback)
            recyclerView.postDelayed({
                checkoutLoadingView.visibility = View.GONE
                checkoutButton.isEnabled = true

                // Calculate cart details
                val subtotal = cartManager.getCartTotal()
                val itemCount = cartManager.getCartItemCount()
                val restaurantId = cartManager.getRestaurantId() ?: ""
                val restaurantName = cartManager.getRestaurantName() ?: ""
                // Launch the CheckoutActivity with cart details
                val intent = Intent(this, CheckoutActivity::class.java).apply {
                    putExtra("EXTRA_RESTAURANT_ID", restaurantId)
                    putExtra("EXTRA_RESTAURANT_NAME", restaurantName)
                    putExtra("EXTRA_SUBTOTAL", subtotal)
                    putExtra("EXTRA_ITEM_COUNT", itemCount)
                }
                startActivity(intent)
            }, 500) // Reduced delay for better UX
        }

        // Apply item animation to RecyclerView
        val controller = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down)
        recyclerView.layoutAnimation = controller

        // Update UI
        updateUI()
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val cartItems = cartManager.getCartItems()
                if (position >= 0 && position < cartItems.size) {
                    val removedItem = cartItems[position]
                    cartManager.removeItem(removedItem.menuItem.itemId!!)
                    updateUI()

                    // Show undo snackbar
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "${removedItem.menuItem.name} removed",
                        Snackbar.LENGTH_LONG
                    ).setAction("UNDO") {
                        cartManager.addItem(removedItem.menuItem, removedItem.menuItem.restaurantId!!, cartManager.getRestaurantName() ?: "")
                        updateUI()
                    }.show()
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun updateUI() {
        val cartItems = cartManager.getCartItems()

        if (cartItems.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyCartView.visibility = View.VISIBLE
            emptyCartImageView.visibility = View.VISIBLE
            restaurantNameTextView.visibility = View.GONE
            orderSummaryCard.visibility = View.GONE
            checkoutButton.visibility = View.GONE
            clearCartButton.visibility = View.GONE

            // Apply animation to empty cart image
            val animation = AnimationUtils.loadAnimation(this, R.anim.pulse)
            emptyCartImageView.startAnimation(animation)
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyCartView.visibility = View.GONE
            emptyCartImageView.visibility = View.GONE
            restaurantNameTextView.visibility = View.VISIBLE
            orderSummaryCard.visibility = View.VISIBLE
            checkoutButton.visibility = View.VISIBLE
            clearCartButton.visibility = View.VISIBLE

            // Update restaurant name
            restaurantNameTextView.text = cartManager.getRestaurantName()

            // Update total items count
            val itemCount = cartManager.getCartItemCount()
            totalItemsTextView.text = "$itemCount ${if (itemCount == 1) "item" else "items"}"

            // Update total price
            totalPriceTextView.text = "₹${String.format("%.2f", cartManager.getCartTotal())}"

            // Enable or disable checkout button based on total
            val isCheckoutEnabled = cartManager.getCartTotal() > 0
            checkoutButton.isEnabled = isCheckoutEnabled

            if (isCheckoutEnabled) {
                checkoutButton.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
            } else {
                checkoutButton.setBackgroundColor(ContextCompat.getColor(this, R.color.status_default))
            }

            // Update adapter
            adapter.updateCartItems(cartItems)

            // Run layout animation if it's the first update
            if (adapter.itemCount != cartItems.size) {
                recyclerView.scheduleLayoutAnimation()
            }
        }
    }

    private fun showClearCartConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Cart")
            .setMessage("Are you sure you want to clear your cart?")
            .setPositiveButton("Yes") { _, _ ->
                cartManager.clearCart()
                updateUI()

                // Show confirmation
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Your cart has been cleared",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    // These methods are still needed for PaymentResultWithDataListener
    override fun onPaymentSuccess(razorpayPaymentID: String?, paymentData: PaymentData?) {
        // This will be called if the payment is processed from this activity
        // For completeness, we'll keep this method even though payment will be handled in CheckoutActivity
        val restaurantId = cartManager.getRestaurantId() ?: return
        val restaurantName = cartManager.getRestaurantName() ?: return
        val subtotal = cartManager.getCartTotal()
        val deliveryFee = 40.0
        val tax = subtotal * 0.05
        val grandTotal = subtotal + deliveryFee + tax

        // Save order to Firebase with online payment details
        orderManager.saveOrder(
            cartItems = cartManager.getCartItems(),
            restaurantId = restaurantId,
            restaurantName = restaurantName,
            subtotal = subtotal,
            deliveryFee = deliveryFee,
            tax = tax,
            total = grandTotal,
            paymentType = "ONLINE",
            paymentId = razorpayPaymentID,
            onSuccess = { orderId ->
                // Clear cart BEFORE showing the dialog
                cartManager.clearCart()
                updateUI()

                // Now show success dialog with payment ID and order ID
                AlertDialog.Builder(this)
                    .setTitle("Payment Successful!")
                    .setMessage("Your payment was successful and order has been placed.\nPayment ID: $razorpayPaymentID\nOrder ID: $orderId\nThank you for ordering with Petuk!")
                    .setPositiveButton("OK") { _, _ ->
                        // The cart is already cleared, but we'll update the UI again to be safe
                        updateUI()
                    }
                    .setCancelable(false)
                    .show()
            },
            onFailure = { exception ->
                // Show error message
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Failed to save order: ${exception.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        )
    }

    override fun onPaymentError(errorCode: Int, errorDescription: String?, paymentData: PaymentData?) {
        // This will be called if the payment fails from this activity
        Snackbar.make(
            findViewById(android.R.id.content),
            "Payment Failed: $errorDescription",
            Snackbar.LENGTH_LONG
        ).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}