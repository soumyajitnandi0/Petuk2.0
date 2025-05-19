package com.example.petukrestaurantpartner.ui.profile

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.petukrestaurantpartner.LoginActivity
import com.example.petukrestaurantpartner.MainActivity
import com.example.petukrestaurantpartner.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var viewModel: ProfileViewModel
    private var restaurantId: String? = null

    // Activity result launcher for profile edit
    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Profile updated, refresh data
            restaurantId?.let {
                viewModel.loadRestaurantData(it)
            }
            showToast("Profile updated successfully")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)

        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        restaurantId = (activity as? MainActivity)?.intent?.getStringExtra("RESTAURANT_ID")

        setupObservers()
        setupClickListeners()

        restaurantId?.let {
            viewModel.loadRestaurantData(it)
        } ?: showToast("Restaurant ID not found")
    }

    private fun setupObservers() {
        viewModel.restaurant.observe(viewLifecycleOwner) { restaurant ->
            binding.textViewRestaurantName.text = restaurant.restaurantName
            binding.textViewRestaurantEmail.text = restaurant.restaurantEmail
            binding.textViewRestaurantPhone.text = restaurant.restaurantPhone
            binding.textViewAddress.text = restaurant.restaurantAddress
            binding.textViewRestaurantCuisine.text= restaurant.cuisineType
            binding.textViewDeliveryTime.text = "Avg. Delivery: ${restaurant.averageDeliveryTime} mins"
            activity?.title = restaurant.restaurantName
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBarProfile.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) showToast(errorMessage)
        }

        viewModel.isRestaurantOpen.observe(viewLifecycleOwner) { isOpen ->
            binding.switchRestaurantStatus.isChecked = isOpen
        }
    }

    private fun setupClickListeners() {
        binding.switchRestaurantStatus.setOnCheckedChangeListener { _, isChecked ->
            restaurantId?.let {
                viewModel.updateRestaurantStatus(it, isChecked)
            }
        }

        binding.buttonEditProfile.setOnClickListener {
            viewModel.restaurant.value?.let { restaurant ->
                val intent = Intent(requireContext(), EditProfileActivity::class.java).apply {
                    putExtra("RESTAURANT_ID", restaurantId)
                    putExtra("RESTAURANT_DATA", restaurant)
                }
                editProfileLauncher.launch(intent)
            } ?: showToast("Restaurant data not loaded yet")
        }

        binding.buttonManageMenu.setOnClickListener {
            showToast("Manage Menu feature coming soon")
        }

        binding.buttonOperatingHours.setOnClickListener {
            showToast("Operating Hours feature coming soon")
        }

        binding.buttonPaymentSettings.setOnClickListener {
            showToast("Payment Settings feature coming soon")
        }

        binding.buttonAnalytics.setOnClickListener {
            showToast("Analytics feature coming soon")
        }

        binding.buttonChangePassword.setOnClickListener {
            showToast("Change Password feature coming soon")
        }

        binding.buttonLogout.setOnClickListener {
            confirmLogout()
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ -> performLogout() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun performLogout() {
        try {
            with(sharedPreferences.edit()) {
                clear()
                apply()
            }
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            navigateToLogin()
            Log.d(TAG, "User logged out successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
            Toast.makeText(requireContext(), "Logout error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(requireActivity(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}