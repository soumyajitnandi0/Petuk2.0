package com.example.petukrestaurantpartner.ui.orders

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.petukrestaurantpartner.R
import java.text.NumberFormat
import java.util.*

/**
 * Adapter for restaurant orders with different layouts based on order status
 */
class OrdersAdapter(private val orderType: OrderType) :
    ListAdapter<RestaurantOrder, OrdersAdapter.OrderViewHolder>(OrderDiffCallback()) {

    // Listener for order actions
    private var orderActionListener: OnOrderActionListener? = null

    enum class OrderType {
        LIVE,       // New and in-progress orders
        PREPARED,   // Orders ready for pickup/delivery
        DELIVERED   // Completed orders
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = getItem(position)
        holder.bind(order, orderType, orderActionListener)
    }

    fun setOnOrderActionListener(listener: OnOrderActionListener) {
        this.orderActionListener = listener
    }

    /**
     * ViewHolder for order items
     */
    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textOrderId: TextView = itemView.findViewById(R.id.text_order_id)
        private val textOrderStatus: TextView = itemView.findViewById(R.id.text_order_status)
        private val textCustomerName: TextView = itemView.findViewById(R.id.text_customer_name)
        private val textOrderTime: TextView = itemView.findViewById(R.id.text_order_time)
        private val textItemCount: TextView = itemView.findViewById(R.id.text_item_count)
        private val textOrderTotal: TextView = itemView.findViewById(R.id.text_order_total)
        private val btnAccept: Button = itemView.findViewById(R.id.button_accept)
        private val btnReject: Button = itemView.findViewById(R.id.button_reject)
        private val btnPrepared: Button = itemView.findViewById(R.id.button_prepared)
        private val btnDelivered: Button = itemView.findViewById(R.id.button_delivered)
        private val btnViewDetails: Button = itemView.findViewById(R.id.button_view_details)

        fun bind(order: RestaurantOrder, orderType: OrderType, listener: OnOrderActionListener?) {
            // Set basic order info
            textOrderId.text = "Order #${order.orderId?.takeLast(6) ?: "Unknown"}"
            textCustomerName.text = order.customerName ?: "Unknown Customer"
            textOrderTime.text = order.getFormattedDateTime()
            textItemCount.text = "${order.getItemCount()} items"

            // Format currency
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            textOrderTotal.text = currencyFormat.format(order.total)

            // Set order status with appropriate color
            val statusText = formatOrderStatus(order.orderStatus)
            textOrderStatus.text = statusText
            setStatusColor(textOrderStatus, order.orderStatus)

            // Configure buttons based on order type and status
            configureButtons(order, orderType, listener)

            // Set detail button click listener
            btnViewDetails.setOnClickListener {
                listener?.onViewOrderDetails(order)
            }
        }

        private fun formatOrderStatus(status: String?): String {
            return when (status) {
                "PLACED" -> "New Order"
                "ACCEPTED" -> "Accepted"
                "COOKING" -> "Preparing"
                "PREPARED" -> "Ready"
                "OUT_FOR_DELIVERY" -> "Out for Delivery"
                "DELIVERED" -> "Delivered"
                "COMPLETED" -> "Completed"
                "REJECTED" -> "Rejected"
                "CANCELLED" -> "Cancelled"
                else -> status ?: "Unknown"
            }
        }

        private fun setStatusColor(textView: TextView, status: String?) {
            val color = when (status) {
                "PLACED" -> Color.parseColor("#FF9800") // Orange
                "ACCEPTED", "COOKING" -> Color.parseColor("#2196F3") // Blue
                "PREPARED", "OUT_FOR_DELIVERY" -> Color.parseColor("#8BC34A") // Light Green
                "DELIVERED", "COMPLETED" -> Color.parseColor("#4CAF50") // Green
                "REJECTED", "CANCELLED" -> Color.parseColor("#F44336") // Red
                else -> Color.parseColor("#757575") // Gray
            }

            textView.setBackgroundColor(color)
        }

        private fun configureButtons(order: RestaurantOrder, orderType: OrderType, listener: OnOrderActionListener?) {
            // Hide all action buttons by default
            btnAccept.visibility = View.GONE
            btnReject.visibility = View.GONE
            btnPrepared.visibility = View.GONE
            btnDelivered.visibility = View.GONE

            when (orderType) {
                OrderType.LIVE -> {
                    // Show accept/reject buttons for new orders
                    if (order.orderStatus == "PLACED") {
                        btnAccept.visibility = View.VISIBLE
                        btnReject.visibility = View.VISIBLE

                        btnAccept.setOnClickListener {
                            listener?.onAcceptOrder(order.orderId ?: "")
                        }

                        btnReject.setOnClickListener {
                            listener?.onRejectOrder(order.orderId ?: "")
                        }
                    }

                    // Show mark as prepared button for accepted/cooking orders
                    if (order.orderStatus == "ACCEPTED" || order.orderStatus == "COOKING") {
                        btnPrepared.visibility = View.VISIBLE

                        btnPrepared.setOnClickListener {
                            listener?.onMarkPrepared(order.orderId ?: "")
                        }
                    }
                }

                OrderType.PREPARED -> {
                    // Show mark as delivered button for prepared orders
                    if (order.orderStatus == "PREPARED" || order.orderStatus == "OUT_FOR_DELIVERY") {
                        btnDelivered.visibility = View.VISIBLE

                        btnDelivered.setOnClickListener {
                            listener?.onMarkDelivered(order.orderId ?: "")
                        }
                    }
                }

                OrderType.DELIVERED -> {
                    // No action buttons for delivered orders
                }
            }
        }
    }

    /**
     * DiffUtil callback for efficient RecyclerView updates
     */
    class OrderDiffCallback : DiffUtil.ItemCallback<RestaurantOrder>() {
        override fun areItemsTheSame(oldItem: RestaurantOrder, newItem: RestaurantOrder): Boolean {
            return oldItem.orderId == newItem.orderId
        }

        override fun areContentsTheSame(oldItem: RestaurantOrder, newItem: RestaurantOrder): Boolean {
            return oldItem == newItem
        }
    }

    /**
     * Interface for order actions
     */
    interface OnOrderActionListener {
        fun onAcceptOrder(orderId: String)
        fun onRejectOrder(orderId: String)
        fun onMarkPrepared(orderId: String)
        fun onMarkDelivered(orderId: String)
        fun onViewOrderDetails(order: RestaurantOrder)
    }
}