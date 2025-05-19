package com.example.petukrestaurantpartner

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.petukrestaurantpartner.databinding.ActivityRegisterBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.security.MessageDigest

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference.child("restaurants")

        binding.buttonRegister.setOnClickListener {
            val restaurantName = binding.editTextRestaurantName.text.toString().trim()
            val restaurantAddress = binding.editTextAddress.text.toString().trim()
            val restaurantPhone = binding.editTextPhone.text.toString().trim()
            val restaurantEmail = binding.editTextEmail.text.toString().trim()
            val restaurantPassword = binding.editTextPassword.text.toString().trim()
            val confirmPassword = binding.editTextConfirmPassword.text.toString().trim()
            val cuisine = binding.editTextCuisineType.text.toString().trim()
            val deliveryTimeStr = binding.editTextDeliveryTime.text.toString().trim()

            when {
                restaurantName.isEmpty() -> showToast("Restaurant name required")
                restaurantAddress.isEmpty() -> showToast("Address required")
                restaurantPhone.isEmpty() -> showToast("Phone number required")
                !isValidPhone(restaurantPhone) -> showToast("Please enter a valid phone number")
                restaurantEmail.isEmpty() -> showToast("Email required")
                !isValidEmail(restaurantEmail) -> showToast("Please enter a valid email address")
                restaurantPassword.isEmpty() -> showToast("Password required")
                !isPasswordStrong(restaurantPassword) -> showToast("Password must be at least 8 characters with letters and numbers")
                confirmPassword.isEmpty() -> showToast("Please confirm your password")
                restaurantPassword != confirmPassword -> showToast("Passwords do not match")
                cuisine.isEmpty() -> showToast("Cuisine type required")
                deliveryTimeStr.isEmpty() -> showToast("Average delivery time required")
                !isValidDeliveryTime(deliveryTimeStr) -> showToast("Please enter a valid delivery time (5-120 minutes)")
                else -> {
                    val deliveryTime = deliveryTimeStr.toInt()
                    registerRestaurant(restaurantName, restaurantAddress, restaurantPhone, restaurantEmail, restaurantPassword,cuisine,deliveryTime )}
            }
        }

        binding.textViewLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPhone(phone: String): Boolean {
        return phone.length >= 10 && phone.all { it.isDigit() }
    }

    private fun isPasswordStrong(password: String): Boolean {
        // Password must be at least 8 characters and contain both letters and numbers
        return password.length >= 8 &&
                password.any { it.isLetter() } &&
                password.any { it.isDigit() }
    }
    private fun isValidDeliveryTime(deliveryTime: String): Boolean {
        return try {
            val time = deliveryTime.toInt()
            // Delivery time should be between 5 and 120 minutes
            time in 5..120
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun registerRestaurant(restaurantName: String, restaurantAddress: String, restaurantPhone: String,
                                   restaurantEmail: String, restaurantPassword: String,cuisine:String , averageDeliveryTime: Int) {
        // Show progress bar
        binding.progressBar.visibility = View.VISIBLE

        // Hash the password before storing
        val hashedPassword = hashPassword(restaurantPassword)

        // First check if email already exists
        databaseReference.orderByChild("restaurantEmail").equalTo(restaurantEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        binding.progressBar.visibility = View.GONE
                        showToast("Email already registered")
                        return
                    }

                    // Then check if restaurant name exists
                    databaseReference.orderByChild("restaurantName").equalTo(restaurantName)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                binding.progressBar.visibility = View.GONE

                                if (!dataSnapshot.exists()) {
                                    val id = databaseReference.push().key
                                    val restaurant = Restaurant(id, restaurantName, restaurantAddress,
                                        restaurantPhone, restaurantEmail, hashedPassword,cuisine,averageDeliveryTime)

                                    databaseReference.child(id!!).setValue(restaurant)
                                    showToast("Restaurant registered successfully")
                                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                                    finish()
                                } else {
                                    showToast("Restaurant name already exists")
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                binding.progressBar.visibility = View.GONE
                                showToast("Database error: ${databaseError.message}")
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar.visibility = View.GONE
                    showToast("Database error: ${error.message}")
                }
            })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}