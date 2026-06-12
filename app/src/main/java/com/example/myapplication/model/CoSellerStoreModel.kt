package com.example.myapplication.model

data class CoSellerStoreModel(
    val storeId : String = "",
    val storeName : String= "",
    val ownerSellerId : String = "",
    val ownerSellerEmail : String = "",
    val ownerSellerName : String = "",
    
    // ✅ NEW: Multiple co-sellers support (unlimited)
    val coSellerIds: List<String> = emptyList(),
    val coSellerEmails: List<String> = emptyList(),
    val coSellerNames: List<String> = emptyList(),
    
    // ✅ DEPRECATED: Keep for backward compatibility with existing stores
    @Deprecated("Use coSellerIds instead")
    val coSellerId : String= "",
    @Deprecated("Use coSellerEmails instead")
    val coSellerEmail : String= "",
    @Deprecated("Use coSellerNames instead")
    val coSellerName : String = "",
    
    val inviteCode: String = "",
    val status : String = "ACTIVE", // ACTIVE, PENDING_INVITE
    val storeLogo : String = "",
    val storeBanner : String ="",
    val storeDescription : String ="",
    val memberCount: Int = 1  // 1 owner + co-sellers count
)

