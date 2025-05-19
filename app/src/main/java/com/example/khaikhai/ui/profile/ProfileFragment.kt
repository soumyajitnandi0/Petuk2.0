package com.example.khaikhai.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.khaikhai.LoginActivity
import com.example.khaikhai.R
import com.example.khaikhai.databinding.FragmentProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var profileOptionAdapter: ProfileOptionAdapter
    private lateinit var viewModel: ProfileViewModel

    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference.child("profile_images")

    // Activity result launcher for image selection
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadProfileImage(uri)
            }
        }
    }

    // Activity result launcher for image selection from dialog
    private val dialogImagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadProfileImage(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        // Initialize the ViewModel with context for shared preferences
        viewModel.initialize(requireContext())

        // Setup UI components
        setupProfileHeader()
        setupProfileOptions()
        setupStatsData()

        // Observe user profile data
        observeUserProfile()

        return binding.root
    }

    private fun setupProfileHeader() {
        // Setup edit profile button
        binding.btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        // Setup profile image click to change photo
        binding.imageProfile.setOnClickListener {
            openGallery()
        }

        // Setup camera button click
        binding.btnCamera.setOnClickListener {
            openGallery()
        }
    }

    private fun setupStatsData() {
        // This would typically come from a repository or ViewModel
        // For now, just setting static data for demonstration
        binding.textOrdersCount.text = "0"
        binding.textFavoritesCount.text = "0"
        binding.textRewardsCount.text = "0"
    }

    private fun observeUserProfile() {
        // Show loading state
        showLoading(true)

        // Observe user data
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.textProfileName.text = name
        }

        viewModel.userEmail.observe(viewLifecycleOwner) { email ->
            binding.textProfileEmail.text = email
        }

        viewModel.userPhone.observe(viewLifecycleOwner) { phone ->
            binding.textProfilePhone.text = phone.ifEmpty { getString(R.string.add_phone_number) }
        }

        viewModel.userBio.observe(viewLifecycleOwner) { bio ->
            binding.textProfileBio.text = bio.ifEmpty { "No bio available" }
            binding.textProfileBio.visibility = if (bio.isEmpty()) View.GONE else View.VISIBLE
        }

        // Observe the full user profile for additional data
        viewModel.userProfile.observe(viewLifecycleOwner) { customer ->
            if (customer != null) {
                // Load profile image
                loadProfileImage(customer.profilePhotoUrl)

                // Set account status chip text based on rewards points
                val points = binding.textRewardsCount.text.toString().toIntOrNull() ?: 0
                val status = when {
                    points >= 500 -> "Platinum Member"
                    points >= 200 -> "Gold Member"
                    points >= 100 -> "Silver Member"
                    else -> "Bronze Member"
                }
                binding.chipAccountStatus.text = status
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
        }
    }

    private fun loadProfileImage(imageUrl: String?) {
        val requestOptions = RequestOptions()
            .placeholder(R.drawable.ic_person_placeholder)
            .error(R.drawable.ic_person_placeholder)

        Glide.with(requireContext())
            .load(imageUrl)
            .apply(requestOptions)
            .into(binding.imageProfile)
    }

    private fun setupProfileOptions() {
        // Initialize adapter with click listener
        profileOptionAdapter = ProfileOptionAdapter { option ->
            handleProfileOptionClick(option)
        }

        // Setup RecyclerView
        binding.rvProfileOptions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = profileOptionAdapter
        }

        // Observe profile options
        viewModel.profileOptions.observe(viewLifecycleOwner) { options ->
            profileOptionAdapter.submitList(options)
        }
    }

    private fun handleProfileOptionClick(option: ProfileOption) {
        when (option.type) {
            ProfileOptionType.PERSONAL_INFO -> {
                showEditProfileDialog()
            }
            ProfileOptionType.LOGOUT -> {
                showLogoutConfirmationDialog()
            }
            ProfileOptionType.ORDER_HISTORY -> {
                // Navigate to order history or show toast
                Toast.makeText(requireContext(), "Order History coming soon!", Toast.LENGTH_SHORT).show()
            }
            ProfileOptionType.ADDRESSES -> {
                // Navigate to addresses or show toast
                Toast.makeText(requireContext(), "Addresses coming soon!", Toast.LENGTH_SHORT).show()
            }
            ProfileOptionType.FAVORITES -> {
                // Navigate to favorites or show toast
                Toast.makeText(requireContext(), "Favorites coming soon!", Toast.LENGTH_SHORT).show()
            }
            else -> {
                // Show a simple toast for other options
                Toast.makeText(requireContext(), "${option.title} feature coming soon!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditProfileDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_profile, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        // Initialize dialog components
        val etName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_name)
        val etEmail = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_email)
        val etPhone = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_phone)
        val etBio = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_bio)
        val etLocation = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_location)
        val btnSave = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_save)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_cancel)
        val btnChangePhoto = dialogView.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btn_change_photo)
        val profileImage = dialogView.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.iv_profile_image)

        // Pre-fill with current values
        etName.setText(viewModel.userName.value)
        etEmail.setText(viewModel.userEmail.value)
        etPhone.setText(viewModel.userPhone.value?.takeIf { it != getString(R.string.add_phone_number) } ?: "")
        etBio.setText(viewModel.userBio.value)

        // Load current profile image
        loadDialogProfileImage(viewModel.userProfile.value?.profilePhotoUrl, profileImage)

        // Set click listener for change photo button
        btnChangePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            dialogImagePickerLauncher.launch(intent)
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val bio = etBio.text.toString().trim()
            val location = etLocation.text.toString().trim()

            if (name.isNotEmpty()) {
                viewModel.updateUserProfile(name, phone, bio, location)
                dialog.dismiss()
                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            } else {
                etName.error = "Name cannot be empty"
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun loadDialogProfileImage(imageUrl: String?, imageView: de.hdodenhof.circleimageview.CircleImageView) {
        val requestOptions = RequestOptions()
            .placeholder(R.drawable.ic_person_placeholder)
            .error(R.drawable.ic_person_placeholder)

        Glide.with(requireContext())
            .load(imageUrl)
            .apply(requestOptions)
            .into(imageView)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun uploadProfileImage(uri: Uri) {
        showLoading(true)

        // Create a unique filename
        val filename = UUID.randomUUID().toString()
        val fileRef = storageRef.child(filename)

        fileRef.putFile(uri)
            .addOnSuccessListener {
                // Get the download URL
                fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    // Update user profile with new image URL
                    viewModel.updateProfileImage(downloadUri.toString())
                    Toast.makeText(requireContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(requireContext(), "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                // Log out user
                viewModel.logout()

                // Navigate to login screen
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.cardProfileHeader.alpha = if (isLoading) 0.5f else 1.0f
        binding.rvProfileOptions.alpha = if (isLoading) 0.5f else 1.0f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        // Refresh user data when the fragment becomes visible
        viewModel.loadUserProfile()
    }
}