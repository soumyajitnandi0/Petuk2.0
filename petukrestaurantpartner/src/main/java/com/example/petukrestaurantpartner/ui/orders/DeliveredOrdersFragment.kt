package com.example.petukrestaurantpartner.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.petukrestaurantpartner.R

/**
 * Implementation for the DeliveredOrdersFragment
 * Shows all completed orders that have been delivered to customers
 */
class DeliveredOrdersFragment : Fragment(), OrdersAdapter.OnOrderActionListener {

    private lateinit var ordersViewModel: OrdersViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var ordersAdapter: OrdersAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var noOrdersView: View
    private lateinit var progressBar: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_order_list, container, false)

        // Get the shared ViewModel from parent fragment
        ordersViewModel = ViewModelProvider(requireParentFragment()).get(OrdersViewModel::class.java)

        // Initialize views
        recyclerView = root.findViewById(R.id.recycler_orders)
        swipeRefreshLayout = root.findViewById(R.id.swipe_refresh)
        noOrdersView = root.findViewById(R.id.text_no_orders)
        progressBar = root.findViewById(R.id.progress_bar)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        ordersAdapter = OrdersAdapter(OrdersAdapter.OrderType.DELIVERED)
        ordersAdapter.setOnOrderActionListener(this)
        recyclerView.adapter = ordersAdapter

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            ordersViewModel.fetchOrders()
        }

        // Observe delivered orders
        ordersViewModel.deliveredOrders.observe(viewLifecycleOwner) { orders ->
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
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false
            }
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        // Refresh orders when fragment becomes visible
        ordersViewModel.fetchOrders()
    }

    // Order action implementations - mostly not used for delivered orders
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
        // Not used in this fragment
    }

    override fun onViewOrderDetails(order: RestaurantOrder) {
        // Navigate to order details (implement navigation component)
        Toast.makeText(context, "View order details: ${order.orderId}", Toast.LENGTH_SHORT).show()
    }
}