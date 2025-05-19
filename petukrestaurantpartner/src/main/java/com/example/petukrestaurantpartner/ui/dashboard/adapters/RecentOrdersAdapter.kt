package com.example.petukrestaurantpartner.ui.dashboard.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.petukrestaurantpartner.R
import com.example.petukrestaurantpartner.ui.dashboard.models.Order
import java.text.NumberFormat
import java.util.*

class RecentOrdersAdapter(
    private var orders: List<Order> = emptyList(),
    private val onOrderClicked: (Order) -> Unit
) : RecyclerView.Adapter<RecentOrdersAdapter.OrderViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.bind(order)
    }

    override fun getItemCount(): Int = orders.size

    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderId: TextView = itemView.findViewById(R.id.tv_order_id)
        private val tvOrderDate: TextView = itemView.findViewById(R.id.tv_order_date)
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tv_customer_name)
        private val tvOrderAmount: TextView = itemView.findViewById(R.id.tv_order_amount)
        private val tvOrderStatus: TextView = itemView.findViewById(R.id.tv_order_status)

        fun bind(order: Order) {
            val orderId = order.orderId.takeLast(6) // Show only the last 6 characters for brevity
            tvOrderId.text = "#$orderId"
            tvOrderDate.text = order.getFormattedDate()
            tvCustomerName.text = order.customerName
            tvOrderAmount.text = currencyFormat.format(order.total)
            tvOrderStatus.text = order.orderStatus

            // Set status color based on order status
            val statusColor = when (order.orderStatus.uppercase()) {
                "ACCEPTED" -> itemView.context.getColor(R.color.status_completed)
                "PREPARED" -> itemView.context.getColor(R.color.status_preparing)
                "DELIVERED" -> itemView.context.getColor(R.color.status_delivering)
                "CANCELLED" -> itemView.context.getColor(R.color.status_cancelled)
                else -> itemView.context.getColor(R.color.status_default)
            }
            tvOrderStatus.setTextColor(statusColor)

            // Set click listener
            itemView.setOnClickListener { onOrderClicked(order) }
        }
    }
}