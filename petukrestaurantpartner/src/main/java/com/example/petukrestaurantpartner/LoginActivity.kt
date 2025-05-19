package com.example.petukrestaurantpartner

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.petukrestaurantpartner.databinding.ActivityLoginBinding
import com.google.firebase.database.*
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase reference
        databaseReference = FirebaseDatabase.getInstance().getReference("restaurants")

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)

        // Check for existing session
        val isLoggedIn = sharedPreferences.getBoolean("IS_LOGGED_IN", false)
        val loginTime = sharedPreferences.getLong("LOGIN_TIME", 0)
        val restaurantId = sharedPreferences.getString("RESTAURANT_ID", null)

        val THIRTY_DAYS_MILLIS = 30L * 24 * 60 * 60 * 1000 // 30 days in millis

        if (isLoggedIn && System.currentTimeMillis() - loginTime < THIRTY_DAYS_MILLIS) {
            // Auto-login if session is still valid
            startActivity(Intent(this, MainActivity::class.java).apply {
                putExtra("RESTAURANT_ID", restaurantId)
            })
            finish()
            return
        }

        binding.textViewRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()

            when {
                email.isEmpty() -> showToast("Email required")
                !isValidEmail(email) -> showToast("Please enter a valid email address")
                password.isEmpty() -> showToast("Password required")
                else -> loginUser(email, password)
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun loginUser(email: String, password: String) {
        // Show progress bar
        binding.progressBar.visibility = View.VISIBLE

        // Hash the password
        val hashedPassword = hashPassword(password)

        databaseReference.orderByChild("restaurantEmail").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.progressBar.visibility = View.GONE

                    if (!snapshot.exists()) {
                        showToast("Account not found")
                        return
                    }

                    for (restaurantSnapshot in snapshot.children) {
                        val restaurant = restaurantSnapshot.getValue(Restaurant::class.java)
                            ?: continue

                        if (restaurant.restaurantPassword == hashedPassword) {
                            // Save session info
                            val editor = sharedPreferences.edit()
                            editor.putString("RESTAURANT_ID", restaurant.id)
                            editor.putLong("LOGIN_TIME", System.currentTimeMillis())
                            editor.putBoolean("IS_LOGGED_IN", true)
                            editor.apply()

                            startActivity(Intent(this@LoginActivity, MainActivity::class.java).apply {
                                putExtra("RESTAURANT_ID", restaurant.id)
                            })
                            finish()
                            return
                        }
                    }
                    showToast("Invalid password")
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar.visibility = View.GONE
                    showToast("Database error: ${error.message}")
                }
            })
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
