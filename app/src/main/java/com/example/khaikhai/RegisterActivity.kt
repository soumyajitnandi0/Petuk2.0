package com.example.khaikhai

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.khaikhai.databinding.ActivityRegisterBinding
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

        // Initialize Firebase Database
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.reference.child("customers")

        // Set click listeners
        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Validation
        when {
            fullName.isEmpty() -> {
                binding.etFullName.error = "Full name is required"
                binding.etFullName.requestFocus()
                return
            }
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
            password.length < 6 -> {
                binding.etPassword.error = "Password must be at least 6 characters"
                binding.etPassword.requestFocus()
                return
            }
            confirmPassword.isEmpty() -> {
                binding.etConfirmPassword.error = "Please confirm your password"
                binding.etConfirmPassword.requestFocus()
                return
            }
            password != confirmPassword -> {
                binding.etConfirmPassword.error = "Passwords don't match"
                binding.etConfirmPassword.requestFocus()
                return
            }
            else -> {
                // Show progress bar
                binding.progressBar.visibility = View.VISIBLE

                // Check if email already exists in database
                checkIfEmailExists(email, fullName, password)
            }
        }
    }

    private fun checkIfEmailExists(email: String, fullName: String, password: String) {
        databaseReference.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Email already exists
                        binding.progressBar.visibility = View.GONE
                        binding.etEmail.error = "This email is already registered"
                        binding.etEmail.requestFocus()
                    } else {
                        // Email doesn't exist, proceed with registration
                        registerNewUser(email, fullName, password)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar.visibility = View.GONE
                    showToast("Registration failed: ${error.message}")
                }
            })
    }

    private fun registerNewUser(email: String, fullName: String, password: String) {
        // Generate a unique ID for the new user
        val userId = databaseReference.push().key

        // Hash password for security before storing
        val hashedPassword = hashPassword(password)

        // Create customer object
        val customer = Customer(
            id = userId,
            fullName = fullName,
            email = email,
            phone = "", // User can add later
            password = hashedPassword,
            profilePhotoUrl = ""
        )

        // Save to database
        saveUserToDatabase(customer)
    }

    private fun saveUserToDatabase(customer: Customer) {
        customer.id?.let { userId ->
            databaseReference.child(userId).setValue(customer)
                .addOnCompleteListener { task ->
                    binding.progressBar.visibility = View.GONE
                    if (task.isSuccessful) {
                        showToast("Registration successful")

                        // Go to login activity
                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        showToast("Failed to save user data: ${task.exception?.message}")
                    }
                }
        }
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