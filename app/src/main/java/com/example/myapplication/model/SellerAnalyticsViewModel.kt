package com.example.myapplication.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SellerAnalytics(
    val totalOrders: Int = 0,
    val completedOrders: Int = 0,
    val cancelledOrders: Int = 0,
    val refundedOrders: Int = 0,
    val totalRevenue: Double = 0.0,
    val totalProfit: Double = 0.0,
    val productsSold: Int = 0,
    val pendingOrders: Int = 0,
    val processingOrders: Int = 0
)

class SellerAnalyticsViewModel : ViewModel() {
    
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _analytics = MutableStateFlow(SellerAnalytics())
    val analytics = _analytics.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()
    
    fun loadAnalytics() {
        val sellerId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                android.util.Log.d("SellerAnalytics", "=== LOADING SELLER ANALYTICS ===")
                android.util.Log.d("SellerAnalytics", "Seller ID: $sellerId")
                
                // Get all seller orders
                val ordersSnapshot = db.collection("orders")
                    .whereEqualTo("sellerId", sellerId)
                    .get()
                    .await()
                
                android.util.Log.d("SellerAnalytics", "Total orders found: ${ordersSnapshot.size()}")
                
                var totalOrders = 0
                var completedOrders = 0
                var cancelledOrders = 0
                var refundedOrders = 0
                var totalRevenue = 0.0
                var totalProfit = 0.0
                var productsSold = 0
                var pendingOrders = 0
                var processingOrders = 0
                
                ordersSnapshot.documents.forEach { doc ->
                    totalOrders++
                    
                    val status = doc.getString("status") ?: ""
                    val sellerAmount = doc.getDouble("sellerAmount") ?: 0.0
                    val totalAmount = doc.getDouble("totalAmountPKR") ?: 0.0
                    val items = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                    
                    android.util.Log.d("SellerAnalytics", "Order ${doc.id}: status=$status, sellerAmount=$sellerAmount, items=${items.size}")
                    
                    when (status) {
                        "COMPLETED" -> {
                            completedOrders++
                            totalProfit += sellerAmount
                            totalRevenue += totalAmount
                            productsSold += items.size
                        }
                        "CANCELLED" -> {
                            cancelledOrders++
                            val refundedAmount = doc.getDouble("refundedAmount") ?: 0.0
                            if (refundedAmount > 0) {
                                refundedOrders++
                            }
                        }
                        "PENDING", "PAYMENT_CONFIRMED" -> pendingOrders++
                        "CONFIRMED", "PROCESSING", "SHIPPED" -> processingOrders++
                    }
                }
                
                android.util.Log.d("SellerAnalytics", "=== ANALYTICS SUMMARY ===")
                android.util.Log.d("SellerAnalytics", "Total Orders: $totalOrders")
                android.util.Log.d("SellerAnalytics", "Completed: $completedOrders")
                android.util.Log.d("SellerAnalytics", "Cancelled: $cancelledOrders")
                android.util.Log.d("SellerAnalytics", "Refunded: $refundedOrders")
                android.util.Log.d("SellerAnalytics", "Total Revenue: $totalRevenue")
                android.util.Log.d("SellerAnalytics", "Total Profit: $totalProfit")
                android.util.Log.d("SellerAnalytics", "Products Sold: $productsSold")
                
                _analytics.value = SellerAnalytics(
                    totalOrders = totalOrders,
                    completedOrders = completedOrders,
                    cancelledOrders = cancelledOrders,
                    refundedOrders = refundedOrders,
                    totalRevenue = totalRevenue,
                    totalProfit = totalProfit,
                    productsSold = productsSold,
                    pendingOrders = pendingOrders,
                    processingOrders = processingOrders
                )
                
                _isLoading.value = false
                
            } catch (e: Exception) {
                android.util.Log.e("SellerAnalytics", "Error loading analytics: ${e.message}", e)
                _isLoading.value = false
            }
        }
    }
}
