package com.example.myapplication

import android.content.Context

import android.widget.Toast
import androidx.core.content.edit

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore

object AppUtil {
    fun showToast(context: Context,message: String){
        Toast.makeText(context,message,Toast.LENGTH_LONG).show()
    }
    // We get userdoc bcz we want to update data it using cart but if simply get ad display data so we can get usermodel instead of this
    fun addItemToCart(productId: String,context: Context){
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            showToast(context, "Please login first")
            return
        }
        val userDoc = Firebase.firestore.collection("users")
            .document(uid)
        userDoc.get().addOnCompleteListener {
            if(it.isSuccessful) {
                val currentCart = it.result.get("cartItems") as? Map<String, Long> ?: emptyMap()
                val currentQuantity = currentCart[productId]?:0
                val updateQuantity = currentQuantity + 1

                val updatedCart = mapOf("cartItems.$productId" to updateQuantity)
                userDoc.update(updatedCart).addOnCompleteListener {
                    if (it.isSuccessful){
                        showToast(context,"Item Added to Cart")
                    }
                    else{
                        showToast(context,"Failed Added Item To Cart")
                    }
                }
            }
            }
        }


    fun removeItemToCart(productId: String,context: Context,removeAll: Boolean= false){
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            showToast(context, "Please login first")
            return
        }
        val userDoc = Firebase.firestore.collection("users")
            .document(uid)
        userDoc.get().addOnCompleteListener {
            if(it.isSuccessful) {
                val currentCart = it.result.get("cartItems") as? Map<String, Long> ?: emptyMap()
                val currentQuantity = currentCart[productId]?:0
                val updateQuantity = currentQuantity - 1


                val updatedCart = if (updateQuantity<=0 || removeAll){
                    mapOf("cartItems.$productId" to FieldValue.delete())}
                else{
                 mapOf("cartItems.$productId" to updateQuantity)}
                userDoc.update(updatedCart).addOnCompleteListener {
                    if (it.isSuccessful){
                        showToast(context,"Item Removed to Cart")
                    }
                    else{
                        showToast(context,"Failed Remove Item To Cart")
                    }
                }
            }
        }
    }


    fun getDuscountPercentage() : Float {
        // ✅ REMOVED: No discount for buyers
        return 0.0f
    }
    
    // Dynamic commission fetching from Firestore
    fun getCommissionPercentage(
        onSuccess: (Float) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        Firebase.firestore.collection("system_settings")
            .document("commission")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val enabled = document.getBoolean("commissionEnabled") ?: true
                    val rate = document.getDouble("commissionRate")?.toFloat() ?: 5.0f
                    
                    // If commission is disabled, return 0%
                    val finalRate = if (enabled) rate else 0.0f
                    onSuccess(finalRate)
                } else {
                    // Default to 5% if no settings found
                    onSuccess(5.0f)
                }
            }
            .addOnFailureListener { exception ->
                // Default to 5% on error
                onError(exception.message ?: "Failed to fetch commission rate")
                onSuccess(5.0f)
            }
    }
    
    // Synchronous version for backward compatibility (returns default 5%)
    fun getCommissionPercentageSync() : Float {
        return 5.0f
    }
    
    // Check if commission should apply to negotiated prices
    fun shouldApplyCommissionToNegotiated(
        onResult: (Boolean) -> Unit
    ) {
        Firebase.firestore.collection("system_settings")
            .document("commission")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val enabled = document.getBoolean("commissionEnabled") ?: true
                    val applyToNegotiated = document.getBoolean("applyToNegotiated") ?: true
                    onResult(enabled && applyToNegotiated)
                } else {
                    onResult(true) // Default: apply to negotiated
                }
            }
            .addOnFailureListener {
                onResult(true) // Default on error
            }
    }

    private const val PREF_NAME = "favourite_pref"
 private const val KEY_FAVOURITES = "favourite_list"

    fun addOrRemoveFromFavourite(context: Context,productId : String){
        val list = getFavouriteList(context).toMutableSet()
         if (list.contains(productId)) {
            list.remove(productId)
             showToast(context,"Item Removed From Favourite")
         }else{
           list.add(productId)
             showToast(context,"Item Added to Favourite")
        }
        val prefs = context.getSharedPreferences(PREF_NAME,Context.MODE_PRIVATE)
        prefs.edit {
            putStringSet(KEY_FAVOURITES,list)
        }
        }
    fun checkFavourite(context: Context,productId : String): Boolean{
        if (getFavouriteList(context).contains(productId)){
            return true;
        }
        return false
    }

    fun getFavouriteList(context: Context) : Set<String>{
        val prefs = context.getSharedPreferences(PREF_NAME,Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_FAVOURITES,emptySet()) ?: emptySet()
    }
    
    fun clearUserSession() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            // Handle error silently
        }
    }
    
    fun isUserLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }
    
    fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

}





