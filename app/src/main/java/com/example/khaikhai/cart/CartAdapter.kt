package com.example.khaikhai.cart

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.khaikhai.R
import com.squareup.picasso.Picasso

class CartAdapter(
    private var cartItems: List<CartItem>,
    private val onQuantityChanged: (String, Int) -> Unit,
    private val onItemRemoved: (String) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImageView: ImageView = itemView.findViewById(R.id.itemImageView)
        val itemNameTextView: TextView = itemView.findViewById(R.id.itemNameTextView)
        val itemPriceTextView: TextView = itemView.findViewById(R.id.itemPriceTextView)
        val itemUnitPriceTextView: TextView = itemView.findViewById(R.id.itemUnitPriceTextView)
        val itemQuantityTextView: TextView = itemView.findViewById(R.id.itemQuantityTextView)
        val decreaseButton: ImageButton = itemView.findViewById(R.id.decreaseButton)
        val increaseButton: ImageButton = itemView.findViewById(R.id.increaseButton)
        val removeButton: ImageButton = itemView.findViewById(R.id.removeButton)
        val itemContainer: View = itemView.findViewById(R.id.itemContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val cartItem = cartItems[position]
        val menuItem = cartItem.menuItem

        holder.itemNameTextView.text = menuItem.name
        holder.itemPriceTextView.text = "₹${String.format("%.2f", cartItem.getTotalPrice())}"
        holder.itemUnitPriceTextView.text = "₹${String.format("%.2f", menuItem.price)} each"
        holder.itemQuantityTextView.text = cartItem.quantity.toString()

        // Apply animations when binding
        val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.fade_in)
        holder.itemContainer.startAnimation(animation)

        // Load image if available
        if (menuItem.imageUrl.isNotEmpty()) {
            Picasso.get()
                .load(menuItem.imageUrl)
                .placeholder(R.drawable.food_placeholder)
                .error(R.drawable.ic_launcher_foreground)
                .fit()
                .centerCrop()
                .into(holder.itemImageView)
        } else {
            holder.itemImageView.setImageResource(R.drawable.food_placeholder)
        }

        // Set up decrease button
        holder.decreaseButton.setOnClickListener {
            if (cartItem.quantity > 1) {
                // Add animation
                val scaleAnimation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.button_scale)
                holder.decreaseButton.startAnimation(scaleAnimation)

                onQuantityChanged(menuItem.itemId!!, cartItem.quantity - 1)
            }
        }

        // Change decrease button appearance based on quantity
        if (cartItem.quantity <= 1) {
            holder.decreaseButton.alpha = 0.5f
        } else {
            holder.decreaseButton.alpha = 1.0f
        }

        // Set up increase button
        holder.increaseButton.setOnClickListener {
            // Add animation
            val scaleAnimation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.button_scale)
            holder.increaseButton.startAnimation(scaleAnimation)

            onQuantityChanged(menuItem.itemId!!, cartItem.quantity + 1)
        }

        // Set up remove button
        holder.removeButton.setOnClickListener {
            val removeAnimation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.slide_out_right)
            holder.itemContainer.startAnimation(removeAnimation)

            removeAnimation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {}

                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    onItemRemoved(menuItem.itemId!!)
                }

                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            })
        }
    }

    override fun getItemCount(): Int = cartItems.size

    fun updateCartItems(newCartItems: List<CartItem>) {
        cartItems = newCartItems
        notifyDataSetChanged()
    }
}

// ItemDecoration for RecyclerView
class CartItemDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val dividerHeight = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        1f,
        context.resources.displayMetrics
    ).toInt()

    private val paint = Paint().apply {
        color = Color.LTGRAY
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight

        for (i in 0 until parent.childCount - 1) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams

            val top = child.bottom + params.bottomMargin
            val bottom = top + dividerHeight

            c.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
        }
    }
}

// SwipeToDelete implementation class
class SwipeToDeleteCallback(private val adapter: CartAdapter, context: Context) :
    RecyclerView.ItemDecoration() {

    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete)
    private val background = ColorDrawable(Color.RED)

    fun onDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
               dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top
        val iconMargin = (itemHeight - deleteIcon!!.intrinsicHeight) / 2

        // Calculate position of delete icon
        val iconTop = itemView.top + (itemHeight - deleteIcon.intrinsicHeight) / 2
        val iconBottom = iconTop + deleteIcon.intrinsicHeight

        if (dX < 0) { // Swiping to the left
            val iconLeft = itemView.right - iconMargin - deleteIcon.intrinsicWidth
            val iconRight = itemView.right - iconMargin
            deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

            background.setBounds(
                itemView.right + dX.toInt(),
                itemView.top,
                itemView.right,
                itemView.bottom
            )
        } else { // View is unSwiped
            background.setBounds(0, 0, 0, 0)
        }

        background.draw(c)
        deleteIcon.draw(c)
    }
}