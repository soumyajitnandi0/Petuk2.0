package com.example.khaikhai.ui.wallet

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class WalletManager(private val context: Context) {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val ordersRef: DatabaseReference = database.getReference("orders")
    private val transactionsRef: DatabaseReference = database.getReference("wallet_transactions")
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    // Generate a numeric ID
    private fun generateNumericId(): Int {
        return Random.nextInt(100000, 999999)
    }

    // Add cashback to user wallet
    fun addCashback(
        customerId: String,
        orderId: String,
        restaurantName: String,
        orderAmount: Double,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            // Calculate 5% cashback amount
            val cashbackAmount = orderAmount * 0.05

            // Format to 2 decimal places
            val formattedCashback = String.format("%.2f", cashbackAmount)

            // Generate a unique numeric ID for the transaction
            val numericId = generateNumericId()

            // Get current date
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            // Create transaction object
            val transaction = Transaction(
                id = numericId,
                restaurantName = restaurantName,
                date = currentDate,
                amount = formattedCashback,  // Remove $ prefix, we'll add it in the view
                isReward = false
            )

            // Update wallet balance
            updateWalletBalance(customerId, cashbackAmount, transaction, onSuccess, onFailure)

        } catch (e: Exception) {
            onFailure(e)
        }
    }

    // Update wallet balance and add transaction
    private fun updateWalletBalance(
        customerId: String,
        amount: Double,
        transaction: Transaction,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        getWalletBalance(customerId, { currentBalance ->
            // Calculate new balance
            val newBalance = currentBalance + amount

            // Store transaction which includes the balance update
            saveTransaction(customerId, transaction, {
                // Transaction saved successfully
                onSuccess()
            }, onFailure)

        }, onFailure)
    }

    // Save transaction to database
    private fun saveTransaction(
        customerId: String,
        transaction: Transaction,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val customerTransactionsRef = transactionsRef.child(customerId)

        // Push transaction to database
        customerTransactionsRef.push().setValue(transaction)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    // Get wallet balance by calculating from orders
    fun getWalletBalance(
        customerId: String,
        onSuccess: (Double) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // Get all transactions to calculate current balance
        getTransactionHistory(customerId, { transactions ->
            var balance = 0.0

            // Sum up all transactions (positive for cashback, negative for redemptions)
            for (transaction in transactions) {
                val amount = transaction.amount.toDoubleOrNull() ?: 0.0
                if (transaction.isReward) {
                    balance -= amount // Subtract for redemptions
                } else {
                    balance += amount // Add for earned cashback
                }
            }

            // Also check for any unprocessed cashback in orders
            ordersRef.orderByChild("customerId").equalTo("-OQ817LJeV2h_DmkR5kA")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (orderSnapshot in snapshot.children) {
                                // Add any order amounts that should contribute to cashback
                                val orderAmount = orderSnapshot.child("totalAmount").getValue(Double::class.java) ?: 0.0

                                // Check if this order has been processed for cashback already
                                // If you have a field in orders to track this, check it here
                                // For now, we'll assume all orders contribute to cashback

                                // Add 5% cashback from each order
                                balance += orderAmount * 0.05
                            }
                        }

                        onSuccess(balance)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // If we can't get orders, still return the calculated balance from transactions
                        onSuccess(balance)
                    }
                })
        }, { error ->
            // If we can't get transactions, check if we can at least get orders
            ordersRef.orderByChild("customerId").equalTo("-OQ817LJeV2h_DmkR5kA")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var balance = 0.0

                        if (snapshot.exists()) {
                            for (orderSnapshot in snapshot.children) {
                                val orderAmount = orderSnapshot.child("totalAmount").getValue(Double::class.java) ?: 0.0
                                balance += orderAmount * 0.05
                            }
                        }

                        onSuccess(balance)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        onFailure(Exception(error.message))
                    }
                })
        })
    }

    // Get transaction history
    fun getTransactionHistory(
        customerId: String,
        onSuccess: (List<Transaction>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        transactionsRef.child(customerId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val transactions = mutableListOf<Transaction>()

                    for (transactionSnapshot in snapshot.children) {
                        try {
                            val transaction = transactionSnapshot.getValue(Transaction::class.java)
                            transaction?.let { transactions.add(it) }
                        } catch (e: Exception) {
                            // Log conversion error but continue with other transactions
                            e.printStackTrace()
                        }
                    }

                    // Sort transactions by ID (most recent first)
                    transactions.sortByDescending { it.id }

                    onSuccess(transactions)
                }

                override fun onCancelled(error: DatabaseError) {
                    onFailure(Exception(error.message))
                }
            })
    }

    // Redeem cashback
    fun redeemCashback(
        customerId: String,
        amount: Double,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // First check if user has enough balance
        getWalletBalance(customerId, { balance ->
            if (balance >= amount) {
                // Generate a unique numeric ID for the transaction
                val numericId = generateNumericId()

                // Get current date
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                val currentDate = dateFormat.format(Date())

                // Create redemption transaction
                val transaction = Transaction(
                    id = numericId,
                    restaurantName = "Cashback Redemption",
                    date = currentDate,
                    amount = String.format("%.2f", amount),  // Store without $ prefix
                    isReward = true
                )

                // Add negative transaction (representing the redemption)
                saveTransaction(customerId, transaction, onSuccess, onFailure)
            } else {
                onFailure(Exception("Insufficient balance"))
            }
        }, onFailure)
    }
}