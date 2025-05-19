package com.example.petukrestaurantpartner.ui.menu

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.petukrestaurantpartner.MainActivity
import com.example.petukrestaurantpartner.databinding.DialogMenuItemBinding
import com.example.petukrestaurantpartner.databinding.FragmentMenuBinding
import com.example.petukrestaurantpartner.ui.menu.MenuItem

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MenuViewModel
    private lateinit var menuAdapter: MenuAdapter

    private var restaurantId: String? = null
    private var restaurantName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(MenuViewModel::class.java)

        // Get restaurant ID and name from MainActivity
        restaurantId = (activity as? MainActivity)?.intent?.getStringExtra("RESTAURANT_ID")
        restaurantName = (activity as? MainActivity)?.intent?.getStringExtra("RESTAURANT_NAME")

        if (restaurantId == null) {
            showToast("Restaurant ID not found")
            return
        }

        setupRecyclerView()
        setupObservers()

        binding.buttonAddMenuItem.setOnClickListener {
            showAddEditMenuItemDialog()
        }

        // Load menu items
        viewModel.loadMenuItems(restaurantId!!)
    }

    private fun setupRecyclerView() {
        menuAdapter = MenuAdapter(
            onEditClick = { menuItem ->
                showAddEditMenuItemDialog(menuItem)
            },
            onDeleteClick = { menuItem ->
                showDeleteConfirmationDialog(menuItem)
            },
            onAvailabilityChanged = { menuItem, isAvailable ->
                menuItem.itemId?.let { itemId ->
                    viewModel.updateItemAvailability(itemId, isAvailable) { errorMessage ->
                        showToast(errorMessage)
                    }
                }
            }
        )

        binding.recyclerViewMenu.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = menuAdapter
        }
    }

    private fun setupObservers() {
        viewModel.menuItems.observe(viewLifecycleOwner) { items ->
            menuAdapter.submitList(items)

            // Show empty view if no items
            binding.textViewEmptyMenu.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarMenu.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                showToast(errorMessage)
            }
        }
    }

    private fun showAddEditMenuItemDialog(menuItem: MenuItem? = null) {
        val dialogBinding = DialogMenuItemBinding.inflate(LayoutInflater.from(context))
        val isEdit = menuItem != null

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // Set dialog title
        dialogBinding.textViewDialogTitle.text = if (isEdit) "Edit Menu Item" else "Add Menu Item"

        // Setup category dropdown
        val categories = arrayOf("Appetizers", "Main Course", "Desserts", "Beverages", "Sides", "Specials")
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        dialogBinding.autoCompleteCategory.setAdapter(categoryAdapter)

        // Populate fields if editing
        if (isEdit) {
            dialogBinding.apply {
                editTextItemName.setText(menuItem?.name)
                editTextItemDescription.setText(menuItem?.description)
                editTextItemPrice.setText(menuItem?.price.toString())
                autoCompleteCategory.setText(menuItem?.category, false)
                switchItemAvailableDialog.isChecked = menuItem?.isAvailable ?: true
            }
        }

        // Set button listeners
        dialogBinding.buttonCancelDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.buttonSaveMenuItem.setOnClickListener {
            val name = dialogBinding.editTextItemName.text.toString().trim()
            val description = dialogBinding.editTextItemDescription.text.toString().trim()
            val priceText = dialogBinding.editTextItemPrice.text.toString().trim()
            val category = dialogBinding.autoCompleteCategory.text.toString().trim()
            val isAvailable = dialogBinding.switchItemAvailableDialog.isChecked

            // Validate inputs
            if (name.isEmpty() || description.isEmpty() || priceText.isEmpty() || category.isEmpty()) {
                showToast("Please fill in all fields")
                return@setOnClickListener
            }

            val price = try {
                priceText.toDouble()
            } catch (e: NumberFormatException) {
                showToast("Please enter a valid price")
                return@setOnClickListener
            }

            if (isEdit) {
                // Update existing item
                val updatedItem = menuItem!!.copy(
                    name = name,
                    description = description,
                    price = price,
                    category = category,
                    isAvailable = isAvailable
                )

                viewModel.updateMenuItem(
                    updatedItem,
                    onSuccess = {
                        showToast("Menu item updated successfully")
                        dialog.dismiss()
                    },
                    onFailure = { errorMessage ->
                        showToast(errorMessage)
                    }
                )
            } else {
                // Create new item
                val newMenuItem = MenuItem(
                    restaurantId = restaurantId,
                    restaurantName = restaurantName,
                    name = name,
                    description = description,
                    price = price,
                    category = category,
                    isAvailable = isAvailable
                )

                viewModel.addMenuItem(
                    newMenuItem,
                    onSuccess = {
                        showToast("Menu item added successfully")
                        dialog.dismiss()
                    },
                    onFailure = { errorMessage ->
                        showToast(errorMessage)
                    }
                )
            }
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(menuItem: MenuItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Menu Item")
            .setMessage("Are you sure you want to delete ${menuItem.name}?")
            .setPositiveButton("Delete") { _, _ ->
                menuItem.itemId?.let { itemId ->
                    viewModel.deleteMenuItem(
                        itemId,
                        onSuccess = {
                            showToast("Menu item deleted successfully")
                        },
                        onFailure = { errorMessage ->
                            showToast(errorMessage)
                        }
                    )
                } ?: showToast("Invalid menu item ID")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}