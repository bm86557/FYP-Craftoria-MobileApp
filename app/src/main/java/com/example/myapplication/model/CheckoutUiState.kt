package com.example.myapplication.model

data class CheckoutUiState(
    val isLoading: Boolean = false,
    val orderId: String? = null,
    val clientSecret: String? = null,
    val paymentIntentId: String? = null,
    val error: String? = null
)
