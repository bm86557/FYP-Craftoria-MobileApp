package com.example.myapplication.model

data class StoreInviteModel(
    val inviteId: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val storeLogo: String = "",
    val ownerSellerId: String = "",
    val ownerSellerName: String = "",
    val ownerSellerEmail: String = "",
    val invitedSellerId: String = "",
    val invitedSellerEmail: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED
    val invitedAt: Long = System.currentTimeMillis()
)
