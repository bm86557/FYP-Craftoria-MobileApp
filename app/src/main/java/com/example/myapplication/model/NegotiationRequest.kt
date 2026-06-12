package com.example.myapplication.model

data class NegotiationRequest(
    val requestId : String = "",
    val productId : String = "",
    val buyerId : String = "",
    val sellerId : String = "",
    val offeredPrice : Int = 0,
    val status : String = "pending",
    val timestamp : Long = 0L
)
