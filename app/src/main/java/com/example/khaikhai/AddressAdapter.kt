package com.example.khaikhai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AddressAdapter(
    private val addresses: List<Address>,
    private val onEditClick: (Address) -> Unit,
    private val onDeleteClick: (Address) -> Unit,
    private val onSetDefaultClick: (Address) -> Unit
) : RecyclerView.Adapter<AddressAdapter.AddressViewHolder>() {

    inner class AddressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLabel: TextView = itemView.findViewById(R.id.tvAddressLabel)
        val tvFullAddress: TextView = itemView.findViewById(R.id.tvFullAddress)
        val tvDefault: TextView = itemView.findViewById(R.id.tvDefault)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditAddress)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteAddress)
        val btnSetDefault: ImageButton = itemView.findViewById(R.id.btnSetDefault)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_address, parent, false)
        return AddressViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        val address = addresses[position]

        holder.tvLabel.text = address.label
        holder.tvFullAddress.text = formatAddress(address)

        // Show/hide default indicator
        if (address.isDefault) {
            holder.tvDefault.visibility = View.VISIBLE
            holder.btnSetDefault.visibility = View.GONE
        } else {
            holder.tvDefault.visibility = View.GONE
            holder.btnSetDefault.visibility = View.VISIBLE
        }

        // Set click listeners
        holder.btnEdit.setOnClickListener { onEditClick(address) }
        holder.btnDelete.setOnClickListener { onDeleteClick(address) }
        holder.btnSetDefault.setOnClickListener { onSetDefaultClick(address) }
    }

    override fun getItemCount(): Int = addresses.size

    private fun formatAddress(address: Address): String {
        return "${address.street}, ${address.city}, ${address.state} ${address.zipCode}, ${address.country}"
    }
}