package com.example.khaikhai.checkout

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.khaikhai.Address
import com.example.khaikhai.AddressActivity
import com.example.khaikhai.R
import com.example.khaikhai.cart.CartManager
import com.example.khaikhai.cart.OrderManager
import com.example.khaikhai.ui.menu.MenuActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject

class CheckoutActivity : AppCompatActivity(), PaymentResultWithDataListener {

    private lateinit var cartManager: CartManager
    private lateinit var orderManager: OrderManager

    // UI Components
    private lateinit var restaurantNameTextView: TextView
    private lateinit var totalItemsTextView: TextView
    private lateinit var subtotalTextView: TextView
    private lateinit var deliveryFeeTextView: TextView
    private lateinit var taxTextView: TextView
    private lateinit var totalAmountTextView: TextView
    private lateinit var deliveryAddressTextView: TextView
    private lateinit var paymentMethodRadioGroup: RadioGroup
    private lateinit var radioOnlinePayment: RadioButton
    private lateinit var radioCOD: RadioButton
    private lateinit var placeOrderButton: Button
    private lateinit var paymentProgressBar: ProgressBar
    private lateinit var couponEditText: EditText
    private lateinit var applyCouponButton: Button
    private lateinit var couponResultTextView: TextView
    private lateinit var discountTextView: TextView
    private lateinit var discountContainer: View
    private lateinit var viewItemsButton: Button
    private lateinit var changeAddressButton: Button

    // Payment related variables
    private var restaurantId: String = ""
    private var restaurantName: String = ""
    private var subtotal: Double = 0.0
    private var deliveryFee: Double = 40.0  // Default delivery fee
    private var tax: Double = 0.0
    private var discount: Double = 0.0
    private var grandTotal: Double = 0.0
    private var itemCount: Int = 0
    private var selectedPaymentMethod: String = "ONLINE"

    // Current selected address
    private var currentDeliveryAddress: Address? = null
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        // Set up toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Checkout"

        // Initialize CartManager and OrderManager
        cartManager = CartManager(this)
        orderManager = OrderManager(this)

        // Initialize Razorpay
        Checkout.preload(applicationContext)

        // Get data from intent
        restaurantId = intent.getStringExtra("EXTRA_RESTAURANT_ID") ?: ""
        restaurantName = intent.getStringExtra("EXTRA_RESTAURANT_NAME") ?: ""
        subtotal = intent.getDoubleExtra("EXTRA_SUBTOTAL", 0.0)
        itemCount = intent.getIntExtra("EXTRA_ITEM_COUNT", 0)

        // Calculate tax and total
        tax = subtotal * 0.05
        grandTotal = subtotal + deliveryFee + tax

        // Initialize views
        initializeViews()

        // Set initial values
        updatePriceDetails()

        // Set up button click listeners
        setupClickListeners()

        // Fetch user's default address
        fetchUserDefaultAddress()
    }

    private fun initializeViews() {
        // Find all views by ID
        restaurantNameTextView = findViewById(R.id.restaurantNameTextView)
        totalItemsTextView = findViewById(R.id.totalItemsTextView)
        subtotalTextView = findViewById(R.id.subtotalTextView)
        deliveryFeeTextView = findViewById(R.id.deliveryFeeTextView)
        taxTextView = findViewById(R.id.taxTextView)
        totalAmountTextView = findViewById(R.id.totalAmountTextView)
        deliveryAddressTextView = findViewById(R.id.deliveryAddressTextView)
        paymentMethodRadioGroup = findViewById(R.id.paymentMethodRadioGroup)
        radioOnlinePayment = findViewById(R.id.radioOnlinePayment)
        radioCOD = findViewById(R.id.radioCOD)
        placeOrderButton = findViewById(R.id.placeOrderButton)
        paymentProgressBar = findViewById(R.id.paymentProgressBar)
        couponEditText = findViewById(R.id.couponEditText)
        applyCouponButton = findViewById(R.id.applyCouponButton)
        couponResultTextView = findViewById(R.id.couponResultTextView)
        discountTextView = findViewById(R.id.discountTextView)
        discountContainer = findViewById(R.id.discountContainer)
        viewItemsButton = findViewById(R.id.viewItemsButton)
        changeAddressButton = findViewById(R.id.changeAddressButton)

        // Set initial values
        restaurantNameTextView.text = restaurantName
        totalItemsTextView.text = "$itemCount ${if (itemCount == 1) "item" else "items"}"

        // Set a placeholder until we fetch the actual address
        deliveryAddressTextView.text = "Loading address..."
    }

    private fun setupClickListeners() {
        // Payment method selection
        paymentMethodRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedPaymentMethod = when (checkedId) {
                R.id.radioOnlinePayment -> "ONLINE"
                R.id.radioCOD -> "COD"
                else -> "ONLINE"
            }
        }

        // Apply coupon button
        applyCouponButton.setOnClickListener {
            applyCoupon()
        }

        // View items button
        viewItemsButton.setOnClickListener {
            showCartItemsDialog()
        }

        // Change address button
        changeAddressButton.setOnClickListener {
            // Launch the address management activity
            val intent = Intent(this, AddressActivity::class.java)
            startActivity(intent)

            // We'll refresh the address when we come back to this activity
        }

        // Place order button
        placeOrderButton.setOnClickListener {
            if (currentDeliveryAddress == null) {
                Toast.makeText(this, "Please select a delivery address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            placeOrder()
        }
    }

    private fun fetchUserDefaultAddress() {
        // Show loading state
        deliveryAddressTextView.text = "Loading address..."
        placeOrderButton.isEnabled = false

        // Get current user email from SharedPreferences
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("user_email", null)

        if (userEmail == null) {
            deliveryAddressTextView.text = "No user logged in"
            return
        }

        // Reference to Firebase database
        val databaseReference = FirebaseDatabase.getInstance().getReference("customers")

        // Find user by email
        databaseReference.orderByChild("email").equalTo(userEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        deliveryAddressTextView.text = "No user account found"
                        return
                    }

                    // Get the user ID
                    for (userSnapshot in snapshot.children) {
                        currentUserId = userSnapshot.key

                        // Get the addresses
                        val addressesList = mutableListOf<Address>()
                        userSnapshot.child("addresses").children.forEach { addressSnapshot ->
                            addressSnapshot.getValue(Address::class.java)?.let {
                                addressesList.add(it)
                            }
                        }

                        // Find the default address
                        currentDeliveryAddress = addressesList.find { it.isDefault }

                        // If no default is set but there are addresses, use the first one
                        if (currentDeliveryAddress == null && addressesList.isNotEmpty()) {
                            currentDeliveryAddress = addressesList[0]
                        }

                        // Update UI
                        if (currentDeliveryAddress != null) {
                            deliveryAddressTextView.text = formatAddress(currentDeliveryAddress!!)
                            placeOrderButton.isEnabled = true
                        } else {
                            deliveryAddressTextView.text = "No delivery address found. Please add an address."
                        }

                        break
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    deliveryAddressTextView.text = "Error loading address: ${error.message}"
                }
            })
    }

    private fun formatAddress(address: Address): String {
        return "${address.label}: ${address.street}, ${address.city}, ${address.state} ${address.zipCode}, ${address.country}"
    }

    override fun onResume() {
        super.onResume()
        // Refresh the address when returning to this activity
        fetchUserDefaultAddress()
    }

    private fun updatePriceDetails() {
        // Set price details
        subtotalTextView.text = "₹${String.format("%.2f", subtotal)}"
        deliveryFeeTextView.text = "₹${String.format("%.2f", deliveryFee)}"
        taxTextView.text = "₹${String.format("%.2f", tax)}"

        // Update discount if applicable
        if (discount > 0) {
            discountContainer.visibility = View.VISIBLE
            discountTextView.text = "-₹${String.format("%.2f", discount)}"
        } else {
            discountContainer.visibility = View.GONE
        }

        // Calculate and update grand total
        grandTotal = subtotal + deliveryFee + tax - discount
        totalAmountTextView.text = "₹${String.format("%.2f", grandTotal)}"
    }

    private fun applyCoupon() {
        val couponCode = couponEditText.text.toString().trim().uppercase()

        if (couponCode.isEmpty()) {
            Snackbar.make(
                findViewById(android.R.id.content),
                "Please enter a coupon code",
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        // Show loading
        applyCouponButton.isEnabled = false
        applyCouponButton.text = "Applying..."

        // Simulate coupon validation (in a real app, this would check against a database)
        findViewById<View>(android.R.id.content).postDelayed({
            applyCouponButton.isEnabled = true
            applyCouponButton.text = "Apply"

            // Demo coupon codes for testing
            when (couponCode) {
                "WELCOME10" -> {
                    // 10% discount
                    discount = subtotal * 0.1
                    updatePriceDetails()
                    couponResultTextView.text = "10% discount applied!"
                    couponResultTextView.setTextColor(getColor(R.color.cashback_green))
                    couponResultTextView.visibility = View.VISIBLE
                }
                "FLAT50" -> {
                    // Flat ₹50 off
                    discount = 50.0
                    updatePriceDetails()
                    couponResultTextView.text = "₹50 discount applied!"
                    couponResultTextView.setTextColor(getColor(R.color.cashback_green))
                    couponResultTextView.visibility = View.VISIBLE
                }
                "FREEDEL" -> {
                    // Free delivery
                    discount = deliveryFee
                    updatePriceDetails()
                    couponResultTextView.text = "Free delivery applied!"
                    couponResultTextView.setTextColor(getColor(R.color.cashback_green))
                    couponResultTextView.visibility = View.VISIBLE
                }
                else -> {
                    // Invalid coupon
                    discount = 0.0
                    updatePriceDetails()
                    couponResultTextView.text = "Invalid coupon code"
                    couponResultTextView.setTextColor(getColor(R.color.error_color))
                    couponResultTextView.visibility = View.VISIBLE
                }
            }
        }, 1000)
    }

    private fun showCartItemsDialog() {
        val cartItems = cartManager.getCartItems()
        val itemsList = StringBuilder()

        for (item in cartItems) {
            itemsList.append("${item.quantity}x ${item.menuItem.name}\n")
            itemsList.append("   ₹${String.format("%.2f", item.menuItem.price)} each\n")
            itemsList.append("   Total: ₹${String.format("%.2f", item.getTotalPrice())}\n\n")
        }

        AlertDialog.Builder(this)
            .setTitle("Cart Items")
            .setMessage(itemsList.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun placeOrder() {
        // Show loading
        placeOrderButton.visibility = View.INVISIBLE
        paymentProgressBar.visibility = View.VISIBLE

        when (selectedPaymentMethod) {
            "ONLINE" -> startOnlinePayment()
            "COD" -> processCodOrder()
        }
    }

    private fun startOnlinePayment() {
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_NhfzLjYqDQdROT")

        try {
            // Convert to subunits (INR → paise)
            val amount = (grandTotal * 100).toInt().toString()

            val options = JSONObject().apply {
                put("name", "PETUK")
                put("description", "Order from $restaurantName")
                put("image", "http://example.com/image/rzp.jpg")
                put("theme.color", "#FF1515")
                put("currency", "INR")
                put("amount", amount)
                put("prefill.contact", "9876543210") // Optional
                put("prefill.email", "customer@example.com") // Optional
            }

            checkout.open(this, options)
        } catch (e: Exception) {
            // Hide loading
            placeOrderButton.visibility = View.VISIBLE
            paymentProgressBar.visibility = View.GONE

            // Navigate to status screen with failure
            launchStatusActivity(false, errorMessage = e.message ?: "Payment initialization failed")
        }
    }

    private fun processCodOrder() {
        // Save order to Firebase with COD payment type
        orderManager.saveOrder(
            cartItems = cartManager.getCartItems(),
            restaurantId = restaurantId,
            restaurantName = restaurantName,
            subtotal = subtotal,
            deliveryFee = deliveryFee,
            tax = tax,
            total = grandTotal,
            paymentType = "COD",
            // Remove the deliveryAddress parameter from here as it's not in the method signature
            onSuccess = { orderId ->
                // Hide loading
                placeOrderButton.visibility = View.VISIBLE
                paymentProgressBar.visibility = View.GONE

                // Navigate to status screen with success
                cartManager.clearCart()
                launchStatusActivity(true, orderId = orderId)
            },
            onFailure = { exception ->
                // Hide loading
                placeOrderButton.visibility = View.VISIBLE
                paymentProgressBar.visibility = View.GONE

                // Navigate to status screen with failure
                launchStatusActivity(false, errorMessage = exception.message ?: "Failed to place order")
            }
        )
    }

    private fun launchStatusActivity(
        isSuccess: Boolean,
        orderId: String = "",
        paymentId: String = "",
        errorMessage: String = ""
    ) {
        val intent = Intent(this, StatusActivity::class.java).apply {
            putExtra("IS_SUCCESS", isSuccess)
            putExtra("ORDER_ID", orderId)
            putExtra("PAYMENT_ID", paymentId)
            putExtra("RESTAURANT_NAME", restaurantName)
            putExtra("ERROR_MESSAGE", errorMessage)
        }
        startActivity(intent)
        if (isSuccess) {
            finish() // Close checkout activity if order was successful
        }
    }

    override fun onPaymentSuccess(razorpayPaymentID: String?, paymentData: PaymentData?) {
        // Handle successful payment
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
            // Remove the deliveryAddress parameter from here as it's not in the method signature
            onSuccess = { orderId ->
                // Hide loading
                placeOrderButton.visibility = View.VISIBLE
                paymentProgressBar.visibility = View.GONE

                // Navigate to status screen with success
                cartManager.clearCart()
                launchStatusActivity(true, orderId = orderId, paymentId = razorpayPaymentID ?: "")
            },
            onFailure = { exception ->
                // Hide loading
                placeOrderButton.visibility = View.VISIBLE
                paymentProgressBar.visibility = View.GONE

                // Navigate to status screen with failure
                launchStatusActivity(false, errorMessage = exception.message ?: "Failed to save order")
            }
        )
    }


    override fun onPaymentError(errorCode: Int, errorDescription: String?, paymentData: PaymentData?) {
        // Hide loading
        placeOrderButton.visibility = View.VISIBLE
        paymentProgressBar.visibility = View.GONE

        // Navigate to status screen with failure
        launchStatusActivity(false, errorMessage = errorDescription ?: "Payment failed")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}