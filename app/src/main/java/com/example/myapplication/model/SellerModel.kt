package com.example.myapplication.model

/**
 * Model for basic seller information used in lists and browsing
 */
data class SellerInfo(
    val sellerId: String = "",
    val name: String = "",
    val email: String = "",
    val profileImage: String = ""
)

/**
 * Model for detailed seller profile information
 */
data class SellerProfile(
    val sellerId: String = "",
    val name: String = "",
    val email: String = "",
    val profileImage: String = "",
    val role: String = "seller"
)
