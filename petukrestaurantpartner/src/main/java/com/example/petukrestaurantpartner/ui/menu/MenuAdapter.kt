package com.example.petukrestaurantpartner.ui.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.petukrestaurantpartner.databinding.ItemMenuBinding
import com.example.petukrestaurantpartner.ui.menu.MenuItem
import java.text.NumberFormat
import java.util.Locale

class MenuAdapter(
    private val onEditClick: (MenuItem) -> Unit,
    private val onDeleteClick: (MenuItem) -> Unit,
    private val onAvailabilityChanged: (MenuItem, Boolean) -> Unit
) : ListAdapter<MenuItem, MenuAdapter.MenuViewHolder>(MenuDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MenuViewHolder(private val binding: ItemMenuBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(menuItem: MenuItem) {
            binding.apply {
                textViewItemName.text = menuItem.name
                textViewItemDescription.text = menuItem.description
                textViewItemCategory.text = menuItem.category

                // Format price as currency
                val numberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
                textViewItemPrice.text = numberFormat.format(menuItem.price)

                switchItemAvailability.isChecked = menuItem.isAvailable
                switchItemAvailability.text = if (menuItem.isAvailable) "Available   " else "Unavailable   "

                switchItemAvailability.setOnCheckedChangeListener { _, isChecked ->
                    onAvailabilityChanged(menuItem, isChecked)
                }

                buttonEditItem.setOnClickListener {
                    onEditClick(menuItem)
                }

                buttonDeleteItem.setOnClickListener {
                    onDeleteClick(menuItem)
                }
            }
        }
    }

    class MenuDiffCallback : DiffUtil.ItemCallback<MenuItem>() {
        override fun areItemsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
            return oldItem.itemId == newItem.itemId
        }

        override fun areContentsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean {
            return oldItem == newItem
        }
    }
}