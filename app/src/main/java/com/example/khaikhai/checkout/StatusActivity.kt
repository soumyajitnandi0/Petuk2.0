package com.example.khaikhai.checkout

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.airbnb.lottie.LottieAnimationView
import com.example.khaikhai.R
import com.example.khaikhai.cart.CartActivity
import com.example.khaikhai.ui.home.HomeFragment
import com.example.khaikhai.ui.menu.MenuActivity

class StatusActivity : AppCompatActivity() {

    private lateinit var animationView: LottieAnimationView
    private lateinit var statusTitleTextView: TextView
    private lateinit var statusMessageTextView: TextView
    private lateinit var orderDetailsTextView: TextView
    private lateinit var continueButton: Button
//    private lateinit var detailsCardView: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)

        // Initialize views
        animationView = findViewById(R.id.animationView)
        statusTitleTextView = findViewById(R.id.statusTitleTextView)
        statusMessageTextView = findViewById(R.id.statusMessageTextView)
        orderDetailsTextView = findViewById(R.id.orderDetailsTextView)
        continueButton = findViewById(R.id.continueButton)

        // Get data from intent
        val isSuccess = intent.getBooleanExtra("IS_SUCCESS", false)
        val orderId = intent.getStringExtra("ORDER_ID") ?: ""
        val paymentId = intent.getStringExtra("PAYMENT_ID") ?: ""
        val restaurantName = intent.getStringExtra("RESTAURANT_NAME") ?: ""
        val errorMessage = intent.getStringExtra("ERROR_MESSAGE") ?: ""

        // Set up UI based on success or failure
        if (isSuccess) {
            setupSuccessUI(orderId, paymentId, restaurantName)
        } else {
            setupFailureUI(errorMessage)
        }

        // Set up button click listener
        continueButton.setOnClickListener {
            if (isSuccess) {
                // Navigate to home or orders screen
                val intent = Intent(this, CartActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            } else {
                // Go back to checkout to try again
                finish()
            }
        }
    }

    private fun setupSuccessUI(orderId: String, paymentId: String, restaurantName: String) {
        // Set success animation
        animationView.setAnimation(R.raw.success)
        animationView.playAnimation()

        // Set texts
        statusTitleTextView.text = "Order Placed Successfully!"
        statusMessageTextView.text = "Thank you for ordering with Petuk!"
//        detailsCardView.setBackgroundColor(Color.GREEN)

        // Build order details text
        val orderDetails = StringBuilder()
        orderDetails.append("Order ID: $orderId\n")
        if (paymentId.isNotEmpty()) {
            orderDetails.append("Payment ID: $paymentId\n")
        }
        orderDetails.append("Restaurant: $restaurantName\n")
        orderDetails.append("\nYou will receive a notification when your order is ready for delivery.")

        orderDetailsTextView.text = orderDetails.toString()

        // Set button text
        continueButton.text = "Continue Shopping"
    }

    private fun setupFailureUI(errorMessage: String) {
        // Set failure animation
        animationView.setAnimation(R.raw.failure)
        animationView.playAnimation()

        // Set texts
        statusTitleTextView.text = "Order Failed"
        statusMessageTextView.text = "We couldn't process your order at this time."
//        detailsCardView.setBackgroundColor(Color.RED)

        // Set error details
        val errorDetails = if (errorMessage.isNotEmpty()) {
            "Error: $errorMessage\n\nPlease try again or contact support if the issue persists."
        } else {
            "Something went wrong with your order. Please try again or contact support if the issue persists."
        }

        orderDetailsTextView.text = errorDetails

        // Set button text
        continueButton.text = "Try Again"
    }
}