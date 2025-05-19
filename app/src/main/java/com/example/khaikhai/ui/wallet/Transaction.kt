package com.example.khaikhai.ui.wallet

data class Transaction(
    val id: Int = 0,
    val restaurantName: String = "",
    val date: String = "",
    val amount: String = "",
    val isReward: Boolean = false
)