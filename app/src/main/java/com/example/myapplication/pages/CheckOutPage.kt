package com.example.myapplication.pages

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.AppRoutes
import com.example.myapplication.AppUtil
import com.example.myapplication.MainActivity
import com.example.myapplication.model.CheckOutViewModel
import com.example.myapplication.model.ProductModel
import com.example.myapplication.model.UserModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PAYMENT_CASH_ON_DELIVERY = "cash_on_delivery"
private const val PAYMENT_STRIPE = "stripe"
private const val PAYMENT_WALLET = "wallet"

@Composable
fun CheckOutPage(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    negotiatedPrice: Float = 0f,
    negotiatedProductId: String = "",
    selectedProductId: String = "",
) {
    val viewmodel: CheckOutViewModel = viewModel()
    val context = LocalContext.current
    val activity = context as? MainActivity
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val usermodel = remember { mutableStateOf(UserModel()) }
    val productList = remember { mutableStateListOf<ProductModel>() }
    val subTotal = remember { mutableStateOf(0f) }
    val discount = remember { mutableStateOf(0f) }
    val platformFee = remember { mutableStateOf(0f) }
    val total = remember { mutableStateOf(0f) }
    var buyerBalance by remember { mutableStateOf(0.0) }

    fun calculateAndAssignTotals(isSingleProductCheckout: Boolean = false) {
        subTotal.value = 0f
        discount.value = 0f
        platformFee.value = 0f
        if (negotiatedPrice > 0f) {
            subTotal.value = negotiatedPrice
        } else {
            productList.forEach {
                // ✅ Use price (discounted/current price) - what buyer sees on product page
                val priceStr = it.price.ifEmpty { it.actualPrice }
                if (priceStr.isNotEmpty()) {
                    // If single product checkout, use quantity 1
                    // Otherwise use cart quantity
                    val qty = if (isSingleProductCheckout) {
                        1
                    } else {
                        // ✅ FIXED: Convert Long to Int properly
                        val cartQty = usermodel.value.cartItems[it.id]
                        when (cartQty) {
                            is Long -> cartQty.toInt()
                            is Int -> cartQty
                            else -> 0
                        }
                    }
                    subTotal.value += priceStr.toFloat() * qty
                    
                    android.util.Log.d("CheckOutPage", "Product: ${it.title}, Price: $priceStr, Qty: $qty, Subtotal: ${priceStr.toFloat() * qty}")
                }
            }
        }
        discount.value = subTotal.value * (AppUtil.getDuscountPercentage()) / 100
        
        // ✅ FIXED: Commission is NOT charged to buyer
        // Commission will be deducted from seller when order completes
        platformFee.value = 0f
        
        // ✅ Buyer pays: Subtotal - Discount (NO commission added)
        total.value = "%.2f".format(subTotal.value - discount.value).toFloat()
        
        android.util.Log.d("CheckOutPage", "Final Subtotal: ${subTotal.value}")
    }
    // Address fields
    var fullName by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedPayment by remember { mutableStateOf(PAYMENT_CASH_ON_DELIVERY) }

    LaunchedEffect(negotiatedProductId, negotiatedPrice, selectedProductId) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        
        android.util.Log.d("CheckOutPage", "=== LAUNCHED EFFECT START ===")
        android.util.Log.d("CheckOutPage", "User ID: $uid")
        android.util.Log.d("CheckOutPage", "negotiatedProductId: $negotiatedProductId")
        android.util.Log.d("CheckOutPage", "negotiatedPrice: $negotiatedPrice")
        android.util.Log.d("CheckOutPage", "selectedProductId: $selectedProductId")
        
        // Buyer balance fetch karo
        buyerBalance = viewmodel.checkBuyerBalance()
        android.util.Log.d("CheckOutPage", "Buyer Balance: $buyerBalance")
        
        // Clear product list at the start
        productList.clear()
        
        Firebase.firestore.collection("users")
            .document(uid)
            .get()
            .addOnCompleteListener { userTask ->
                if (!userTask.isSuccessful) {
                    android.util.Log.e("CheckOutPage", "Failed to fetch user: ${userTask.exception?.message}")
                    return@addOnCompleteListener
                }
                val result = userTask.result.toObject(UserModel::class.java) ?: return@addOnCompleteListener
                usermodel.value = result
                
                android.util.Log.d("CheckOutPage", "User cart items: ${result.cartItems}")

                // Priority: negotiated > selected > all cart items
                val singleNegotiated = negotiatedPrice > 0f && negotiatedProductId.isNotBlank()
                val singleSelected = selectedProductId.isNotBlank()
                
                when {
                    singleNegotiated -> {
                        android.util.Log.d("CheckOutPage", "Mode: Negotiated product checkout")
                        // Negotiated product checkout
                        Firebase.firestore.collection("data").document("stock")
                            .collection("products")
                            .document(negotiatedProductId)
                            .get()
                            .addOnCompleteListener { pt ->
                                if (pt.isSuccessful) {
                                    productList.clear()
                                    val p = pt.result.toObject(ProductModel::class.java)
                                    if (p != null) {
                                        android.util.Log.d("CheckOutPage", "Fetched product: ${p.title}, actualPrice: '${p.actualPrice}', price: '${p.price}'")
                                        productList.add(p)
                                        calculateAndAssignTotals(isSingleProductCheckout = true)
                                    } else {
                                        android.util.Log.e("CheckOutPage", "Product is null!")
                                    }
                                } else {
                                    android.util.Log.e("CheckOutPage", "Failed to fetch product: ${pt.exception?.message}")
                                }
                            }
                    }
                    singleSelected -> {
                        android.util.Log.d("CheckOutPage", "Mode: Selected product checkout")
                        // Selected product checkout - ONLY this product, quantity 1
                        Firebase.firestore.collection("data").document("stock")
                            .collection("products")
                            .document(selectedProductId)
                            .get()
                            .addOnCompleteListener { pt ->
                                if (pt.isSuccessful) {
                                    productList.clear()
                                    val p = pt.result.toObject(ProductModel::class.java)
                                    if (p != null) {
                                        android.util.Log.d("CheckOutPage", "Fetched product: ${p.title}, actualPrice: '${p.actualPrice}', price: '${p.price}'")
                                        productList.add(p)
                                        calculateAndAssignTotals(isSingleProductCheckout = true)
                                    } else {
                                        android.util.Log.e("CheckOutPage", "Product is null!")
                                    }
                                } else {
                                    android.util.Log.e("CheckOutPage", "Failed to fetch product: ${pt.exception?.message}")
                                }
                            }
                    }
                    else -> {
                        android.util.Log.d("CheckOutPage", "Mode: Cart checkout")
                        // All cart items checkout - use cart quantities
                        val keys = usermodel.value.cartItems.keys.toList()
                        android.util.Log.d("CheckOutPage", "Cart product IDs: $keys")
                        if (keys.isEmpty()) {
                            android.util.Log.w("CheckOutPage", "Cart is empty!")
                            productList.clear()
                            calculateAndAssignTotals(isSingleProductCheckout = false)
                            return@addOnCompleteListener
                        }
                        Firebase.firestore.collection("data").document("stock")
                            .collection("products")
                            .whereIn("id", keys)
                            .get()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    productList.clear()
                                    val products = task.result.toObjects(ProductModel::class.java)
                                    android.util.Log.d("CheckOutPage", "Fetched ${products.size} products from cart")
                                    products.forEach { p ->
                                        android.util.Log.d("CheckOutPage", "  - ${p.title}: actualPrice='${p.actualPrice}', price='${p.price}'")
                                    }
                                    productList.addAll(products)
                                    calculateAndAssignTotals(isSingleProductCheckout = false)
                                } else {
                                    android.util.Log.e("CheckOutPage", "Failed to fetch cart products: ${task.exception?.message}")
                                }
                            }
                    }
                }
            }
    }

    val canPay = total.value > 0f && productList.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(text = "Checkout", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        RowCheckOutItems(title = "Sub Total", value = subTotal.value.toString())
        HorizontalDivider()
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "To Pay", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(
            text = "PKR" + total.value.toString(),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "= $${viewmodel.pkrToUsd(total.value.toInt())} USD",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
        )
        
        // ── Buyer Balance Display ─────────────────────
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (buyerBalance < subTotal.value) 
                    Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💰 Your Wallet Balance:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Rs. ${String.format("%.2f", buyerBalance)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (buyerBalance < subTotal.value) 
                        Color(0xFFC62828) else Color(0xFF2E7D32)
                )
            }
            if (selectedPayment == PAYMENT_WALLET && buyerBalance < subTotal.value) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                Text(
                    text = "⚠️ Insufficient balance for wallet payment. You need Rs. ${String.format("%.2f", subTotal.value - buyerBalance)} more.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFC62828)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Delivery Address",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = street,
            onValueChange = { street = it },
            label = { Text("Street Address") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("City") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Payment Method", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

// Wallet Payment
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedPayment = PAYMENT_WALLET },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedPayment == PAYMENT_WALLET,
                onClick = { selectedPayment = PAYMENT_WALLET }
            )
            Column {
                Text("💰 Pay from Wallet")
                if (buyerBalance < subTotal.value) {
                    Text(
                        text = "Insufficient balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC62828)
                    )
                }
            }
        }

// Cash on Delivery
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedPayment = PAYMENT_CASH_ON_DELIVERY },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedPayment == PAYMENT_CASH_ON_DELIVERY,
                onClick = { selectedPayment = PAYMENT_CASH_ON_DELIVERY }
            )
            Text("💵 Cash on Delivery")
        }

// Card Payment
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedPayment = PAYMENT_STRIPE },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedPayment == PAYMENT_STRIPE,
                onClick = { selectedPayment = PAYMENT_STRIPE }
            )
            Text("💳 Credit / Debit Card")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch(context = Dispatchers.IO) {
                    withContext(context = Dispatchers.Main) { isLoading = true }
                    try {
                        val validationMessage = validateDeliveryAddress(
                            fullName = fullName,
                            street = street,
                            city = city,
                            phone = phone
                        )
                        if (validationMessage != null) {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                AppUtil.showToast(context, validationMessage)
                            }
                            return@launch
                        }

                        val address = mapOf(
                            "fullName" to fullName.trim(),
                            "street" to street.trim(),
                            "city" to city.trim(),
                            "phone" to phone.trim()
                        )

                        // ✅ Calculate actual subtotal (without discount/platformFee)
                        val actualSubtotal = productList.sumOf { product ->
                            val quantity = if (negotiatedPrice > 0f || selectedProductId.isNotBlank()) {
                                1
                            } else {
                                // ✅ FIXED: Convert Long to Int properly
                                val cartQty = usermodel.value.cartItems[product.id]
                                when (cartQty) {
                                    is Long -> cartQty.toInt()
                                    is Int -> cartQty
                                    else -> 0
                                }
                            }
                            val price = if (negotiatedPrice > 0f) {
                                negotiatedPrice.toDouble()
                            } else {
                                // ✅ Use price (discounted/current price) - what buyer sees
                                val priceStr = product.price.ifEmpty { product.actualPrice }
                                priceStr.toDoubleOrNull() ?: 0.0
                            }
                            val itemSubtotal = price * quantity
                            android.util.Log.e("CheckOutPage", "ITEM CALC: ${product.title} = $price x $quantity = $itemSubtotal")
                            itemSubtotal
                        }.toFloat()
                        
                        android.util.Log.e("CheckOutPage", "=== CHECKOUT CALCULATION ===")
                        android.util.Log.e("CheckOutPage", "Products count: ${productList.size}")
                        productList.forEach { product ->
                            val priceStr = product.price.ifEmpty { product.actualPrice }
                            val qty = if (negotiatedPrice > 0f || selectedProductId.isNotBlank()) {
                                1
                            } else {
                                // ✅ FIXED: Convert Long to Int properly
                                val cartQty = usermodel.value.cartItems[product.id]
                                when (cartQty) {
                                    is Long -> cartQty.toInt()
                                    is Int -> cartQty
                                    else -> 0
                                }
                            }
                            android.util.Log.e("CheckOutPage", "  Product: ${product.title}")
                            android.util.Log.e("CheckOutPage", "    actualPrice: '${product.actualPrice}'")
                            android.util.Log.e("CheckOutPage", "    price: '${product.price}'")
                            android.util.Log.e("CheckOutPage", "    using: '$priceStr'")
                            android.util.Log.e("CheckOutPage", "    cart quantity type: ${usermodel.value.cartItems[product.id]?.javaClass?.simpleName}")
                            android.util.Log.e("CheckOutPage", "    cart quantity value: ${usermodel.value.cartItems[product.id]}")
                            android.util.Log.e("CheckOutPage", "    quantity: $qty")
                            android.util.Log.e("CheckOutPage", "    subtotal: ${(priceStr.toDoubleOrNull() ?: 0.0) * qty}")
                        }
                        android.util.Log.e("CheckOutPage", "Subtotal (UI): ${subTotal.value}")
                        android.util.Log.e("CheckOutPage", "Discount (UI): ${discount.value}")
                        android.util.Log.e("CheckOutPage", "Platform Fee (UI): ${platformFee.value}")
                        android.util.Log.e("CheckOutPage", "Total (UI): ${total.value}")
                        android.util.Log.e("CheckOutPage", "Actual Subtotal (for order): $actualSubtotal")
                        android.util.Log.e("CheckOutPage", "Buyer Balance: $buyerBalance")
                        
                        // ✅ SAFETY CHECK: If actualSubtotal is 0 but UI shows value, use UI value
                        val finalTotal = if (actualSubtotal == 0f && subTotal.value > 0f) {
                            android.util.Log.e("CheckOutPage", "⚠️ WARNING: actualSubtotal is 0 but UI subtotal is ${subTotal.value}. Using UI value!")
                            subTotal.value
                        } else {
                            actualSubtotal
                        }
                        
                        android.util.Log.e("CheckOutPage", "✅ FINAL TOTAL TO USE: $finalTotal")
                        
                        if (finalTotal == 0f) {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                AppUtil.showToast(context, "ERROR: Cannot place order with 0 amount! Check product prices.")
                            }
                            return@launch
                        }

                        // ✅ NEW: Cart items with seller info
                        val itemsList = productList.map { product ->
                            val quantity = if (negotiatedPrice > 0f || selectedProductId.isNotBlank()) {
                                1 // Single product checkout
                            } else {
                                // ✅ FIXED: Convert Long to Int properly
                                val cartQty = usermodel.value.cartItems[product.id]
                                when (cartQty) {
                                    is Long -> cartQty.toInt()
                                    is Int -> cartQty
                                    else -> 0
                                }
                            }
                            
                            // Use negotiated price if available, otherwise use actual price
                            val itemPrice = if (negotiatedPrice > 0f) {
                                negotiatedPrice.toString()
                            } else {
                                // ✅ Use price (discounted/current price) - what buyer sees
                                product.price.ifEmpty { product.actualPrice }
                            }
                            
                            mapOf(
                                "productId" to (product.id ?: ""),
                                "productName" to (product.title ?: ""),
                                "price" to itemPrice,
                                "quantity" to quantity,
                                "sellerId" to (product.sellerId ?: ""), // ✅ Each product's seller
                                "sellerName" to (product.sellerName ?: ""),
                                "isCoStoreProduct" to (product.isCoStoreProduct),
                                "coStoreId" to (product.coStoreId ?: ""),
                                "coStoreName" to (product.coStoreName ?: "")
                            )
                        }

                        // ✅ NEW: Group by seller for multi-seller orders
                        val sellerGroups = itemsList.groupBy { it["sellerId"] as String }

                        if (selectedPayment == PAYMENT_WALLET) {
                            // Wallet Payment — balance check + deduct
                            viewmodel.placeMultiSellerOrder(
                                sellerGroups = sellerGroups,
                                totalPKR = finalTotal,  // ✅ Use finalTotal
                                address = address,
                                paymentMethod = "wallet",
                                onSuccess = { orderIds ->
                                    scope.launch(Dispatchers.Main) {
                                        isLoading = false
                                        
                                        // Remove purchased items from cart
                                        productList.forEach { product ->
                                            AppUtil.removeItemToCart(product.id, context, removeAll = true)
                                        }
                                        
                                        AppUtil.showToast(context, "${orderIds.size} order(s) placed successfully!")
                                        navController.navigate(AppRoutes.BUYER_ORDERS) {
                                            popUpTo(AppRoutes.CHECKOUT) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                onError = { error ->
                                    scope.launch(Dispatchers.Main) {
                                        isLoading = false
                                        AppUtil.showToast(context, error)
                                    }
                                }
                            )
                        } else if (selectedPayment == PAYMENT_CASH_ON_DELIVERY) {
                            // COD — seedha order save karo
                            viewmodel.placeMultiSellerOrder(
                                sellerGroups = sellerGroups,
                                totalPKR = finalTotal,  // ✅ Use finalTotal
                                address = address,
                                paymentMethod = "cash_on_delivery",
                                onSuccess = { orderIds ->
                                    scope.launch(Dispatchers.Main) {
                                        isLoading = false
                                        
                                        // Remove purchased items from cart
                                        productList.forEach { product ->
                                            AppUtil.removeItemToCart(product.id, context, removeAll = true)
                                        }
                                        
                                        AppUtil.showToast(context, "${orderIds.size} order(s) placed successfully!")
                                        navController.navigate(AppRoutes.BUYER_ORDERS) {
                                            popUpTo(AppRoutes.CHECKOUT) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                onError =  { error ->
                                    scope.launch(Dispatchers.Main) {
                                        isLoading = false
                                        AppUtil.showToast(context, "Error: $error")
                                    }
                                }
                            )
                        } else {
                            // Stripe — pehle clientSecret lo with complete metadata
                            if (activity == null) {
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    AppUtil.showToast(context, "Payment screen is not ready. Please reopen checkout.")
                                }
                                return@launch
                            }

                            val amountPKR = finalTotal.toInt()  // ✅ Use finalTotal
                            val buyerEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
                            
                            // ✅ NEW: Pass complete order data to Stripe
                            val secret = viewmodel.fetchClientSecret(
                                amountPKR = amountPKR,
                                sellerGroups = sellerGroups,
                                buyerEmail = buyerEmail
                            )

                            withContext(context = Dispatchers.Main) {
                                isLoading = false
                                // Pending order data store karo MainActivity mein
                                activity.pendingSellerGroups = sellerGroups
                                activity.pendingAddress = address
                                activity.pendingTotal = finalTotal  // ✅ Use finalTotal
                                activity.pendingPaymentIntentId = viewmodel.uiState.value.paymentIntentId.orEmpty()
                                activity.checkoutViewModel = viewmodel
                                activity.presentPayment(clientSecret = secret)
                            }
                        }
                    } catch (e: Exception) {
                        withContext(context = Dispatchers.Main) {
                            isLoading = false
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading && canPay,
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(text = "Pay Now")
            }
        }
    }
}

private fun validateDeliveryAddress(
    fullName: String,
    street: String,
    city: String,
    phone: String
): String? {
    if (fullName.isBlank()) return "Please enter full name"
    if (street.isBlank()) return "Please enter street address"
    if (city.isBlank()) return "Please enter city"
    if (phone.isBlank()) return "Please enter phone number"
    if (phone.length < 10) return "Phone number should be at least 10 digits"
    return null
}

@Composable
fun RowCheckOutItems(title: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(text = value, fontSize = 18.sp)
    }
}
