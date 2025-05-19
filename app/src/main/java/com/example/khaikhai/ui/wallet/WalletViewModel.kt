package com.example.khaikhai.ui.wallet

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val walletManager = WalletManager(application.applicationContext)
    private val sharedPreferences = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val database = FirebaseDatabase.getInstance()
    private val customersRef = database.getReference("customers")

    private val _cashbackBalance = MutableLiveData<String>()
    val cashbackBalance: LiveData<String> = _cashbackBalance

    private val _transactionList = MutableLiveData<List<Transaction>>()
    val transactionList: LiveData<List<Transaction>> = _transactionList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        // Load wallet data when ViewModel is created
        loadWalletData()
    }

    // Get the current user's email
    private fun getCurrentUserEmail(): String? {
        return sharedPreferences.getString("user_email", null)
    }

    // Find customer ID from email
    private fun findCustomerIdByEmail(email: String, callback: (String?) -> Unit) {
        customersRef.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Get the customer ID from the first matching record
                        val customerId = snapshot.children.firstOrNull()?.key
                        callback(customerId)
                    } else {
                        callback(null)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(null)
                }
            })
    }

    // Load wallet data from Firebase
    private fun loadWalletData() {
        _isLoading.value = true
        _error.value = null

        val userEmail = getCurrentUserEmail()

        if (userEmail != null) {
            // First find the customer ID using email
            findCustomerIdByEmail(userEmail) { customerId ->
                if (customerId != null) {
                    // Get wallet balance
                    walletManager.getWalletBalance(
                        customerId = customerId,
                        onSuccess = { balance ->
                            _cashbackBalance.value = "$${String.format("%.2f", balance)}"

                            // After getting balance, load transactions
                            loadTransactions(customerId)
                        },
                        onFailure = { e ->
                            _error.value = "Failed to load wallet: ${e.message}"
                            _isLoading.value = false

                            // Set default values if error occurs
                            _cashbackBalance.value = "$0.00"
                            _transactionList.value = emptyList()
                        }
                    )
                } else {
                    // Customer not found with this email
                    _error.value = "Customer account not found"
                    _cashbackBalance.value = "$0.00"
                    _transactionList.value = emptyList()
                    _isLoading.value = false
                }
            }
        } else {
            // No user email in SharedPreferences
            _error.value = "User not signed in"
            _cashbackBalance.value = "$0.00"
            _transactionList.value = emptyList()
            _isLoading.value = false
        }
    }

    // Load transaction history
    private fun loadTransactions(customerId: String) {
        walletManager.getTransactionHistory(
            customerId = customerId,
            onSuccess = { transactions ->
                _transactionList.value = transactions
                _isLoading.value = false
            },
            onFailure = { e ->
                _error.value = "Failed to load transactions: ${e.message}"
                _transactionList.value = emptyList()
                _isLoading.value = false
            }
        )
    }

    // Redeem cashback
    fun redeemCashback(amount: Double) {
        _isLoading.value = true
        _error.value = null

        val userEmail = getCurrentUserEmail()

        if (userEmail != null) {
            findCustomerIdByEmail(userEmail) { customerId ->
                if (customerId != null) {
                    walletManager.redeemCashback(
                        customerId = customerId,
                        amount = amount,
                        onSuccess = {
                            // Reload wallet data after redemption
                            loadWalletData()
                        },
                        onFailure = { e ->
                            _error.value = "Failed to redeem cashback: ${e.message}"
                            _isLoading.value = false
                        }
                    )
                } else {
                    _error.value = "Customer account not found"
                    _isLoading.value = false
                }
            }
        } else {
            _error.value = "User not signed in"
            _isLoading.value = false
        }
    }

    // Refresh wallet data (can be called from fragment)
    fun refreshWalletData() {
        loadWalletData()
    }
}