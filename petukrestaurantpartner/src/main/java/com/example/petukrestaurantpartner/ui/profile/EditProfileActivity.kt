package com.example.petukrestaurantpartner.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.petukrestaurantpartner.Restaurant
import com.example.petukrestaurantpartner.databinding.ActivityEditProfileBinding
import com.google.firebase.database.FirebaseDatabase
import android.util.Patterns

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var restaurantId: String
    private var restaurant: Restaurant? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set toolbar title and back button
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Restaurant Profile"
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        // Get restaurant data from intent
        restaurantId = intent.getStringExtra("RESTAURANT_ID") ?: ""
        restaurant = intent.getParcelableExtra("RESTAURANT_DATA")

        if (restaurantId.isEmpty() || restaurant == null) {
            Toast.makeText(this, "Error: Restaurant data not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Populate form fields with current data
        populateFields()

        // Set up save button
        binding.buttonSave.setOnClickListener {
            if (validateInputs()) {
                saveRestaurantData()
            }
        }
    }

    private fun populateFields() {
        restaurant?.let {
            binding.editTextRestaurantName.setText(it.restaurantName)
            binding.editTextEmail.setText(it.restaurantEmail)
            binding.editTextPhone.setText(it.restaurantPhone)
            binding.editTextAddress.setText(it.restaurantAddress)
            binding.editTextCuisineType.setText(it.cuisineType)
            binding.editTextDeliveryTime.setText(it.averageDeliveryTime.toString())
        }
    }

    private fun validateInputs(): Boolean {
        val name = binding.editTextRestaurantName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val phone = binding.editTextPhone.text.toString().trim()
        val address = binding.editTextAddress.text.toString().trim()
        val cuisine = binding.editTextCuisineType.text.toString().trim()
        val deliveryTimeStr = binding.editTextDeliveryTime.text.toString().trim()

        if (name.isEmpty()) {
            binding.editTextRestaurantName.error = "Required"
            return false
        }

        if (email.isEmpty()) {
            binding.editTextEmail.error = "Required"
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextEmail.error = "Invalid email format"
            return false
        }

        if (phone.isEmpty()) {
            binding.editTextPhone.error = "Required"
            return false
        }

        if (phone.length < 10 || !phone.all { it.isDigit() }) {
            binding.editTextPhone.error = "Invalid phone number"
            return false
        }

        if (address.isEmpty()) {
            binding.editTextAddress.error = "Required"
            return false
        }

        if (cuisine.isEmpty()) {
            binding.editTextCuisineType.error = "Required"
            return false
        }

        if (deliveryTimeStr.isEmpty()) {
            binding.editTextDeliveryTime.error = "Required"
            return false
        }

        try {
            val deliveryTime = deliveryTimeStr.toInt()
            if (deliveryTime < 5 || deliveryTime > 120) {
                binding.editTextDeliveryTime.error = "Must be between 5-120 minutes"
                return false
            }
        } catch (e: NumberFormatException) {
            binding.editTextDeliveryTime.error = "Invalid number"
            return false
        }

        return true
    }

    private fun saveRestaurantData() {
        binding.progressBar.visibility = View.VISIBLE

        val updatedRestaurant = restaurant?.copy(
            restaurantName = binding.editTextRestaurantName.text.toString().trim(),
            restaurantEmail = binding.editTextEmail.text.toString().trim(),
            restaurantPhone = binding.editTextPhone.text.toString().trim(),
            restaurantAddress = binding.editTextAddress.text.toString().trim(),
            cuisineType = binding.editTextCuisineType.text.toString().trim(),
            averageDeliveryTime = binding.editTextDeliveryTime.text.toString().toInt()
        ) ?: return

        val databaseReference = FirebaseDatabase.getInstance().getReference("restaurants")
        databaseReference.child(restaurantId).setValue(updatedRestaurant)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}