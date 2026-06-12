package com.example.myapplication.model

import androidx.lifecycle.ViewModel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
class CheckOutViewModel : ViewModel() {

    // ✅ LOCAL BACKEND - Computer IP Address
    private val BASE_URL = "http://192.168.1.9:3000"
    private val PKR_TO_USD = 278.0
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState = _uiState.asStateFlow()
    
    // ✅ Fetch commission rate from Firestore
    private suspend fun getCommissionRate(): Double {
        return try {
            val doc = db.collection("system_settings")
                .document("commission")
                .get()
                .await()
            
            if (doc.exists()) {
                val enabled = doc.getBoolean("commissionEnabled") ?: true
                val rate = doc.getDouble("commissionRate") ?: 5.0
                
                android.util.Log.d("CheckOutViewModel", "📊 Commission Settings: enabled=$enabled, rate=$rate%")
                
                // If disabled, return 0%
                if (!enabled) {
                    android.util.Log.d("CheckOutViewModel", "⚠️ Commission is DISABLED - Using 0%")
                    0.0
                } else {
                    rate / 100.0 // Convert percentage to decimal
                }
            } else {
                android.util.Log.d("CheckOutViewModel", "⚠️ No commission settings found - Using default 5%")
                0.05 // Default 5%
            }
        } catch (e: Exception) {
            android.util.Log.e("CheckOutViewModel", "❌ Error fetching commission rate: ${e.message}")
            0.05 // Default 5% on error
        }
    }

    // ✅ NEW: Multi-seller order placement
    fun placeMultiSellerOrder(
        sellerGroups: Map<String, List<Map<String, Any>>>,
        totalPKR: Float,
        address: Map<String, String>,
        paymentMethod: String,
        stripePaymentIntentId: String = "",
        onSuccess: (List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val buyerId = auth.currentUser?.uid ?: ""
                val orderIds = mutableListOf<String>()
                
                android.util.Log.d("CheckOutViewModel", "=== PLACING MULTI-SELLER ORDER ===")
                android.util.Log.d("CheckOutViewModel", "Buyer ID: $buyerId")
                android.util.Log.d("CheckOutViewModel", "Total PKR: $totalPKR")
                android.util.Log.d("CheckOutViewModel", "Payment Method: $paymentMethod")
                android.util.Log.d("CheckOutViewModel", "Seller Groups: ${sellerGroups.size}")
                
                // Balance check for wallet payment
                if (paymentMethod == "wallet") {
                    val buyerRef = db.collection("users").document(buyerId)
                    val currentBalance = buyerRef.get().await().getDouble("walletBalance") ?: 0.0
                    
                    android.util.Log.d("CheckOutViewModel", "Wallet Balance: $currentBalance")
                    android.util.Log.d("CheckOutViewModel", "Required Amount: $totalPKR")
                    
                    if (currentBalance < totalPKR) {
                        android.util.Log.e("CheckOutViewModel", "Insufficient balance!")
                        onError("Insufficient balance! You have Rs. ${String.format("%.2f", currentBalance)} but need Rs. ${String.format("%.2f", totalPKR)}")
                        return@launch
                    }
                }
                
                // ✅ Create separate order for each seller
                sellerGroups.forEach { (sellerId, items) ->
                    android.util.Log.d("CheckOutViewModel", "--- Processing Seller: $sellerId ---")
                    android.util.Log.d("CheckOutViewModel", "Items count: ${items.size}")
                    
                    // Log each item
                    items.forEachIndexed { index, item ->
                        android.util.Log.d("CheckOutViewModel", "Item $index:")
                        android.util.Log.d("CheckOutViewModel", "  - productId: ${item["productId"]}")
                        android.util.Log.d("CheckOutViewModel", "  - productName: ${item["productName"]}")
                        android.util.Log.d("CheckOutViewModel", "  - price: ${item["price"]}")
                        android.util.Log.d("CheckOutViewModel", "  - quantity: ${item["quantity"]}")
                    }
                    
                    // Calculate this seller's total
                    val sellerTotal = items.sumOf { item ->
                        val priceStr = item["price"] as? String
                        val price = priceStr?.toDoubleOrNull() ?: 0.0
                        val quantity = (item["quantity"] as? Int) ?: 0
                        val itemTotal = price * quantity
                        
                        android.util.Log.d("CheckOutViewModel", "  Item calculation: price=$priceStr ($price) * qty=$quantity = $itemTotal")
                        
                        itemTotal
                    }
                    
                    // ✅ Fetch dynamic commission rate
                    val commissionRate = getCommissionRate()
                    val commission = sellerTotal * commissionRate
                    val sellerAmount = sellerTotal - commission
                    
                    android.util.Log.d("CheckOutViewModel", "--- Seller: $sellerId ---")
                    android.util.Log.d("CheckOutViewModel", "Seller Total: $sellerTotal")
                    android.util.Log.d("CheckOutViewModel", "Commission Rate: ${commissionRate * 100}%")
                    android.util.Log.d("CheckOutViewModel", "Commission Amount: $commission")
                    android.util.Log.d("CheckOutViewModel", "Seller Amount: $sellerAmount")
                    
                    val orderRef = db.collection("orders").document()
                    val order = hashMapOf(
                        "orderId" to orderRef.id,
                        "buyerId" to buyerId,
                        "sellerId" to sellerId,
                        "items" to items,
                        "address" to address,
                        
                        // ✅ NEW: Detailed payment breakdown
                        "totalAmountPKR" to sellerTotal,
                        "platformCommission" to commission,
                        "sellerAmount" to sellerAmount,
                        
                        "paymentMethod" to paymentMethod,
                        "stripePaymentIntentId" to stripePaymentIntentId,
                        
                        "status" to "PAYMENT_CONFIRMED",
                        "paymentStatus" to if (paymentMethod == "cash_on_delivery") "PENDING" else "PAID",
                        
                        "createdAt" to FieldValue.serverTimestamp(),
                        "paymentConfirmedAt" to FieldValue.serverTimestamp(),
                        "lastUpdatedAt" to FieldValue.serverTimestamp(),
                        
                        "trackingNumber" to "",
                        "courierService" to "",
                        "estimatedDeliveryDate" to null,
                        
                        "isMultiSellerOrder" to (sellerGroups.size > 1),
                        "totalOrderAmount" to totalPKR
                    )
                    
                    orderRef.set(order).await()
                    orderIds.add(orderRef.id)
                    android.util.Log.d("CheckOutViewModel", "Order created: ${orderRef.id}")
                }
                
                // ✅ Deduct wallet for wallet payment (only once for all orders)
                if (paymentMethod == "wallet") {
                    android.util.Log.d("CheckOutViewModel", "Deducting wallet: $totalPKR")
                    deductBuyerWallet(buyerId, totalPKR.toDouble(), orderIds.joinToString(","))
                } else if (paymentMethod == "cash_on_delivery") {
                    android.util.Log.d("CheckOutViewModel", "COD order - No wallet deduction")
                    android.util.Log.d("CheckOutViewModel", "Buyer will pay Rs. $totalPKR cash on delivery")
                } else {
                    android.util.Log.d("CheckOutViewModel", "Stripe payment - Wallet already charged via Stripe")
                }
                
                android.util.Log.d("CheckOutViewModel", "=== ORDER PLACEMENT SUCCESS ===")
                android.util.Log.d("CheckOutViewModel", "Total orders created: ${orderIds.size}")
                onSuccess(orderIds)
            } catch (e: Exception) {
                android.util.Log.e("CheckOutViewModel", "Error placing order: ${e.message}", e)
                onError(e.message ?: "Error placing order")
            }
        }
    }

    // ✅ IMPROVED: Fetch client secret with complete order metadata
    suspend fun fetchClientSecret(
        amountPKR: Int,
        sellerGroups: Map<String, List<Map<String, Any>>>,
        buyerEmail: String
    ): String {
        val client = OkHttpClient()
        
        // Prepare metadata for Stripe
        val metadata = JSONObject().apply {
            put("buyerId", auth.currentUser?.uid ?: "")
            put("buyerEmail", buyerEmail)
            put("sellerCount", sellerGroups.size)
            put("totalAmount", amountPKR)
            put("platform", "android_app")
            
            // Add seller details
            sellerGroups.entries.forEachIndexed { index, (sellerId, items) ->
                val sellerTotal = items.sumOf { item ->
                    val price = (item["price"] as? String)?.toDoubleOrNull() ?: 0.0
                    val quantity = (item["quantity"] as? Int) ?: 0
                    price * quantity
                }
                put("seller_${index}_id", sellerId)
                put("seller_${index}_amount", sellerTotal.toInt())
                put("seller_${index}_items", items.size)
            }
        }
        
        val json = JSONObject().apply {
            put("amountPKR", amountPKR)
            put("metadata", metadata)
            put("description", "Order from ${sellerGroups.size} seller(s)")
        }.toString()
        
        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL/create-payment-intent")
            .post(body)
            .build()
            
        val response = client.newCall(request).execute()
        val responseString = response.body!!.string()
        
        if (!response.isSuccessful) {
            throw Exception("Backend error: $responseString")
        }
        
        val responseJson = JSONObject(responseString)
        if (!responseJson.has("clientSecret")) {
            throw Exception("Client Secret Not Found: $responseString")
        }
        
        // Save payment intent ID
        if (responseJson.has("paymentIntentId")) {
            _uiState.update {
                it.copy(paymentIntentId = responseJson.getString("paymentIntentId"))
            }
        }
        
        return responseJson.getString("clientSecret")
    }

    fun pkrToUsd(amountPKR: Int): String {
        val usd = amountPKR / PKR_TO_USD
        return String.format("%.2f", usd)
    }

    // ❌ DEPRECATED: Use placeMultiSellerOrder instead
    @Deprecated("Use placeMultiSellerOrder for better multi-seller support")
    fun placeCodOrder(
        sellerId: String,
        items: List<Map<String, Any>>,
        totalPKR: Float,
        address: Map<String, String>,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // Redirect to new method
        val sellerGroups = mapOf(sellerId to items)
        placeMultiSellerOrder(
            sellerGroups = sellerGroups,
            totalPKR = totalPKR,
            address = address,
            paymentMethod = "cash_on_delivery",
            onSuccess = { orderIds -> onSuccess(orderIds.firstOrNull() ?: "") },
            onError = onError
        )
    }

    // ❌ DEPRECATED: Use placeMultiSellerOrder instead
    @Deprecated("Use placeMultiSellerOrder for better multi-seller support")
    fun placeWalletOrder(
        sellerId: String,
        items: List<Map<String, Any>>,
        totalPKR: Float,
        address: Map<String, String>,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // Redirect to new method
        val sellerGroups = mapOf(sellerId to items)
        placeMultiSellerOrder(
            sellerGroups = sellerGroups,
            totalPKR = totalPKR,
            address = address,
            paymentMethod = "wallet",
            onSuccess = { orderIds -> onSuccess(orderIds.firstOrNull() ?: "") },
            onError = onError
        )
    }

    // ✅ NEW: Save Stripe orders after successful payment
    fun saveStripeOrders(
        sellerGroups: Map<String, List<Map<String, Any>>>,
        totalPKR: Float,
        address: Map<String, String>,
        paymentIntentId: String,
        onSuccess: (List<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // ✅ Initialize refund tracking document for this PaymentIntent
                android.util.Log.d("CheckOutViewModel", "Initializing refund tracking for PaymentIntent: $paymentIntentId")
                
                db.collection("stripe_refund_tracking").document(paymentIntentId).set(
                    mapOf(
                        "paymentIntentId" to paymentIntentId,
                        "totalPaid" to totalPKR.toDouble(),
                        "totalRefunded" to 0.0,
                        "refundedOrders" to emptyList<String>(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "lastRefundedAt" to null
                    )
                ).await()
                
                android.util.Log.d("CheckOutViewModel", "✅ Refund tracking initialized")
                
                // Now place the orders
                placeMultiSellerOrder(
                    sellerGroups = sellerGroups,
                    totalPKR = totalPKR,
                    address = address,
                    paymentMethod = "stripe",
                    stripePaymentIntentId = paymentIntentId,
                    onSuccess = onSuccess,
                    onError = onError
                )
            } catch (e: Exception) {
                android.util.Log.e("CheckOutViewModel", "Error initializing refund tracking: ${e.message}", e)
                // Continue with order placement even if tracking init fails
                placeMultiSellerOrder(
                    sellerGroups = sellerGroups,
                    totalPKR = totalPKR,
                    address = address,
                    paymentMethod = "stripe",
                    stripePaymentIntentId = paymentIntentId,
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
        }
    }
    
    // ── Buyer wallet se amount deduct karo ──
    private fun deductBuyerWallet(buyerId: String, amount: Double, orderId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("CheckOutViewModel", "=== DEDUCTING BUYER WALLET ===")
                android.util.Log.d("CheckOutViewModel", "Buyer ID: $buyerId")
                android.util.Log.d("CheckOutViewModel", "Amount: $amount")
                android.util.Log.d("CheckOutViewModel", "Order ID: $orderId")
                
                val buyerRef = db.collection("users").document(buyerId)
                db.runTransaction { transaction ->
                    val snap = transaction.get(buyerRef)
                    val current = snap.getDouble("walletBalance") ?: 0.0
                    val newBalance = current - amount
                    
                    android.util.Log.d("CheckOutViewModel", "Current Balance: $current")
                    android.util.Log.d("CheckOutViewModel", "New Balance: $newBalance")
                    
                    transaction.update(buyerRef, "walletBalance", newBalance)
                }.await()
                
                android.util.Log.d("CheckOutViewModel", "Wallet deducted successfully")
                
                // Transaction history save karo
                db.collection("wallet_transactions").add(
                    mapOf(
                        "userId" to buyerId,
                        "amount" to amount,
                        "type" to "DEBIT",
                        "orderId" to orderId,
                        "description" to "Payment for Order #${orderId.take(8).uppercase()}",
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                ).await()
                
                android.util.Log.d("CheckOutViewModel", "Transaction history saved")
            } catch (e: Exception) {
                android.util.Log.e("CheckOutViewModel", "Error deducting wallet: ${e.message}", e)
            }
        }
    }
    
    // ── Buyer balance check karo ──
    suspend fun checkBuyerBalance(): Double {
        return try {
            val buyerId = auth.currentUser?.uid ?: return 0.0
            val snap = db.collection("users").document(buyerId).get().await()
            snap.getDouble("walletBalance") ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    fun resetState() {
        _uiState.update { CheckoutUiState() }
    }
}