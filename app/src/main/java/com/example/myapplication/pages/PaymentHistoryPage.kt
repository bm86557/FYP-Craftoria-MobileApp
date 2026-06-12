package com.example.myapplication.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class PaymentHistoryItem(
    val orderId: String,
    val amount: Double,
    val paymentMethod: String,
    val timestamp: Timestamp?,
    val status: String,
    val type: String // "order" or "wallet_transaction"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryPage(
    navController: NavController
) {
    var paymentHistory by remember { mutableStateOf<List<PaymentHistoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("all") } // all, stripe, wallet, cod
    
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val db = FirebaseFirestore.getInstance()

    // Load payment history
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val allPayments = mutableListOf<PaymentHistoryItem>()
            
            android.util.Log.d("PaymentHistory", "Starting to fetch for user: $currentUserId")
            
            // 1. Fetch all orders (without orderBy to avoid index issues)
            try {
                val ordersSnap = db.collection("orders")
                    .whereEqualTo("buyerId", currentUserId)
                    .get()
                    .await()
                
                android.util.Log.d("PaymentHistory", "Orders found: ${ordersSnap.size()}")
                
                ordersSnap.documents.forEach { doc ->
                    val paymentMethod = doc.getString("paymentMethod") ?: ""
                    // Try multiple field names for amount
                    val amount = doc.getDouble("totalAmountPKR") 
                        ?: doc.getLong("totalAmountPKR")?.toDouble()
                        ?: doc.getDouble("totalAmount") 
                        ?: doc.getLong("totalAmount")?.toDouble() 
                        ?: 0.0
                    // Try multiple field names for timestamp
                    val timestamp = doc.getTimestamp("createdAt") 
                        ?: doc.getTimestamp("timestamp")
                        ?: doc.getTimestamp("paymentConfirmedAt")
                    val status = doc.getString("status") ?: ""
                    
                    android.util.Log.d("PaymentHistory", "Order: ${doc.id}, method: $paymentMethod, amount: $amount, status: $status, timestamp: $timestamp")
                    
                    // Add all orders regardless of payment method
                    allPayments.add(
                        PaymentHistoryItem(
                            orderId = doc.id,
                            amount = amount,
                            paymentMethod = if (paymentMethod.isEmpty()) "unknown" else paymentMethod,
                            timestamp = timestamp,
                            status = status,
                            type = "order"
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("PaymentHistory", "Error fetching orders: ${e.message}", e)
            }
            
            // 2. Fetch wallet transactions (without orderBy)
            try {
                val walletSnap = db.collection("wallet_transactions")
                    .whereEqualTo("userId", currentUserId)
                    .get()
                    .await()
                
                android.util.Log.d("PaymentHistory", "Wallet transactions found: ${walletSnap.size()}")
                
                walletSnap.documents.forEach { doc ->
                    val amount = doc.getDouble("amount") ?: 0.0
                    val type = doc.getString("type") ?: ""
                    val orderId = doc.getString("orderId") ?: ""
                    val timestamp = doc.getTimestamp("timestamp")
                    
                    android.util.Log.d("PaymentHistory", "Wallet: ${doc.id}, type: $type, orderId: $orderId, amount: $amount")
                    
                    // Only add DEBIT transactions
                    if (type == "DEBIT") {
                        // Check if not already added from orders
                        val alreadyExists = allPayments.any { it.orderId == orderId && orderId.isNotEmpty() }
                        if (!alreadyExists) {
                            allPayments.add(
                                PaymentHistoryItem(
                                    orderId = orderId,
                                    amount = amount,
                                    paymentMethod = "wallet",
                                    timestamp = timestamp,
                                    status = "completed",
                                    type = "wallet_transaction"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PaymentHistory", "Error fetching wallet: ${e.message}", e)
            }
            
            // Sort by timestamp (newest first)
            paymentHistory = allPayments.sortedByDescending { 
                it.timestamp?.seconds ?: 0L 
            }
            
            android.util.Log.d("PaymentHistory", "Total payments loaded: ${paymentHistory.size}")
            
        } catch (e: Exception) {
            android.util.Log.e("PaymentHistory", "Error: ${e.message}", e)
        } finally {
            isLoading = false
        }
    }

    // Filter payments
    val filteredPayments = when (selectedFilter) {
        "stripe" -> paymentHistory.filter { it.paymentMethod == "stripe" }
        "wallet" -> paymentHistory.filter { it.paymentMethod == "wallet" }
        "cod" -> paymentHistory.filter { it.paymentMethod == "cash_on_delivery" }
        else -> paymentHistory // Show all including unknown
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "all",
                    onClick = { selectedFilter = "all" },
                    label = { Text("All (${paymentHistory.size})") }
                )
                FilterChip(
                    selected = selectedFilter == "stripe",
                    onClick = { selectedFilter = "stripe" },
                    label = { Text("Card") }
                )
                FilterChip(
                    selected = selectedFilter == "wallet",
                    onClick = { selectedFilter = "wallet" },
                    label = { Text("Wallet") }
                )
                FilterChip(
                    selected = selectedFilter == "cod",
                    onClick = { selectedFilter = "cod" },
                    label = { Text("COD") }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (filteredPayments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No payment history",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPayments) { payment ->
                        PaymentHistoryCard(
                            payment = payment,
                            onClick = {
                                if (payment.orderId.isNotEmpty()) {
                                    try {
                                        navController.navigate("orderDetail/${payment.orderId}")
                                    } catch (e: Exception) {
                                        android.util.Log.e("PaymentHistory", "Navigation error: ${e.message}")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentHistoryCard(
    payment: PaymentHistoryItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (payment.orderId.isNotEmpty()) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Payment Method Icon
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (payment.paymentMethod) {
                        "stripe" -> Color(0xFFE3F2FD)
                        "wallet" -> Color(0xFFFFF3E0)
                        "cash_on_delivery" -> Color(0xFFE8F5E9)
                        else -> Color(0xFFF5F5F5)
                    }
                ) {
                    Icon(
                        when (payment.paymentMethod) {
                            "stripe" -> Icons.Default.CreditCard
                            "wallet" -> Icons.Default.AccountBalanceWallet
                            "cash_on_delivery" -> Icons.Default.Money
                            else -> Icons.Default.Receipt
                        },
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = when (payment.paymentMethod) {
                            "stripe" -> Color(0xFF1976D2)
                            "wallet" -> Color(0xFFF57C00)
                            "cash_on_delivery" -> Color(0xFF2E7D32)
                            else -> Color(0xFF757575)
                        }
                    )
                }

                Column {
                    Text(
                        text = when (payment.paymentMethod) {
                            "stripe" -> "💳 Card Payment"
                            "wallet" -> "💰 Wallet Payment"
                            "cash_on_delivery" -> "💵 Cash on Delivery"
                            else -> "📦 Order Payment"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    if (payment.orderId.isNotEmpty()) {
                        Text(
                            text = "Order #${payment.orderId.take(8).uppercase()}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatTimestamp(payment.timestamp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "PKR ${String.format("%.0f", payment.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (payment.status) {
                        "completed", "delivered" -> Color(0xFFE8F5E9)
                        "pending" -> Color(0xFFFFF3E0)
                        else -> Color(0xFFE3F2FD)
                    }
                ) {
                    Text(
                        text = payment.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (payment.status) {
                            "completed", "delivered" -> Color(0xFF2E7D32)
                            "pending" -> Color(0xFFF57C00)
                            else -> Color(0xFF1976D2)
                        }
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Timestamp?): String {
    return try {
        if (timestamp != null) {
            val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            sdf.format(timestamp.toDate())
        } else {
            "N/A"
        }
    } catch (e: Exception) {
        "N/A"
    }
}
