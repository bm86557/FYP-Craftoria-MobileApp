package com.example.myapplication.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

data class Order(
    @get:PropertyName("orderId")
    @set:PropertyName("orderId")
    var orderId: String = "",

    @get:PropertyName("buyerId")
    @set:PropertyName("buyerId")
    var buyerId: String = "",

    @get:PropertyName("sellerId")
    @set:PropertyName("sellerId")
    var sellerId: String = "",

    @get:PropertyName("items")
    @set:PropertyName("items")
    var items: List<Map<String, Any>>? = null,

    @get:PropertyName("address")
    @set:PropertyName("address")
    var address: Map<String, Any>? = null,

    @get:PropertyName("totalAmountPKR")
    @set:PropertyName("totalAmountPKR")
    var totalAmountPKR: Double = 0.0,

    @get:PropertyName("platformCommission")
    @set:PropertyName("platformCommission")
    var platformCommission: Double = 0.0,

    @get:PropertyName("sellerAmount")
    @set:PropertyName("sellerAmount")
    var sellerAmount: Double = 0.0,

    @get:PropertyName("paymentMethod")
    @set:PropertyName("paymentMethod")
    var paymentMethod: String = "",

    @get:PropertyName("stripePaymentIntentId")
    @set:PropertyName("stripePaymentIntentId")
    var stripePaymentIntentId: String = "",

    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = "PENDING",

    @get:PropertyName("paymentStatus")
    @set:PropertyName("paymentStatus")
    var paymentStatus: String = "PENDING",

    @get:PropertyName("trackingNumber")
    @set:PropertyName("trackingNumber")
    var trackingNumber: String = "",

    @get:PropertyName("courierService")
    @set:PropertyName("courierService")
    var courierService: String = "",

    @get:PropertyName("isMultiSellerOrder")
    @set:PropertyName("isMultiSellerOrder")
    var isMultiSellerOrder: Boolean = false,

    @get:PropertyName("totalOrderAmount")
    @set:PropertyName("totalOrderAmount")
    var totalOrderAmount: Double = 0.0,

    @get:PropertyName("cancellationReason")
    @set:PropertyName("cancellationReason")
    var cancellationReason: String = "",

    // ✅ REFUND MANAGEMENT FIELDS
    @get:PropertyName("refundStatus")
    @set:PropertyName("refundStatus")
    var refundStatus: String = "", // PENDING_SELLER_APPROVAL, PENDING_ADMIN_APPROVAL, APPROVED, REJECTED, PROCESSED

    @get:PropertyName("refundRequestedBy")
    @set:PropertyName("refundRequestedBy")
    var refundRequestedBy: String = "", // "buyer" or "seller"

    @get:PropertyName("refundRequestedAt")
    @set:PropertyName("refundRequestedAt")
    var refundRequestedAt: Any? = null,

    @get:PropertyName("refundReason")
    @set:PropertyName("refundReason")
    var refundReason: String = "",

    @get:PropertyName("refundAmount")
    @set:PropertyName("refundAmount")
    var refundAmount: Double = 0.0,

    @get:PropertyName("refundApprover")
    @set:PropertyName("refundApprover")
    var refundApprover: String = "", // "seller" or "admin"

    @get:PropertyName("refundApprovedBy")
    @set:PropertyName("refundApprovedBy")
    var refundApprovedBy: String = "",

    @get:PropertyName("refundApprovedAt")
    @set:PropertyName("refundApprovedAt")
    var refundApprovedAt: Any? = null,

    @get:PropertyName("refundRejectedBy")
    @set:PropertyName("refundRejectedBy")
    var refundRejectedBy: String = "",

    @get:PropertyName("refundRejectedAt")
    @set:PropertyName("refundRejectedAt")
    var refundRejectedAt: Any? = null,

    @get:PropertyName("refundRejectionReason")
    @set:PropertyName("refundRejectionReason")
    var refundRejectionReason: String = "",

    @get:PropertyName("refundProcessedAt")
    @set:PropertyName("refundProcessedAt")
    var refundProcessedAt: Any? = null,

    @get:PropertyName("refundedAmount")
    @set:PropertyName("refundedAmount")
    var refundedAmount: Double = 0.0,

    @get:PropertyName("cancellationFee")
    @set:PropertyName("cancellationFee")
    var cancellationFee: Double = 0.0,

    // ✅ Timestamp field - Changed to Any to handle both Timestamp and Long
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Any? = null
) {
    fun getItemsList(): List<Map<String, Any>> {
        return items ?: emptyList()
    }

    fun getAddressMap(): Map<String, Any> {
        return address ?: emptyMap()
    }
    
    // Helper function to get createdAt as Long
    fun getCreatedAtLong(): Long {
        return when (createdAt) {
            is Long -> createdAt as Long
            is com.google.firebase.Timestamp -> (createdAt as com.google.firebase.Timestamp).toDate().time
            else -> System.currentTimeMillis()
        }
    }
}

class OrdersViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val BASE_URL = "http://192.168.1.9:3000"

    private val _buyerOrders = MutableStateFlow<List<Order>>(emptyList())
    val buyerOrders = _buyerOrders.asStateFlow()

    private val _sellerOrders = MutableStateFlow<List<Order>>(emptyList())
    val sellerOrders = _sellerOrders.asStateFlow()

    private val _isCancelling = MutableStateFlow(false)
    val isCancelling = _isCancelling.asStateFlow()

    private val _cancelError = MutableStateFlow<String?>(null)
    val cancelError = _cancelError.asStateFlow()

    fun loadBuyerOrders() {
        val userId = auth.currentUser?.uid ?: return
        android.util.Log.d("OrdersViewModel", "=== LOADING BUYER ORDERS ===")
        android.util.Log.d("OrdersViewModel", "Buyer ID: $userId")
        
        viewModelScope.launch {
            db.collection("orders")
                .whereEqualTo("buyerId", userId)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        android.util.Log.e("OrdersViewModel", "❌ Error loading buyer orders: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snap == null) {
                        android.util.Log.e("OrdersViewModel", "❌ Snapshot is null")
                        return@addSnapshotListener
                    }

                    android.util.Log.d("OrdersViewModel", "📦 Found ${snap.documents.size} buyer orders")
                    
                    val orders = snap.documents.mapNotNull { doc ->
                        try {
                            android.util.Log.d("OrdersViewModel", "Parsing order: ${doc.id}")
                            doc.toObject(Order::class.java)
                        } catch (e: Exception) {
                            android.util.Log.e("OrdersViewModel", "❌ Error parsing order ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    
                    android.util.Log.d("OrdersViewModel", "✅ Successfully loaded ${orders.size} buyer orders")
                    _buyerOrders.value = orders
                }
        }
    }

    fun loadSellerOrders() {
        val userId = auth.currentUser?.uid ?: return
        android.util.Log.d("OrdersViewModel", "=== LOADING SELLER ORDERS ===")
        android.util.Log.d("OrdersViewModel", "Seller ID: $userId")
        
        viewModelScope.launch {
            db.collection("orders")
                .whereEqualTo("sellerId", userId)
                .addSnapshotListener { snap, error ->
                    if (error != null) {
                        android.util.Log.e("OrdersViewModel", "❌ Error loading seller orders: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snap == null) {
                        android.util.Log.e("OrdersViewModel", "❌ Snapshot is null")
                        return@addSnapshotListener
                    }

                    android.util.Log.d("OrdersViewModel", "📦 Found ${snap.documents.size} seller orders")
                    
                    val orders = snap.documents.mapNotNull { doc ->
                        try {
                            android.util.Log.d("OrdersViewModel", "Parsing order: ${doc.id}")
                            doc.toObject(Order::class.java)
                        } catch (e: Exception) {
                            android.util.Log.e("OrdersViewModel", "❌ Error parsing order ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    
                    android.util.Log.d("OrdersViewModel", "✅ Successfully loaded ${orders.size} seller orders")
                    _sellerOrders.value = orders
                }
        }
    }

    // ✅ BUYER CANCELS ORDER (Seller approval needed)
    fun buyerCancelOrder(
        orderId: String,
        reason: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isCancelling.value = true

                val orderDoc = db.collection("orders").document(orderId).get().await()
                val order = orderDoc.toObject(Order::class.java)

                if (order == null) {
                    onError("Order not found")
                    _isCancelling.value = false
                    return@launch
                }

                if (order.status in listOf("COMPLETED", "CANCELLED")) {
                    onError("Cannot cancel ${order.status.lowercase()} order")
                    _isCancelling.value = false
                    return@launch
                }

                // ✅ NO CANCELLATION FEE - Full refund
                val refundAmount = order.totalAmountPKR

                val updates = mapOf(
                    "status" to "CANCELLED",
                    "cancelledAt" to FieldValue.serverTimestamp(),
                    "cancellationReason" to reason,
                    "refundStatus" to "PENDING_SELLER_APPROVAL",
                    "refundRequestedBy" to "buyer",
                    "refundRequestedAt" to FieldValue.serverTimestamp(),
                    "refundReason" to reason,
                    "refundAmount" to refundAmount,
                    "cancellationFee" to 0.0, // No cancellation fee
                    "refundApprover" to "seller",
                    "paymentStatus" to "PENDING_REFUND"
                )

                db.collection("orders").document(orderId).update(updates).await()

                android.util.Log.d("OrdersViewModel", "✅ Buyer cancellation sent to seller (Full refund)")

                _isCancelling.value = false
                onSuccess()

            } catch (e: Exception) {
                _isCancelling.value = false
                onError(e.message ?: "Failed to cancel order")
                android.util.Log.e("OrdersViewModel", "Error: ${e.message}", e)
            }
        }
    }

    // ✅ SELLER INITIATES REFUND (Admin approval needed)
    fun sellerInitiateRefund(
        orderId: String,
        reason: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val orderDoc = db.collection("orders").document(orderId).get().await()
                val order = orderDoc.toObject(Order::class.java)

                if (order == null) {
                    onError("Order not found")
                    return@launch
                }

                val updates = mapOf(
                    "status" to "CANCELLED",
                    "cancelledAt" to FieldValue.serverTimestamp(),
                    "cancellationReason" to reason,
                    "refundStatus" to "PENDING_ADMIN_APPROVAL",
                    "refundRequestedBy" to "seller",
                    "refundRequestedAt" to FieldValue.serverTimestamp(),
                    "refundReason" to reason,
                    "refundAmount" to order.totalAmountPKR, // Full refund
                    "cancellationFee" to 0.0, // No fee for seller mistakes
                    "refundApprover" to "admin",
                    "paymentStatus" to "PENDING_REFUND"
                )

                db.collection("orders").document(orderId).update(updates).await()

                android.util.Log.d("OrdersViewModel", "✅ Seller refund sent to admin")
                onSuccess()

            } catch (e: Exception) {
                onError(e.message ?: "Failed to initiate refund")
                android.util.Log.e("OrdersViewModel", "Error: ${e.message}", e)
            }
        }
    }

    // ✅ SELLER APPROVES/REJECTS BUYER'S CANCELLATION
    fun sellerRespondToRefund(
        orderId: String,
        approved: Boolean,
        reason: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val sellerId = auth.currentUser?.uid ?: ""

                if (approved) {
                    // Approve refund
                    db.collection("orders").document(orderId).update(
                        mapOf(
                            "refundStatus" to "APPROVED",
                            "refundApprovedBy" to sellerId,
                            "refundApprovedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()

                    // Get order to process refund
                    val orderDoc = db.collection("orders").document(orderId).get().await()
                    val order = orderDoc.toObject(Order::class.java)

                    if (order != null) {
                        // Process refund
                        val refundSuccess = processRefundByPaymentMethod(order, orderId)

                        if (refundSuccess) {
                            db.collection("orders").document(orderId).update(
                                mapOf(
                                    "refundStatus" to "PROCESSED",
                                    "refundProcessedAt" to FieldValue.serverTimestamp(),
                                    "paymentStatus" to "REFUNDED",
                                    "refundedAmount" to order.refundAmount
                                )
                            ).await()
                        }
                    }

                    android.util.Log.d("OrdersViewModel", "✅ Seller approved refund")
                    onSuccess()
                } else {
                    // Reject refund
                    db.collection("orders").document(orderId).update(
                        mapOf(
                            "refundStatus" to "REJECTED",
                            "refundRejectedBy" to sellerId,
                            "refundRejectedAt" to FieldValue.serverTimestamp(),
                            "refundRejectionReason" to reason,
                            "status" to "CONFIRMED", // Revert order status
                            "paymentStatus" to "PAID"
                        )
                    ).await()

                    android.util.Log.d("OrdersViewModel", "❌ Seller rejected refund")
                    onSuccess()
                }

            } catch (e: Exception) {
                onError(e.message ?: "Failed to respond to refund")
                android.util.Log.e("OrdersViewModel", "Error: ${e.message}", e)
            }
        }
    }

    // ✅ ADMIN APPROVES/REJECTS SELLER'S REFUND REQUEST
    fun adminRespondToRefund(
        orderId: String,
        approved: Boolean,
        reason: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (approved) {
                    // Approve refund
                    db.collection("orders").document(orderId).update(
                        mapOf(
                            "refundStatus" to "APPROVED",
                            "refundApprovedBy" to "admin",
                            "refundApprovedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()

                    // Get order to process refund
                    val orderDoc = db.collection("orders").document(orderId).get().await()
                    val order = orderDoc.toObject(Order::class.java)

                    if (order != null) {
                        // Process refund
                        val refundSuccess = processRefundByPaymentMethod(order, orderId)

                        if (refundSuccess) {
                            db.collection("orders").document(orderId).update(
                                mapOf(
                                    "refundStatus" to "PROCESSED",
                                    "refundProcessedAt" to FieldValue.serverTimestamp(),
                                    "paymentStatus" to "REFUNDED",
                                    "refundedAmount" to order.refundAmount
                                )
                            ).await()
                        }
                    }

                    android.util.Log.d("OrdersViewModel", "✅ Admin approved refund")
                    onSuccess()
                } else {
                    // Reject refund
                    db.collection("orders").document(orderId).update(
                        mapOf(
                            "refundStatus" to "REJECTED",
                            "refundRejectedBy" to "admin",
                            "refundRejectedAt" to FieldValue.serverTimestamp(),
                            "refundRejectionReason" to reason,
                            "status" to "CONFIRMED", // Revert order status
                            "paymentStatus" to "PAID"
                        )
                    ).await()

                    android.util.Log.d("OrdersViewModel", "❌ Admin rejected refund")
                    onSuccess()
                }

            } catch (e: Exception) {
                onError(e.message ?: "Failed to respond to refund")
                android.util.Log.e("OrdersViewModel", "Error: ${e.message}", e)
            }
        }
    }

    // ✅ PROCESS REFUND BASED ON PAYMENT METHOD
    private suspend fun processRefundByPaymentMethod(
        order: Order,
        orderId: String
    ): Boolean {
        return try {
            when (order.paymentMethod) {
                "wallet" -> {
                    android.util.Log.d("OrdersViewModel", "Processing wallet refund")
                    refundBuyerWallet(order.buyerId, order.refundAmount, orderId)
                    true
                }
                "stripe" -> {
                    android.util.Log.d("OrdersViewModel", "Processing Stripe refund")
                    if (order.stripePaymentIntentId.isNotEmpty()) {
                        processStripeRefund(
                            order.stripePaymentIntentId.trim(),
                            order.refundAmount,
                            orderId
                        )
                    } else {
                        android.util.Log.e("OrdersViewModel", "No payment intent ID")
                        false
                    }
                }
                "cash_on_delivery" -> {
                    android.util.Log.d("OrdersViewModel", "COD - No refund needed")
                    true
                }
                else -> {
                    android.util.Log.e("OrdersViewModel", "Unknown payment method")
                    false
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OrdersViewModel", "Refund failed: ${e.message}", e)
            false
        }
    }

    // ✅ REFUND TO BUYER WALLET
    private suspend fun refundBuyerWallet(
        buyerId: String,
        amount: Double,
        orderId: String
    ) {
        try {
            android.util.Log.d("OrdersViewModel", "=== REFUNDING BUYER WALLET ===")
            android.util.Log.d("OrdersViewModel", "Buyer ID: $buyerId")
            android.util.Log.d("OrdersViewModel", "Amount: $amount")

            val buyerRef = db.collection("users").document(buyerId)
            db.runTransaction { transaction ->
                val snap = transaction.get(buyerRef)
                val current = snap.getDouble("walletBalance") ?: 0.0
                val newBalance = current + amount

                transaction.update(buyerRef, "walletBalance", newBalance)
            }.await()

            db.collection("wallet_transactions").add(
                mapOf(
                    "userId" to buyerId,
                    "amount" to amount,
                    "type" to "REFUND",
                    "orderId" to orderId,
                    "description" to "Refund for Order #${orderId.take(8).uppercase()}",
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()

            android.util.Log.d("OrdersViewModel", "✅ Buyer wallet refunded")
        } catch (e: Exception) {
            android.util.Log.e("OrdersViewModel", "Error refunding wallet: ${e.message}", e)
        }
    }

    // ✅ CREDIT SELLER WALLET
    private suspend fun creditSellerWallet(
        sellerId: String,
        amount: Double,
        orderId: String
    ) {
        try {
            android.util.Log.d("OrdersViewModel", "=== CREDITING SELLER WALLET ===")

            val sellerRef = db.collection("users").document(sellerId)
            db.runTransaction { transaction ->
                val snap = transaction.get(sellerRef)
                val current = snap.getDouble("walletBalance") ?: 0.0
                val newBalance = current + amount

                transaction.update(sellerRef, "walletBalance", newBalance)
            }.await()

            db.collection("wallet_transactions").add(
                mapOf(
                    "userId" to sellerId,
                    "amount" to amount,
                    "type" to "CREDIT",
                    "orderId" to orderId,
                    "description" to "Payment for Order #${orderId.take(8).uppercase()}",
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()

            android.util.Log.d("OrdersViewModel", "✅ Seller wallet credited")
        } catch (e: Exception) {
            android.util.Log.e("OrdersViewModel", "Error crediting wallet: ${e.message}", e)
        }
    }

    // ✅ STRIPE REFUND WITH TRACKING
    private suspend fun processStripeRefund(
        paymentIntentId: String,
        amountPKR: Double,
        orderId: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("OrdersViewModel", "=== STRIPE REFUND ===")
                android.util.Log.d("OrdersViewModel", "Payment Intent: $paymentIntentId")
                android.util.Log.d("OrdersViewModel", "Amount PKR: $amountPKR")
                android.util.Log.d("OrdersViewModel", "Order ID: $orderId")

                // ✅ STEP 1: Check refund tracking document
                val refundTrackingRef = db.collection("stripe_refund_tracking")
                    .document(paymentIntentId)

                // First, check if tracking document exists
                val trackingSnap = refundTrackingRef.get().await()
                
                val totalPaid: Double
                
                if (!trackingSnap.exists()) {
                    // First refund - get total from orders
                    android.util.Log.d("OrdersViewModel", "First refund for this PaymentIntent")
                    
                    val ordersSnap = db.collection("orders")
                        .whereEqualTo("stripePaymentIntentId", paymentIntentId)
                        .get()
                        .await()
                    
                    totalPaid = ordersSnap.documents.firstOrNull()
                        ?.getDouble("totalOrderAmount") ?: 0.0
                    
                    android.util.Log.d("OrdersViewModel", "Total Paid: $totalPaid")
                    
                    // Create tracking document with transaction
                    db.runTransaction { transaction ->
                        transaction.set(refundTrackingRef, mapOf(
                            "paymentIntentId" to paymentIntentId,
                            "totalPaid" to totalPaid,
                            "totalRefunded" to amountPKR,
                            "refundedOrders" to listOf(orderId),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "lastRefundedAt" to FieldValue.serverTimestamp()
                        ))
                        null
                    }.await()
                    
                } else {
                    // Subsequent refund - check if enough balance
                    totalPaid = trackingSnap.getDouble("totalPaid") ?: 0.0
                    val alreadyRefunded = trackingSnap.getDouble("totalRefunded") ?: 0.0
                    val refundedOrders = trackingSnap.get("refundedOrders") as? List<*> ?: emptyList<String>()
                    
                    android.util.Log.d("OrdersViewModel", "Total Paid: $totalPaid")
                    android.util.Log.d("OrdersViewModel", "Already Refunded: $alreadyRefunded")
                    android.util.Log.d("OrdersViewModel", "Remaining: ${totalPaid - alreadyRefunded}")
                    
                    // Check if this order was already refunded
                    if (refundedOrders.contains(orderId)) {
                        android.util.Log.e("OrdersViewModel", "❌ Order already refunded!")
                        throw Exception("This order has already been refunded")
                    }
                    
                    val remainingRefundable = totalPaid - alreadyRefunded
                    
                    if (amountPKR > remainingRefundable) {
                        android.util.Log.e("OrdersViewModel", "❌ Insufficient refundable amount!")
                        throw Exception("Cannot refund Rs. ${String.format("%.2f", amountPKR)}. Only Rs. ${String.format("%.2f", remainingRefundable)} remaining.")
                    }
                    
                    // Update tracking document with transaction
                    db.runTransaction { transaction ->
                        val snap = transaction.get(refundTrackingRef)
                        val currentRefunded = snap.getDouble("totalRefunded") ?: 0.0
                        val currentOrders = snap.get("refundedOrders") as? List<*> ?: emptyList<String>()
                        
                        val newTotalRefunded = currentRefunded + amountPKR
                        val updatedRefundedOrders = currentOrders.toMutableList().apply { add(orderId) }
                        
                        transaction.update(refundTrackingRef, mapOf(
                            "totalRefunded" to newTotalRefunded,
                            "refundedOrders" to updatedRefundedOrders,
                            "lastRefundedAt" to FieldValue.serverTimestamp()
                        ))
                        null
                    }.await()
                }

                // ✅ STEP 2: Process Stripe refund
                android.util.Log.d("OrdersViewModel", "Processing Stripe API refund...")
                
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val json = org.json.JSONObject().apply {
                    put("paymentIntentId", paymentIntentId)
                    put("amount", amountPKR.toInt())
                    put("orderId", orderId)
                }.toString()

                val body = json.toRequestBody("application/json".toMediaType())
                val request = okhttp3.Request.Builder()
                    .url("$BASE_URL/refund-payment")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseString = response.body?.string() ?: ""

                android.util.Log.d("OrdersViewModel", "Stripe Response: $responseString")

                if (response.isSuccessful) {
                    val responseJson = org.json.JSONObject(responseString)
                    val success = responseJson.optBoolean("success", false)

                    if (success) {
                        android.util.Log.d("OrdersViewModel", "✅ Stripe refund successful")
                        return@withContext true
                    } else {
                        // Rollback tracking if Stripe refund failed
                        android.util.Log.e("OrdersViewModel", "❌ Stripe refund failed, rolling back tracking")
                        rollbackRefundTracking(paymentIntentId, amountPKR, orderId)
                        return@withContext false
                    }
                } else {
                    // Rollback tracking if Stripe API call failed
                    android.util.Log.e("OrdersViewModel", "❌ Stripe API call failed, rolling back tracking")
                    rollbackRefundTracking(paymentIntentId, amountPKR, orderId)
                    return@withContext false
                }

            } catch (e: Exception) {
                android.util.Log.e("OrdersViewModel", "Stripe error: ${e.message}", e)
                return@withContext false
            }
        }
    }

    // ✅ ROLLBACK REFUND TRACKING IF STRIPE FAILS
    private suspend fun rollbackRefundTracking(
        paymentIntentId: String,
        amountPKR: Double,
        orderId: String
    ) {
        try {
            val refundTrackingRef = db.collection("stripe_refund_tracking")
                .document(paymentIntentId)

            db.runTransaction { transaction ->
                val snap = transaction.get(refundTrackingRef)
                if (snap.exists()) {
                    val totalRefunded = snap.getDouble("totalRefunded") ?: 0.0
                    val refundedOrders = snap.get("refundedOrders") as? List<*> ?: emptyList<String>()
                    
                    val newTotalRefunded = (totalRefunded - amountPKR).coerceAtLeast(0.0)
                    val updatedRefundedOrders = refundedOrders.toMutableList().apply { remove(orderId) }
                    
                    transaction.update(refundTrackingRef, mapOf(
                        "totalRefunded" to newTotalRefunded,
                        "refundedOrders" to updatedRefundedOrders
                    ))
                }
                null
            }.await()
            
            android.util.Log.d("OrdersViewModel", "✅ Refund tracking rolled back")
        } catch (e: Exception) {
            android.util.Log.e("OrdersViewModel", "Error rolling back: ${e.message}", e)
        }
    }

    // ✅ UPDATE ORDER STATUS
    // ✅ SEND ORDER CONFIRMATION EMAIL
    private suspend fun sendOrderConfirmationEmail(order: Order) {
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("OrdersViewModel", "=== SENDING ORDER CONFIRMATION EMAIL ===")
                
                // Get buyer details
                val buyerDoc = db.collection("users").document(order.buyerId).get().await()
                val buyerEmail = buyerDoc.getString("email") ?: ""
                val buyerName = buyerDoc.getString("name") ?: "Customer"
                
                // Get seller details
                val sellerDoc = db.collection("users").document(order.sellerId).get().await()
                val sellerName = sellerDoc.getString("name") ?: "Seller"
                
                if (buyerEmail.isEmpty()) {
                    android.util.Log.e("OrdersViewModel", "Buyer email not found")
                    return@withContext
                }
                
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val json = org.json.JSONObject().apply {
                    put("buyerEmail", buyerEmail)
                    put("buyerName", buyerName)
                    put("orderId", order.orderId)
                    put("orderAmount", order.totalAmountPKR.toInt())
                    put("itemCount", order.getItemsList().size)
                    put("sellerName", sellerName)
                }.toString()

                val body = json.toRequestBody("application/json".toMediaType())
                val request = okhttp3.Request.Builder()
                    .url("$BASE_URL/send-order-confirmation")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseString = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    android.util.Log.d("OrdersViewModel", "✅ Order confirmation email sent to $buyerEmail")
                } else {
                    android.util.Log.e("OrdersViewModel", "❌ Email failed: $responseString")
                }

            } catch (e: Exception) {
                android.util.Log.e("OrdersViewModel", "❌ Email error: ${e.message}", e)
            }
        }
    }

    // ✅ SEND ORDER SHIPPED EMAIL
    private suspend fun sendOrderShippedEmail(order: Order) {
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("OrdersViewModel", "=== SENDING ORDER SHIPPED EMAIL ===")
                
                // Get buyer details
                val buyerDoc = db.collection("users").document(order.buyerId).get().await()
                val buyerEmail = buyerDoc.getString("email") ?: ""
                val buyerName = buyerDoc.getString("name") ?: "Customer"
                
                if (buyerEmail.isEmpty()) {
                    android.util.Log.e("OrdersViewModel", "Buyer email not found")
                    return@withContext
                }
                
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val json = org.json.JSONObject().apply {
                    put("buyerEmail", buyerEmail)
                    put("buyerName", buyerName)
                    put("orderId", order.orderId)
                    put("trackingNumber", order.trackingNumber)
                    put("courierService", order.courierService)
                }.toString()

                val body = json.toRequestBody("application/json".toMediaType())
                val request = okhttp3.Request.Builder()
                    .url("$BASE_URL/send-order-shipped")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseString = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    android.util.Log.d("OrdersViewModel", "✅ Order shipped email sent to $buyerEmail")
                } else {
                    android.util.Log.e("OrdersViewModel", "❌ Email failed: $responseString")
                }

            } catch (e: Exception) {
                android.util.Log.e("OrdersViewModel", "❌ Email error: ${e.message}", e)
            }
        }
    }

    // ✅ SEND REFUND PROCESSED EMAIL
    private suspend fun sendRefundProcessedEmail(order: Order) {
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("OrdersViewModel", "=== SENDING REFUND PROCESSED EMAIL ===")
                
                // Get buyer details
                val buyerDoc = db.collection("users").document(order.buyerId).get().await()
                val buyerEmail = buyerDoc.getString("email") ?: ""
                val buyerName = buyerDoc.getString("name") ?: "Customer"
                
                if (buyerEmail.isEmpty()) {
                    android.util.Log.e("OrdersViewModel", "Buyer email not found")
                    return@withContext
                }
                
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val json = org.json.JSONObject().apply {
                    put("buyerEmail", buyerEmail)
                    put("buyerName", buyerName)
                    put("orderId", order.orderId)
                    put("refundAmount", order.refundedAmount.toInt())
                    put("paymentMethod", order.paymentMethod)
                }.toString()

                val body = json.toRequestBody("application/json".toMediaType())
                val request = okhttp3.Request.Builder()
                    .url("$BASE_URL/send-refund-processed")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseString = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    android.util.Log.d("OrdersViewModel", "✅ Refund processed email sent to $buyerEmail")
                } else {
                    android.util.Log.e("OrdersViewModel", "❌ Email failed: $responseString")
                }

            } catch (e: Exception) {
                android.util.Log.e("OrdersViewModel", "❌ Email error: ${e.message}", e)
            }
        }
    }

    fun updateStatus(orderId: String, status: String) {
        viewModelScope.launch {
            try {
                val orderDoc = db.collection("orders").document(orderId).get().await()
                val order = orderDoc.toObject(Order::class.java)

                val updates = mutableMapOf<String, Any>(
                    "status" to status,
                    "lastUpdatedAt" to FieldValue.serverTimestamp()
                )

                when (status) {
                    "CONFIRMED" -> {
                        updates["confirmedAt"] = FieldValue.serverTimestamp()
                        
                        // ✅ Send email notification to buyer
                        if (order != null) {
                            sendOrderConfirmationEmail(order)
                        }
                    }
                    "PROCESSING" -> updates["processingAt"] = FieldValue.serverTimestamp()
                    "SHIPPED" -> {
                        updates["shippedAt"] = FieldValue.serverTimestamp()
                        
                        // ✅ Send shipped email notification
                        if (order != null) {
                            sendOrderShippedEmail(order)
                        }
                    }
                    "DELIVERED" -> updates["deliveredAt"] = FieldValue.serverTimestamp()
                    "COMPLETED" -> {
                        updates["completedAt"] = FieldValue.serverTimestamp()

                        // Credit seller wallet (except COD)
                        val paymentMethod = orderDoc.getString("paymentMethod") ?: ""
                        if (paymentMethod != "cash_on_delivery") {
                            val sellerId = orderDoc.getString("sellerId") ?: ""
                            val sellerAmount = orderDoc.getDouble("sellerAmount") ?: 0.0
                            if (sellerAmount > 0) {
                                creditSellerWallet(sellerId, sellerAmount, orderId)
                            }
                        }
                    }
                }

                db.collection("orders").document(orderId).update(updates).await()

                android.util.Log.d("OrdersViewModel", "✅ Status updated to $status")

            } catch (e: Exception) {
                android.util.Log.e("OrdersViewModel", "Error updating status: ${e.message}", e)
            }
        }
    }

    // ✅ CHECK REFUND STATUS FOR PAYMENT INTENT (Debug Helper)
    suspend fun getRefundStatus(paymentIntentId: String): Map<String, Any> {
        return try {
            val trackingDoc = db.collection("stripe_refund_tracking")
                .document(paymentIntentId)
                .get()
                .await()
            
            if (trackingDoc.exists()) {
                mapOf(
                    "exists" to true,
                    "totalPaid" to (trackingDoc.getDouble("totalPaid") ?: 0.0),
                    "totalRefunded" to (trackingDoc.getDouble("totalRefunded") ?: 0.0),
                    "remainingRefundable" to ((trackingDoc.getDouble("totalPaid") ?: 0.0) - (trackingDoc.getDouble("totalRefunded") ?: 0.0)),
                    "refundedOrders" to (trackingDoc.get("refundedOrders") ?: emptyList<String>())
                )
            } else {
                mapOf("exists" to false)
            }
        } catch (e: Exception) {
            mapOf("error" to (e.message ?: "Unknown error"))
        }
    }
}
