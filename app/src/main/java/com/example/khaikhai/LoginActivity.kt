package com.example.khaikhai

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.khaikhai.databinding.ActivityLoginBinding
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
        databaseReference = FirebaseDatabase.getInstance().getReference("customers")

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)

        // Check for existing session
        val isLoggedIn = sharedPreferences.getBoolean("IS_LOGGED_IN", false)
        val loginTime = sharedPreferences.getLong("LOGIN_TIME", 0)
        val customerEmail = sharedPreferences.getString("CUSTOMER_EMAIL", null)

        val THIRTY_DAYS_MILLIS = 30L * 24 * 60 * 60 * 1000 // 30 days in millis

        if (isLoggedIn && System.currentTimeMillis() - loginTime < THIRTY_DAYS_MILLIS && customerEmail != null) {
            // Auto-login if session is still valid
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Set up click listeners
        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            // Implement forgot password functionality if needed
            showToast("Forgot password functionality will be implemented soon")
        }
    }

    private fun loginUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Input validation
        when {
            email.isEmpty() -> {
                binding.etEmail.error = "Email is required"
                binding.etEmail.requestFocus()
                return
            }
            !isValidEmail(email) -> {
                binding.etEmail.error = "Please enter a valid email"
                binding.etEmail.requestFocus()
                return
            }
            password.isEmpty() -> {
                binding.etPassword.error = "Password is required"
                binding.etPassword.requestFocus()
                return
            }
            else -> {
                // Show progress (add a progress bar to the layout if not already there)
                binding.progressBar.visibility = View.VISIBLE

                // Authenticate with Firebase Realtime Database
                authenticateUser(email, password)
            }
        }
    }

    private fun authenticateUser(email: String, password: String) {
        // Hash the password
        val hashedPassword = hashPassword(password)

        databaseReference.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.progressBar.visibility = View.GONE

                    if (!snapshot.exists()) {
                        showToast("Account not found")
                        return
                    }

                    for (customerSnapshot in snapshot.children) {
                        val customer = customerSnapshot.getValue(Customer::class.java)
                            ?: continue

                        if (customer.password == hashedPassword) {
                            // Save session info
                            val editor = sharedPreferences.edit()
                            editor.putString("CUSTOMER_EMAIL", email)
                            editor.putLong("LOGIN_TIME", System.currentTimeMillis())
                            editor.putBoolean("IS_LOGGED_IN", true)
                            editor.apply()

                            // Also save to user_prefs SharedPreferences for other components
                            val userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                            userPrefs.edit().putString("user_email", email).apply()

                            showToast("Login successful")
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
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

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
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