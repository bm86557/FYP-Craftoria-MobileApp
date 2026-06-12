package com.example.myapplication


import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.myapplication.model.CheckOutViewModel
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.PaymentConfiguration

class MainActivity : ComponentActivity() {
    // ✅ NEW: Multi-seller checkout data
    var pendingSellerGroups: Map<String, List<Map<String, Any>>> = emptyMap()
    var pendingAddress: Map<String, String> = emptyMap()
    var pendingTotal: Float = 0f
    var pendingPaymentIntentId: String = ""
    var checkoutViewModel: CheckOutViewModel? = null
    
    // ❌ DEPRECATED: Old single-seller fields (kept for backward compatibility)
    @Deprecated("Use pendingSellerGroups instead")
    var pendingSellerId: String = ""
    @Deprecated("Use pendingSellerGroups instead")
    var pendingItems: List<Map<String, Any>> = emptyList()
    
    lateinit var paymentSheet: PaymentSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stripePublishableKey = getStripePublishableKey()
        if (stripePublishableKey.isNotBlank()) {
            PaymentConfiguration.init(applicationContext, stripePublishableKey)
        } else {
            AppUtil.showToast(this, "Stripe key missing. Add publishable key in manifest meta-data.")
        }

        paymentSheet = PaymentSheet(activity = this) { result ->
            when (result) {
                is PaymentSheetResult.Completed -> {
                    // ✅ NEW: Save multi-seller orders
                    checkoutViewModel?.saveStripeOrders(
                        sellerGroups = pendingSellerGroups,
                        totalPKR = pendingTotal,
                        address = pendingAddress,
                        paymentIntentId = pendingPaymentIntentId,
                        onSuccess = { orderIds ->
                            // Remove purchased items from cart
                            pendingSellerGroups.values.flatten().forEach { item ->
                                val productId = item["productId"] as? String
                                if (productId != null) {
                                    AppUtil.removeItemToCart(productId, this, removeAll = true)
                                }
                            }
                            
                            AppUtil.showToast(this, "${orderIds.size} order(s) placed successfully!")
                            globalNavigation.navigateToTopLevel(AppRoutes.BUYER_ORDERS)
                            clearPendingCheckoutData()
                        },
                        onError = { error ->
                            AppUtil.showToast(this, "Order save failed: $error")
                        }
                    )
                    AppUtil.showToast(context = this, message = "Payment Successful!")
                }
                is PaymentSheetResult.Failed -> {
                    AppUtil.showToast(
                        context = this,
                        message = "Payment failed: ${result.error.message}"
                    )
                }
                is PaymentSheetResult.Canceled -> {
                    AppUtil.showToast(context = this, message = "Payment cancelled")
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = false) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(
                        modifier = Modifier.padding(paddingValues = innerPadding)
                    )
                }
            }
        }
    }

    fun presentPayment(clientSecret: String) {
        paymentSheet.presentWithPaymentIntent(
            paymentIntentClientSecret = clientSecret,
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = "Craftoria App"
            )
        )
    }

    private fun clearPendingCheckoutData() {
        pendingSellerGroups = emptyMap()
        pendingAddress = emptyMap()
        pendingTotal = 0f
        pendingPaymentIntentId = ""
        checkoutViewModel = null
        
        // Clear deprecated fields
        pendingSellerId = ""
        pendingItems = emptyList()
    }

    private fun getStripePublishableKey(): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            appInfo.metaData?.getString("com.stripe.android.publishableKey").orEmpty()
        } catch (e: Exception) {
            ""
        }
    }
}



