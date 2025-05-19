package com.example.khaikhai.ui.profile

data class ProfileOption(
    val id: Int,
    val title: String,
    val iconResId: Int,
    val type: ProfileOptionType
)

enum class ProfileOptionType {
    PERSONAL_INFO,
    PAYMENT_METHODS,
    ADDRESSES,
    ORDER_HISTORY,
    FAVORITES,
    REWARDS,
    CUSTOMER_SUPPORT,
    SETTINGS,
    LOGOUT
}