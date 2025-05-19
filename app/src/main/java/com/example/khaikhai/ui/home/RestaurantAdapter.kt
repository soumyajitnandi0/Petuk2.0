package com.example.khaikhai.ui.home

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.khaikhai.R
import com.example.khaikhai.ui.menu.MenuActivity

class RestaurantAdapter(
    private val context: Context,
    private var restaurants: List<Restaurant>
) : RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder>() {

    class RestaurantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.card_restaurant)
        val imageView: ImageView = itemView.findViewById(R.id.iv_restaurant)
        val nameTextView: TextView = itemView.findViewById(R.id.tv_restaurant_name)
        val cuisineTextView: TextView = itemView.findViewById(R.id.tv_cuisine)
        val ratingTextView: TextView = itemView.findViewById(R.id.tv_rating)
        val deliveryTimeTextView: TextView = itemView.findViewById(R.id.tv_delivery_time)
        val deliveryFeeTextView: TextView = itemView.findViewById(R.id.tv_delivery_fee)
        val orderButton: Button = itemView.findViewById(R.id.btn_order_now)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_restaurant, parent, false)
        return RestaurantViewHolder(view)
    }

    override fun onBindViewHolder(holder: RestaurantViewHolder, position: Int) {
        val restaurant = restaurants[position]

        holder.nameTextView.text = restaurant.name
        holder.cuisineTextView.text = restaurant.cuisine
        holder.ratingTextView.text = restaurant.rating.toString()
        holder.deliveryTimeTextView.text = restaurant.deliveryTime
        holder.deliveryFeeTextView.text = restaurant.deliveryFee
        holder.imageView.setImageResource(restaurant.imageResId)

        // Set click listener to open restaurant menu
//        holder.cardView.setOnClickListener {
//            val intent = Intent(context, MenuActivity::class.java).apply {
//                putExtra("RESTAURANT_ID", restaurant.id)
//                putExtra("RESTAURANT_NAME", restaurant.name)
//            }
//            context.startActivity(intent)
//        }

        holder.orderButton.setOnClickListener {
            val intent = Intent(context, MenuActivity::class.java).apply {
                putExtra("RESTAURANT_ID", restaurant.id)
                putExtra("RESTAURANT_NAME", restaurant.name)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = restaurants.size

    fun updateData(newRestaurants: List<Restaurant>) {
        this.restaurants = newRestaurants
        notifyDataSetChanged()
    }
}