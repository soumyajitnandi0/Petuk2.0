package com.example.khaikhai.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.khaikhai.AddressActivity
import com.example.khaikhai.R
import com.example.khaikhai.cart.CartActivity
import com.example.khaikhai.cart.CartManager
import com.example.khaikhai.databinding.FragmentHomeBinding
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var restaurantAdapter: RestaurantAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var searchView: SearchView
    private lateinit var cartManager: CartManager
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var fabCart: FloatingActionButton
    private lateinit var cartBadgeText: TextView
    private lateinit var tvDeliveryLocation: TextView

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var cartItemCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize binding
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root = binding.root

        // Initialize views using binding
        recyclerView = binding.rvRestaurants
        progressBar = binding.progressBar
        emptyView = binding.emptyView
        searchView = binding.searchView
        swipeRefreshLayout = binding.swipeRefreshLayout
        shimmerFrameLayout = binding.shimmerFrameLayout
        fabCart = binding.fabCart
        tvDeliveryLocation = binding.tvDeliveryLocation

        // Make the delivery location clickable to open address activity
        tvDeliveryLocation.setOnClickListener {
            openAddressActivity()
        }

        // Initialize CartManager
        cartManager = CartManager(requireContext())

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        restaurantAdapter = RestaurantAdapter(requireContext(), emptyList())
        recyclerView.adapter = restaurantAdapter

        // Add cart badge view directly to the fragment's layout
        val badgeView = inflater.inflate(R.layout.cart_badge, null)
        cartBadgeText = badgeView.findViewById(R.id.cart_badge)

        // Create a FrameLayout for the badge to be placed over the FAB
        val fabContainer = root.findViewById<View>(R.id.fab_cart).parent as ViewGroup
        val fabIndex = fabContainer.indexOfChild(root.findViewById<View>(R.id.fab_cart))

        // Add the badge view
        fabContainer.addView(badgeView, fabIndex + 1) // Add after the FAB

        // Position the badge relative to the FAB
        badgeView.post {
            val fabLocation = IntArray(2)
            fabCart.getLocationOnScreen(fabLocation)

            // Position the badge at the top-right corner of the FAB
            badgeView.x = (fabLocation[0] + fabCart.width - badgeView.width / 2).toFloat()
            badgeView.y = (fabLocation[1] - badgeView.height / 2).toFloat()
        }

        binding.fabCart.setOnClickListener {
            // Navigate to cart activity
            openCartActivity()
        }

        // Setup additional components
        setupCategoryChips(root)
        setupSwipeRefresh()
        setupSearch()

        // Enable options menu
        setHasOptionsMenu(true)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        // Get current user email from SharedPreferences
        val sharedPreferences = requireActivity().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("user_email", null)

        // Load user's default address
        homeViewModel.loadUserDefaultAddress(userEmail)

        // Observe address changes
        homeViewModel.address.observe(viewLifecycleOwner) { address ->
            tvDeliveryLocation.text = address
        }

        // Observe restaurants
        homeViewModel.restaurants.observe(viewLifecycleOwner) { restaurants ->
            restaurantAdapter.updateData(restaurants)

            // Stop shimmer effect
            shimmerFrameLayout.stopShimmer()
            shimmerFrameLayout.visibility = View.GONE

            // Show recyclerView
            recyclerView.visibility = View.VISIBLE

            // Show empty view if no restaurants
            if (restaurants.isEmpty()) {
                emptyView.visibility = View.VISIBLE
            } else {
                emptyView.visibility = View.GONE
            }
        }

        // Observe loading state
        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                // Show shimmer effect instead of progress bar
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.GONE

                shimmerFrameLayout.visibility = View.VISIBLE
                shimmerFrameLayout.startShimmer()
            } else {
                // Hide shimmer when done loading
                shimmerFrameLayout.stopShimmer()
                shimmerFrameLayout.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
            }
        }

        // Load restaurants
        showLoading()
        homeViewModel.loadRestaurants()

        // Initialize cart count
        refreshCartCount()
    }

    private fun showLoading() {
        shimmerFrameLayout.visibility = View.VISIBLE
        shimmerFrameLayout.startShimmer()
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE
    }

    private fun updateCartBadge(count: Int) {
        cartItemCount = count

        if (count > 0) {
            cartBadgeText.visibility = View.VISIBLE
            cartBadgeText.text = if (count > 99) "99+" else count.toString()
        } else {
            cartBadgeText.visibility = View.GONE
        }
    }

    private fun openAddressActivity() {
        val intent = Intent(requireContext(), AddressActivity::class.java)
        startActivity(intent)
    }

    // Fixed refreshCartCount method to use cartManager
    private fun refreshCartCount() {
        // Use cartManager to get the current count
        val count = cartManager.getCartItemCount()
        updateCartBadge(count)
    }

    override fun onResume() {
        super.onResume()
        // Refresh cart count
        refreshCartCount()

        // Refresh user address in case it was changed in AddressActivity
        val sharedPreferences = requireActivity().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("user_email", null)
        homeViewModel.loadUserDefaultAddress(userEmail)

        // Refresh options menu to update cart badge
        activity?.invalidateOptionsMenu()
    }

    override fun onPause() {
        shimmerFrameLayout.stopShimmer()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    searchRestaurants(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    // Reset to show all restaurants
                    homeViewModel.clearSearchFilter()
                } else if (newText.length >= 3) {
                    // Only search when text is at least 3 characters
                    searchRestaurants(newText)
                }
                return true
            }
        })

        // Add clear button functionality
        val closeButton = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        closeButton.setOnClickListener {
            searchView.setQuery("", false)
            searchView.clearFocus()
            homeViewModel.clearSearchFilter()
        }
    }

    private fun setupCategoryChips(root: View) {
        val chipGroup = root.findViewById<ChipGroup>(R.id.category_chip_group)

        // Set up "All" chip click listener
        root.findViewById<Chip>(R.id.chip_all)?.setOnClickListener {
            homeViewModel.clearCategoryFilter()
        }

        // Set up category chip click listeners
        root.findViewById<Chip>(R.id.chip_indian)?.setOnClickListener {
            homeViewModel.filterByCategory("Indian")
        }

        root.findViewById<Chip>(R.id.chip_chinese)?.setOnClickListener {
            homeViewModel.filterByCategory("Chinese")
        }

        root.findViewById<Chip>(R.id.chip_italian)?.setOnClickListener {
            homeViewModel.filterByCategory("Italian")
        }

        root.findViewById<Chip>(R.id.chip_desserts)?.setOnClickListener {
            homeViewModel.filterByCategory("Desserts")
        }
    }

    private fun setupSwipeRefresh() {
        // Set refresh indicator colors
        swipeRefreshLayout.setColorSchemeResources(
            R.color.primary,
            R.color.colorAccent,
            android.R.color.holo_orange_dark
        )

        // Set refresh listener
        swipeRefreshLayout.setOnRefreshListener {
            // Clear any filters and reload data
            searchView.setQuery("", false)
            searchView.clearFocus()
            homeViewModel.clearCategoryFilter()
            homeViewModel.loadRestaurants()

            // Reload the user's address
            val sharedPreferences = requireActivity().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
            val userEmail = sharedPreferences.getString("user_email", null)
            homeViewModel.loadUserDefaultAddress(userEmail)
        }
    }

    private fun searchRestaurants(query: String) {
        homeViewModel.searchRestaurants(query)
    }

    private fun openCartActivity() {
        val intent = Intent(requireContext(), CartActivity::class.java)
        startActivity(intent)
    }
}