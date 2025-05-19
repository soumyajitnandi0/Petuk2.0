package com.example.petukrestaurantpartner.ui.orders

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.petukrestaurantpartner.R
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

/**
 * Implementation for the LiveOrdersFragment - Shows all active orders that need to be prepared
 * Includes new orders (PLACED) and orders being prepared (ACCEPTED, COOKING)
 */
class LiveOrdersFragment : Fragment(), OrdersAdapter.OnOrderActionListener {

    private lateinit var ordersViewModel: OrdersViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var ordersAdapter: OrdersAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var noOrdersView: View
    private lateinit var progressBar: View
    private lateinit var rootView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_order_list, container, false)
        rootView = root

        Timber.d("LiveOrdersFragment: onCreateView")

        // Initialize views
        recyclerView = root.findViewById(R.id.recycler_orders)
        swipeRefreshLayout = root.findViewById(R.id.swipe_refresh)
        noOrdersView = root.findViewById(R.id.text_no_orders)
        progressBar = root.findViewById(R.id.progress_bar)

        // Get the shared ViewModel from parent fragment
        ordersViewModel = ViewModelProvider(requireParentFragment()).get(OrdersViewModel::class.java)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        ordersAdapter = OrdersAdapter(OrdersAdapter.OrderType.LIVE)
        ordersAdapter.setOnOrderActionListener(this)
        recyclerView.adapter = ordersAdapter

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            Timber.d("LiveOrdersFragment: Manual refresh triggered")
            ordersViewModel.fetchOrders()
        }

        // Show loading initially
        progressBar.visibility = View.VISIBLE

        // Observe live orders
        ordersViewModel.liveOrders.observe(viewLifecycleOwner) { orders ->
            Timber.d("LiveOrdersFragment: Received ${orders.size} live orders")

            // Apply animation to new items
            val previousSize = ordersAdapter.itemCount
            ordersAdapter.submitList(orders)

            // If there are new items, apply animation to the recyclerView
            if (orders.size > previousSize) {
                recyclerView.scheduleLayoutAnimation()
            }

            // Show/hide no orders view
            if (orders.isEmpty()) {
                noOrdersView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                Timber.d("LiveOrdersFragment: No orders to display")
            } else {
                noOrdersView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                Timber.d("LiveOrdersFragment: Displaying ${orders.size} orders")
            }

            swipeRefreshLayout.isRefreshing = false
        }

        // Observe loading state
        ordersViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading && ordersAdapter.itemCount == 0) View.VISIBLE else View.GONE

            if (isLoading) {
                Timber.d("LiveOrdersFragment: Loading orders...")
            }
        }

        // Observe errors
        ordersViewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Timber.e("LiveOrdersFragment: Error - $it")
                Snackbar.make(rootView, it, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.error_color))
                    .show()
                swipeRefreshLayout.isRefreshing = false
            }
        }

        // Observe status update success
        ordersViewModel.statusUpdateSuccess.observe(viewLifecycleOwner) { event ->
            event?.let {
                Timber.d("LiveOrdersFragment: Status update - ${it.message}")

                if (it.showAnimation) {
                    // Show fancy animation that an order is moving to prepared
                    showOrderMovingAnimation()
                }

                Snackbar.make(rootView, it.message, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.success_color))
                    .show()
            }
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        Timber.d("LiveOrdersFragment: onResume, triggering data fetch")
        // Refresh orders when fragment becomes visible
        ordersViewModel.fetchOrders()
    }

    // Order action implementations
    override fun onAcceptOrder(orderId: String) {
        Timber.d("LiveOrdersFragment: Accepting order $orderId")
        ordersViewModel.acceptOrder(orderId)
    }

    override fun onRejectOrder(orderId: String) {
        Timber.d("LiveOrdersFragment: Rejecting order $orderId")
        ordersViewModel.rejectOrder(orderId)
    }

    override fun onMarkPrepared(orderId: String) {
        Timber.d("LiveOrdersFragment: Marking order $orderId as prepared")
        // This will update the order status and trigger navigation to the Prepared tab
        ordersViewModel.markOrderPrepared(orderId)
    }

    override fun onMarkDelivered(orderId: String) {
        // Not used in this fragment
    }

    override fun onViewOrderDetails(order: RestaurantOrder) {
        Timber.d("LiveOrdersFragment: Viewing details for order ${order.orderId}")
        // Navigate to order details
        Toast.makeText(context, "View order details: ${order.orderId}", Toast.LENGTH_SHORT).show()
    }

    private fun showOrderMovingAnimation() {
        // A flashing background color animation to indicate a state change
        val colorFrom = ContextCompat.getColor(requireContext(), android.R.color.transparent)
        val colorTo = ContextCompat.getColor(requireContext(), R.color.highlight_color)

        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo, colorFrom)
        colorAnimation.duration = 1000 // 1 second
        colorAnimation.addUpdateListener { animator ->
            rootView.setBackgroundColor(animator.animatedValue as Int)
        }
        colorAnimation.start()
    }
}