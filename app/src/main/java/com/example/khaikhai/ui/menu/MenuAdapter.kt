package com.example.khaikhai.ui.menu

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.khaikhai.R
import com.example.khaikhai.cart.CartManager
import com.squareup.picasso.Picasso
import androidx.core.content.ContextCompat
import android.graphics.drawable.Drawable
import com.squareup.picasso.Callback
import android.view.animation.AnimationUtils

class MenuAdapter(
    private val context: Context,
    private val menuItems: List<MenuItem>,
    private val onAddToCartClicked: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    private val cartManager = CartManager(context)

    class MenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val menuItemImage: ImageView = itemView.findViewById(R.id.menu_item_image)
        val menuItemName: TextView = itemView.findViewById(R.id.menu_item_name)
        val menuItemDescription: TextView = itemView.findViewById(R.id.menu_item_description)
        val menuItemPrice: TextView = itemView.findViewById(R.id.menu_item_price)
        val addToCartButton: Button = itemView.findViewById(R.id.addToCartButton)

        // Quantity control elements
        val quantityContainer: ConstraintLayout = itemView.findViewById(R.id.quantityControlContainer)
        val decreaseButton: ImageButton = itemView.findViewById(R.id.decreaseQuantityButton)
        val increaseButton: ImageButton = itemView.findViewById(R.id.increaseQuantityButton)
        val quantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val menuItem = menuItems[position]

        holder.menuItemName.text = menuItem.name
        holder.menuItemDescription.text = menuItem.description
        holder.menuItemPrice.text = "₹${String.format("%.2f", menuItem.price)}"

        // Apply fade-in animation to items
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.item_animation_fall_down))

        // Load image using Picasso if available
        if (menuItem.imageUrl.isNotEmpty()) {
            val errorDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
            val placeholderDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_launcher_background)

            Picasso.get()
                .load(menuItem.imageUrl)
                .placeholder(placeholderDrawable!!)
                .error(errorDrawable!!)
                .into(holder.menuItemImage, object : Callback {
                    override fun onSuccess() {
                        // Image loaded successfully
                    }

                    override fun onError(e: Exception?) {
                        // Handle error loading image
                    }
                })
        } else {
            holder.menuItemImage.setImageResource(R.drawable.ic_launcher_background)
        }

        // Check if item is already in cart
        val cartItems = cartManager.getCartItems()
        val existingCartItem = cartItems.find { it.menuItem.itemId == menuItem.itemId }

        if (existingCartItem != null) {
            // Item already in cart - show quantity controls
            holder.addToCartButton.visibility = View.GONE
            holder.quantityContainer.visibility = View.VISIBLE
            holder.quantityTextView.text = existingCartItem.quantity.toString()

            // Set up decrease button
            holder.decreaseButton.setOnClickListener {
                if (existingCartItem.quantity > 1) {
                    cartManager.updateItemQuantity(menuItem.itemId!!, existingCartItem.quantity - 1)
                    notifyItemChanged(position)
                } else {
                    // Remove if quantity becomes 0
                    cartManager.removeItem(menuItem.itemId!!)
                    notifyItemChanged(position)
                }
            }

            // Set up increase button
            holder.increaseButton.setOnClickListener {
                cartManager.updateItemQuantity(menuItem.itemId!!, existingCartItem.quantity + 1)
                notifyItemChanged(position)
            }
        } else {
            // Item not in cart - show add button
            holder.addToCartButton.visibility = View.VISIBLE
            holder.quantityContainer.visibility = View.GONE

            // Set click listener for add to cart button
            holder.addToCartButton.setOnClickListener {
                onAddToCartClicked(menuItem)
                notifyItemChanged(position)
            }
        }
    }

    override fun getItemCount(): Int = menuItems.size
}