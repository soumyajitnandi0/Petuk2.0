package com.example.khaikhai.ui.profile

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.khaikhai.Customer
import com.example.khaikhai.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileViewModel : ViewModel() {


    private lateinit var appContext: Context


    private val _userProfile = MutableLiveData<Customer?>()
    val userProfile: LiveData<Customer?> = _userProfile

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _userEmail = MutableLiveData<String>()
    val userEmail: LiveData<String> = _userEmail

    private val _userPhone = MutableLiveData<String>()
    val userPhone: LiveData<String> = _userPhone

    private val _userBio = MutableLiveData<String>()
    val userBio: LiveData<String> = _userBio

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _profileOptions = MutableLiveData<List<ProfileOption>>()
    val profileOptions: LiveData<List<ProfileOption>> = _profileOptions

    private val database = FirebaseDatabase.getInstance()
    private val customersRef = database.reference.child("customers")

    private lateinit var sharedPreferences: SharedPreferences
    private var customerId: String? = null
    private var userEmailStr: String? = null

    init {
        loadProfileOptions()
    }

    fun initialize(context: Context) {
        // Get user information from shared preferences
        sharedPreferences = context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        customerId = sharedPreferences.getString("CUSTOMER_ID", null)
        userEmailStr = sharedPreferences.getString("USER_EMAIL", null)

        // Also check user_prefs as seen in CheckoutActivity
        if (userEmailStr == null) {
            val userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            userEmailStr = userPrefs.getString("user_email", null)
        }

        loadUserProfile()
    }

    private fun loadProfileOptions() {
        val options = listOf(
            ProfileOption(
                id = 1,
                title = "Personal Information",
                iconResId = R.drawable.ic_person_info,
                type = ProfileOptionType.PERSONAL_INFO
            ),
            ProfileOption(
                id = 2,
                title = "Addresses",
                iconResId = R.drawable.ic_location,
                type = ProfileOptionType.ADDRESSES
            ),
            ProfileOption(
                id = 3,
                title = "Order History",
                iconResId = R.drawable.ic_history,
                type = ProfileOptionType.ORDER_HISTORY
            ),
            ProfileOption(
                id = 4,
                title = "Favorites",
                iconResId = R.drawable.ic_favourite,
                type = ProfileOptionType.FAVORITES
            ),
            ProfileOption(
                id = 5,
                title = "Log Out",
                iconResId = R.drawable.ic_logout,
                type = ProfileOptionType.LOGOUT
            )
        )
        _profileOptions.value = options
    }

    fun loadUserProfile() {
        _isLoading.value = true

        // First try using customer ID if available
        if (customerId != null) {
            fetchUserByCustomerId(customerId!!)
        }
        // Then try using email if available
        else if (userEmailStr != null) {
            fetchUserByEmail(userEmailStr!!)
        }
        // If neither is available, show not logged in
        else {
            _isLoading.value = false
            _userName.value = "Not logged in"
            _userEmail.value = "Please log in"
            _userPhone.value = ""
            _userBio.value = ""
        }
    }

    private fun fetchUserByCustomerId(id: String) {
        customersRef.child(id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    handleUserDataSnapshot(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    handleDatabaseError(error)

                    // Try by email as fallback if available
                    if (userEmailStr != null) {
                        fetchUserByEmail(userEmailStr!!)
                    } else {
                        _isLoading.value = false
                    }
                }
            })
    }

    private fun fetchUserByEmail(email: String) {
        customersRef.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Get the first user with this email
                        for (userSnapshot in snapshot.children) {
                            // Save the customer ID for future use
                            customerId = userSnapshot.key
                            sharedPreferences.edit().putString("CUSTOMER_ID", customerId).apply()

                            // Process the user data
                            handleUserDataSnapshot(userSnapshot)
                            break
                        }
                    } else {
                        _isLoading.value = false
                        _userName.value = "User not found"
                        _userEmail.value = email
                        _userPhone.value = ""
                        _userBio.value = ""
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    handleDatabaseError(error)
                }
            })
    }

    private fun handleUserDataSnapshot(snapshot: DataSnapshot) {
        _isLoading.value = false
        if (snapshot.exists()) {
            val customer = snapshot.getValue(Customer::class.java)
            customer?.let {
                _userProfile.value = it
                _userName.value = it.fullName
                _userEmail.value = it.email
                _userPhone.value = it.phone.ifEmpty { "" }
                _userBio.value = it.bio ?: ""
            }
        } else {
            _userName.value = "User not found"
            _userEmail.value = userEmailStr ?: "Unknown email"
            _userPhone.value = ""
            _userBio.value = ""
        }
    }

    private fun handleDatabaseError(error: DatabaseError) {
        _isLoading.value = false
        _userName.value = "Error: ${error.message}"
        _userEmail.value = userEmailStr ?: "Unknown email"
        _userPhone.value = ""
        _userBio.value = ""
    }

    fun logout() {
        // Clear shared preferences
        val editor = sharedPreferences.edit()
        editor.remove("CUSTOMER_ID")
        editor.remove("USER_EMAIL")
        editor.remove("LOGIN_TIME")
        editor.putBoolean("IS_LOGGED_IN", false)
        editor.apply()

        // Also clear user_prefs if they exist
        val userPrefs = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        userPrefs.edit().remove("user_email").apply()

        // Clear user data
        customerId = null
        userEmailStr = null
        _userProfile.value = null
        _userName.value = ""
        _userEmail.value = ""
        _userPhone.value = ""
        _userBio.value = ""
    }

    fun updateUserProfile(name: String, phone: String, bio: String = "", location: String) {
        if (customerId != null) {
            val updates = hashMapOf<String, Any>(
                "fullName" to name,
                "phone" to phone,
                "bio" to bio
            )

            _isLoading.value = true
            customersRef.child(customerId!!).updateChildren(updates)
                .addOnCompleteListener { task ->
                    _isLoading.value = false
                    if (task.isSuccessful) {
                        // Update local data
                        _userName.value = name
                        _userPhone.value = phone
                        _userBio.value = bio

                        // Also update the customer object
                        _userProfile.value?.let {
                            _userProfile.value = it.copy(
                                fullName = name,
                                phone = phone,
                                bio = bio,
                                updatedAt = System.currentTimeMillis()
                            )
                        }
                    }
                }
        }
    }

    fun updateProfileImage(imageUrl: String) {
        if (customerId != null) {
            val updates = hashMapOf<String, Any>(
                "profilePhotoUrl" to imageUrl
            )

            _isLoading.value = true
            customersRef.child(customerId!!).updateChildren(updates)
                .addOnCompleteListener { task ->
                    _isLoading.value = false
                    if (task.isSuccessful) {
                        // Update the customer object
                        _userProfile.value?.let {
                            _userProfile.value = it.copy(
                                profilePhotoUrl = imageUrl
                            )
                        }
                    }
                }
        }
    }
}