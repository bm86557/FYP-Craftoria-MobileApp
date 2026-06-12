package com.example.myapplication.model

data class UserModel (
    val name: String="",
     val email : String="",
    val uid : String="",
    val cartItems : Map<String, Long> = emptyMap(),
   val role : String ="",
   val profileImage : String = "",
    val address : String = "",
    val walletBalance :   Double = 0.0
)