package com.example.khaikhai.ui.wallet

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.khaikhai.R

class TransactionAdapter :
    ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction)
    }

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val restaurantTextView: TextView = itemView.findViewById(R.id.text_transaction_restaurant)
        private val dateTextView: TextView = itemView.findViewById(R.id.text_transaction_date)
        private val amountTextView: TextView = itemView.findViewById(R.id.text_transaction_amount)
        private val iconImageView: ImageView = itemView.findViewById(R.id.image_transaction_type)

        fun bind(transaction: Transaction) {
            restaurantTextView.text = transaction.restaurantName
            dateTextView.text = transaction.date

            val context = itemView.context

            if (transaction.isReward) {
                // For redemptions (negative balance)
                amountTextView.text = "-$${transaction.amount}"
                amountTextView.setTextColor(ContextCompat.getColor(context, R.color.redemption_red))

                // Set redemption icon background
                iconImageView.background = ContextCompat.getDrawable(context, R.drawable.circle_background_red)
                iconImageView.setImageResource(R.drawable.ic_redeem)
            } else {
                // For earned cashback (positive balance)
                amountTextView.text = "+$${transaction.amount}"
                amountTextView.setTextColor(ContextCompat.getColor(context, R.color.cashback_green))

                // Set cashback icon background
                iconImageView.background = ContextCompat.getDrawable(context, R.drawable.circle_background_green)
                iconImageView.setImageResource(R.drawable.ic_restaurant)
            }
        }
    }

    private class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}