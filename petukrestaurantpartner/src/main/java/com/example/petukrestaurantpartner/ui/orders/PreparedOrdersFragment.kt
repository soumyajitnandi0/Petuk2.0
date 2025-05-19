package com.example.petukrestaurantpartner.ui.orders

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.petukrestaurantpartner.R
import com.google.android.material.snackbar.Snackbar

/**
 * Implementation for the PreparedOrdersFragment
 * Shows all orders that have been prepared and are pending delivery or pickup
 */
class PreparedOrdersFragment : Fragment(), OrdersAdapter.OnOrderActionListener {

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

        // Get the shared ViewModel from parent fragment
        ordersViewModel = ViewModelProvider(requireParentFragment()).get(OrdersViewModel::class.java)

        // Initialize views
        recyclerView = root.findViewById(R.id.recycler_orders)
        swipeRefreshLayout = root.findViewById(R.id.swipe_refresh)
        noOrdersView = root.findViewById(R.id.text_no_orders)
        progressBar = root.findViewById(R.id.progress_bar)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        ordersAdapter = OrdersAdapter(OrdersAdapter.OrderType.PREPARED)
        ordersAdapter.setOnOrderActionListener(this)
        recyclerView.adapter = ordersAdapter

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            ordersViewModel.fetchOrders()
        }

        // Observe prepared orders
        ordersViewModel.preparedOrders.observe(viewLifecycleOwner) { orders ->
            ordersAdapter.submitList(orders)

            // Show/hide no orders view
            if (orders.isEmpty()) {
                noOrdersView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                noOrdersView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }

            swipeRefreshLayout.isRefreshing = false
        }

        // Observe loading state
        ordersViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading && ordersAdapter.itemCount == 0) View.VISIBLE else View.GONE
        }

        // Observe errors
        ordersViewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Snackbar.make(rootView, it, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.error_color))
                    .show()
                swipeRefreshLayout.isRefreshing = false
            }
        }

        // Observe status update success
        ordersViewModel.statusUpdateSuccess.observe(viewLifecycleOwner) { event ->
            event?.let {
                if (it.showAnimation) {
                    // Show fancy animation that an order is moving to delivered
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
        // Refresh orders when fragment becomes visible
        ordersViewModel.fetchOrders()
    }

    // Order action implementations
    override fun onAcceptOrder(orderId: String) {
        // Not used in this fragment
    }

    override fun onRejectOrder(orderId: String) {
        // Not used in this fragment
    }

    override fun onMarkPrepared(orderId: String) {
        // Not used in this fragment
    }

    override fun onMarkDelivered(orderId: String) {
        ordersViewModel.markOrderDelivered(orderId)
    }

    override fun onViewOrderDetails(order: RestaurantOrder) {
        // Navigate to order details
        // Example: findNavController().navigate(
        //     PreparedOrdersFragmentDirections.actionToOrderDetails(order.orderId)
        // )
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