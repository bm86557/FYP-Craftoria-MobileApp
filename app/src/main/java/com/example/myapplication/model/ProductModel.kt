package com.example.myapplication.model

import com.google.firebase.firestore.PropertyName

data class ProductModel (
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id : String ="",
    
    @get:PropertyName("sellerId")
    @set:PropertyName("sellerId")
    var sellerId : String = "",
    
    @get:PropertyName("sellerName")
    @set:PropertyName("sellerName")
    var sellerName : String = "",
    
    @get:PropertyName("title")
    @set:PropertyName("title")
    var title : String ="",
    
    @get:PropertyName("description")
    @set:PropertyName("description")
    var description : String ="",
    
    @get:PropertyName("price")
    @set:PropertyName("price")
    var price : String ="",
    
    @get:PropertyName("actualPrice")
    @set:PropertyName("actualPrice")
    var actualPrice : String ="",
    
    @get:PropertyName("category")
    @set:PropertyName("category")
    var category: String ="",
    
    @get:PropertyName("images")
    @set:PropertyName("images")
    var images : Any? = null,
    
    @get:PropertyName("otherDetails")
    @set:PropertyName("otherDetails")
    var otherDetails : Map<String,String>? = null,
    
    @get:PropertyName("minDealPrice")
    @set:PropertyName("minDealPrice")
    var minDealPrice : Int = 0,
    
    @get:PropertyName("coStoreId")
    @set:PropertyName("coStoreId")
    var coStoreId : String = "",
    
    @get:PropertyName("coStoreName")
    @set:PropertyName("coStoreName")
    var coStoreName : String = "",
    
    @get:PropertyName("isCoStoreProduct")
    @set:PropertyName("isCoStoreProduct")
    var isCoStoreProduct : Boolean = false,

    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = "pending",         // "pending" | "approved" | "rejected"

    @get:PropertyName("rejectionReason")
    @set:PropertyName("rejectionReason")
    var rejectionReason: String? = null,

    @get:PropertyName("reviewedBy")
    @set:PropertyName("reviewedBy")
    var reviewedBy: String? = null,

    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: com.google.firebase.Timestamp? = null,

    @get:PropertyName("isFeatured")
    @set:PropertyName("isFeatured")
    var isFeatured: Boolean = false

) {
    fun getImagesList(): List<String> {
        return when (images) {
            is List<*> -> (images as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            is String -> if ((images as String).isNotBlank()) listOf(images as String) else emptyList()
            else -> emptyList()
        }
    }
    
    fun getOtherDetailsMap(): Map<String, String> {
        return otherDetails ?: emptyMap()
    }
}
