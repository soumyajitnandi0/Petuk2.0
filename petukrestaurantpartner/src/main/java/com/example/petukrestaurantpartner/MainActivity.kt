package com.example.petukrestaurantpartner

import android.os.Bundle
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.petukrestaurantpartner.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var restaurantId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get restaurant ID from intent
        restaurantId = intent.getStringExtra("RESTAURANT_ID")

        // If no restaurant ID is provided, warn user and finish activity
        if (restaurantId == null) {
            Toast.makeText(this, "Restaurant ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Top-level destinations
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_orders, R.id.navigation_dashboard, R.id.navigation_profile
            )
        )

        // ❌ Do NOT call setupActionBarWithNavController
        navView.setupWithNavController(navController)
    }

    fun getRestaurantId(): String? {
        return restaurantId
    }
}
