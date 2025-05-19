package com.example.khaikhai.ui.menu

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.khaikhai.R
import com.example.khaikhai.cart.AddToCartResult
import com.example.khaikhai.cart.CartActivity
import com.example.khaikhai.cart.CartManager
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class MenuActivity : AppCompatActivity() {

    private lateinit var menuRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var menuAdapter: MenuAdapter
    private lateinit var cartManager: CartManager
    private lateinit var viewModel: MenuViewModel
    private lateinit var restaurantTitle: TextView
    private lateinit var viewCartButton: ExtendedFloatingActionButton

    private var restaurantId: String = ""
    private var restaurantName: String = ""
    private val menuItems: MutableList<MenuItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // Get restaurant data from intent
        restaurantId = intent.getStringExtra("RESTAURANT_ID") ?: ""
        restaurantName = intent.getStringExtra("RESTAURANT_NAME") ?: ""

        // For debugging
        if (restaurantName.isEmpty()) {
            Toast.makeText(this, "Restaurant name not provided in intent", Toast.LENGTH_SHORT).show()
        }

        // Set up toolbar with restaurant name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = restaurantName

        // Initialize views
        menuRecyclerView = findViewById(R.id.menuRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.emptyView)
        restaurantTitle = findViewById(R.id.tv_restaurant_title)
        viewCartButton = findViewById(R.id.fab_view_cart)

        restaurantTitle.text = restaurantName

        // Initialize CartManager
        cartManager = CartManager(this)

        // Set up RecyclerView
        menuRecyclerView.layoutManager = GridLayoutManager(this, 2)
        menuAdapter = MenuAdapter(this, menuItems) { menuItem ->
            addItemToCart(menuItem)
        }
        menuRecyclerView.adapter = menuAdapter

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(MenuViewModel::class.java)

        // Observe menu items
        viewModel.menuItems.observe(this) { items ->
            menuItems.clear()
            menuItems.addAll(items)

            // Update UI
            progressBar.visibility = View.GONE

            if (menuItems.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                menuRecyclerView.visibility = View.GONE
            } else {
                emptyView.visibility = View.GONE
                menuRecyclerView.visibility = View.VISIBLE
                menuAdapter.notifyDataSetChanged()
            }
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { errorMsg ->
            if (errorMsg.isNotEmpty()) {
                progressBar.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                menuRecyclerView.visibility = View.GONE
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }

        // Set up view cart button
        setupViewCartButton()

        // Load menu items
        loadMenuItems()
    }

    private fun setupViewCartButton() {
        // Initialize the button with current cart count
        updateCartButton()

        // Set click listener to navigate to CartActivity
        viewCartButton.setOnClickListener {
            val cartItems = cartManager.getCartItems()
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, CartActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun updateCartButton() {
        val cartItems = cartManager.getCartItems()
        val itemCount = cartItems.sumOf { it.quantity }

        if (itemCount > 0) {
            viewCartButton.text = "View Cart ($itemCount)"
            viewCartButton.visibility = View.VISIBLE
            viewCartButton.extend()
        } else {
            viewCartButton.text = "View Cart"
            viewCartButton.shrink()
        }
    }

    private fun loadMenuItems() {
        if (restaurantId.isEmpty()) {
            emptyView.text = "Restaurant ID not found"
            emptyView.visibility = View.VISIBLE
            menuRecyclerView.visibility = View.GONE
            return
        }

        progressBar.visibility = View.VISIBLE
        viewModel.loadMenuItems(restaurantId)
    }

    private fun addItemToCart(menuItem: MenuItem) {
        when (val result = cartManager.addItem(menuItem, restaurantId, restaurantName)) {
            is AddToCartResult.Success -> {
                Toast.makeText(this, "${menuItem.name} added to cart", Toast.LENGTH_SHORT).show()
                updateCartButton() // Update the cart button with the new count
                // Refresh the adapter to update quantity controls
                menuAdapter.notifyDataSetChanged()
            }
            is AddToCartResult.DifferentRestaurant -> {
                showDifferentRestaurantDialog(result.currentRestaurantName, menuItem)
            }
        }
    }

    private fun showDifferentRestaurantDialog(currentRestaurantName: String, newMenuItem: MenuItem) {
        AlertDialog.Builder(this)
            .setTitle("Different Restaurant")
            .setMessage("Your cart contains items from $currentRestaurantName. Do you want to clear your cart and add items from $restaurantName?")
            .setPositiveButton("Yes") { _, _ ->
                cartManager.clearCart()
                addItemToCart(newMenuItem)
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Update cart button when returning to this activity
        updateCartButton()
        // Refresh adapter to update quantity indicators
        menuAdapter.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}