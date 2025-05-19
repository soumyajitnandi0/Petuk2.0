package com.example.khaikhai

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.khaikhai.databinding.ActivityAddressBinding
import com.google.firebase.database.*

class AddressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddressBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var addressAdapter: AddressAdapter
    private var currentUserId: String? = null
    private var addresses = mutableListOf<Address>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("customers")

        // Get current user email from SharedPreferences
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("user_email", null)

        if (userEmail == null) {
            showToast("User not logged in")
            finish()
            return
        }

        // Set up RecyclerView
        setupRecyclerView()

        // Find user ID by email
        findUserByEmail(userEmail)

        // Set up click listeners
        binding.btnAddAddress.setOnClickListener {
            showAddAddressDialog()
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        addressAdapter = AddressAdapter(
            addresses,
            onEditClick = { address -> showEditAddressDialog(address) },
            onDeleteClick = { address -> deleteAddress(address) },
            onSetDefaultClick = { address -> setDefaultAddress(address) }
        )

        binding.recyclerViewAddresses.apply {
            layoutManager = LinearLayoutManager(this@AddressActivity)
            adapter = addressAdapter
        }
    }

    private fun findUserByEmail(email: String) {
        binding.progressBar.visibility = View.VISIBLE

        databaseReference.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.progressBar.visibility = View.GONE

                    if (!snapshot.exists()) {
                        showToast("User not found")
                        return
                    }

                    // Get the first (and hopefully only) user with this email
                    for (userSnapshot in snapshot.children) {
                        currentUserId = userSnapshot.key

                        // Load existing addresses
                        val customer = userSnapshot.getValue(Customer::class.java)
                        customer?.addresses?.let {
                            addresses.clear()
                            addresses.addAll(it)
                            addressAdapter.notifyDataSetChanged()
                        }

                        binding.emptyView.visibility = if (addresses.isEmpty()) View.VISIBLE else View.GONE

                        break
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar.visibility = View.GONE
                    showToast("Error: ${error.message}")
                }
            })
    }

    private fun showAddAddressDialog() {
        if (currentUserId == null) {
            showToast("User ID not found")
            return
        }

        val dialogBinding = layoutInflater.inflate(R.layout.dialog_add_address, null)

        AlertDialog.Builder(this)
            .setTitle("Add New Address")
            .setView(dialogBinding)
            .setPositiveButton("Save") { dialog, _ ->
                val etLabel = dialogBinding.findViewById<android.widget.EditText>(R.id.etAddressLabel)
                val etStreet = dialogBinding.findViewById<android.widget.EditText>(R.id.etStreet)
                val etCity = dialogBinding.findViewById<android.widget.EditText>(R.id.etCity)
                val etState = dialogBinding.findViewById<android.widget.EditText>(R.id.etState)
                val etZipCode = dialogBinding.findViewById<android.widget.EditText>(R.id.etZipCode)
                val etCountry = dialogBinding.findViewById<android.widget.EditText>(R.id.etCountry)
                val cbDefault = dialogBinding.findViewById<android.widget.CheckBox>(R.id.cbDefaultAddress)

                val label = etLabel.text.toString().trim()
                val street = etStreet.text.toString().trim()
                val city = etCity.text.toString().trim()
                val state = etState.text.toString().trim()
                val zipCode = etZipCode.text.toString().trim()
                val country = etCountry.text.toString().trim()
                val isDefault = cbDefault.isChecked

                if (label.isEmpty() || street.isEmpty() || city.isEmpty() ||
                    state.isEmpty() || zipCode.isEmpty() || country.isEmpty()) {
                    showToast("All fields are required")
                    return@setPositiveButton
                }

                addAddress(
                    Address(
                        id = databaseReference.push().key,
                        label = label,
                        street = street,
                        city = city,
                        state = state,
                        zipCode = zipCode,
                        country = country,
                        isDefault = isDefault
                    )
                )

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showEditAddressDialog(address: Address) {
        if (currentUserId == null) {
            showToast("User ID not found")
            return
        }

        val dialogBinding = layoutInflater.inflate(R.layout.dialog_add_address, null)

        // Pre-fill the fields
        dialogBinding.findViewById<android.widget.EditText>(R.id.etAddressLabel).setText(address.label)
        dialogBinding.findViewById<android.widget.EditText>(R.id.etStreet).setText(address.street)
        dialogBinding.findViewById<android.widget.EditText>(R.id.etCity).setText(address.city)
        dialogBinding.findViewById<android.widget.EditText>(R.id.etState).setText(address.state)
        dialogBinding.findViewById<android.widget.EditText>(R.id.etZipCode).setText(address.zipCode)
        dialogBinding.findViewById<android.widget.EditText>(R.id.etCountry).setText(address.country)
        dialogBinding.findViewById<android.widget.CheckBox>(R.id.cbDefaultAddress).isChecked = address.isDefault

        AlertDialog.Builder(this)
            .setTitle("Edit Address")
            .setView(dialogBinding)
            .setPositiveButton("Update") { dialog, _ ->
                val etLabel = dialogBinding.findViewById<android.widget.EditText>(R.id.etAddressLabel)
                val etStreet = dialogBinding.findViewById<android.widget.EditText>(R.id.etStreet)
                val etCity = dialogBinding.findViewById<android.widget.EditText>(R.id.etCity)
                val etState = dialogBinding.findViewById<android.widget.EditText>(R.id.etState)
                val etZipCode = dialogBinding.findViewById<android.widget.EditText>(R.id.etZipCode)
                val etCountry = dialogBinding.findViewById<android.widget.EditText>(R.id.etCountry)
                val cbDefault = dialogBinding.findViewById<android.widget.CheckBox>(R.id.cbDefaultAddress)

                val label = etLabel.text.toString().trim()
                val street = etStreet.text.toString().trim()
                val city = etCity.text.toString().trim()
                val state = etState.text.toString().trim()
                val zipCode = etZipCode.text.toString().trim()
                val country = etCountry.text.toString().trim()
                val isDefault = cbDefault.isChecked

                if (label.isEmpty() || street.isEmpty() || city.isEmpty() ||
                    state.isEmpty() || zipCode.isEmpty() || country.isEmpty()) {
                    showToast("All fields are required")
                    return@setPositiveButton
                }

                updateAddress(
                    address.copy(
                        label = label,
                        street = street,
                        city = city,
                        state = state,
                        zipCode = zipCode,
                        country = country,
                        isDefault = isDefault
                    )
                )

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun addAddress(address: Address) {
        binding.progressBar.visibility = View.VISIBLE

        // If this is the first address or marked as default, make sure other addresses are not default
        if (address.isDefault || addresses.isEmpty()) {
            addresses.forEach { it.isDefault = false }
        }

        // Add the new address
        addresses.add(address)

        // Update the database
        updateAddressesInDatabase()
    }

    private fun updateAddress(updatedAddress: Address) {
        binding.progressBar.visibility = View.VISIBLE

        // Find the address in the list
        val index = addresses.indexOfFirst { it.id == updatedAddress.id }
        if (index == -1) {
            binding.progressBar.visibility = View.GONE
            showToast("Address not found")
            return
        }

        // If this address is being set as default, unset other defaults
        if (updatedAddress.isDefault) {
            addresses.forEach { it.isDefault = false }
        }

        // Update the address in the list
        addresses[index] = updatedAddress

        // Update the database
        updateAddressesInDatabase()
    }

    private fun deleteAddress(address: Address) {
        AlertDialog.Builder(this)
            .setTitle("Delete Address")
            .setMessage("Are you sure you want to delete this address?")
            .setPositiveButton("Delete") { _, _ ->
                binding.progressBar.visibility = View.VISIBLE

                // Remove from list
                addresses.removeIf { it.id == address.id }

                // Update database
                updateAddressesInDatabase()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setDefaultAddress(address: Address) {
        binding.progressBar.visibility = View.VISIBLE

        // Update isDefault flag for all addresses
        addresses.forEach { it.isDefault = (it.id == address.id) }

        // Update database
        updateAddressesInDatabase()
    }

    private fun updateAddressesInDatabase() {
        if (currentUserId == null) {
            binding.progressBar.visibility = View.GONE
            showToast("User ID not found")
            return
        }

        // Make sure at least one address is default if we have addresses
        if (addresses.isNotEmpty() && addresses.none { it.isDefault }) {
            addresses[0].isDefault = true
        }

        databaseReference.child(currentUserId!!).child("addresses").setValue(addresses)
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE

                if (task.isSuccessful) {
                    addressAdapter.notifyDataSetChanged()
                    binding.emptyView.visibility = if (addresses.isEmpty()) View.VISIBLE else View.GONE
                    showToast("Addresses updated successfully")
                } else {
                    showToast("Failed to update addresses: ${task.exception?.message}")
                }
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}