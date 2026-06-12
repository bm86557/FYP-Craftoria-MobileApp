package com.example.myapplication.pages

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.model.Order
import com.example.myapplication.model.OrdersViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerRefundHistoryPage(
    navController: NavHostController,
    viewModel: OrdersViewModel = viewModel()
) {
    val orders by viewModel.buyerOrders.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadBuyerOrders()
    }

    // Filter only orders with refund status
    val refundOrders = orders.filter { order ->
        order.refundStatus.isNotEmpty() && order.status == "CANCELLED"
    }.sortedByDescending { order -> order.getCreatedAtLong() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Refund History", fontWeight = FontWeight.Bold)
                        Text(
                            "${refundOrders.size} refund requests",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (refundOrders.isEmpty()) {
            EmptyRefundState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(refundOrders) { order ->
                    BuyerRefundCard(order = order, navController = navController)
                }
            }
        }
    }
}

@Composable
fun BuyerRefundCard(
    order: Order,
    navController: NavHostController
) {
    val items = order.getItemsList()
    val address = order.getAddressMap()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (order.refundStatus) {
                "PROCESSED" -> Color(0xFFE8F5E9)
                "APPROVED" -> Color(0xFFE3F2FD)
                "REJECTED" -> Color(0xFFFFEBEE)
                "PENDING_SELLER_APPROVAL", "PENDING_ADMIN_APPROVAL" -> Color(0xFFFFF3E0)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Order #${order.orderId.take(8).uppercase()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        formatRefundDate(order.getCreatedAtLong()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                RefundStatusBadge(order.refundStatus)
            }

            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(12.dp))

            // Items
            Text(
                "Items (${items.size})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            items.take(2).forEach { item ->
                Text(
                    "• ${item["productName"]} (x${item["quantity"]})",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (items.size > 2) {
                Text(
                    "  +${items.size - 2} more items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            // Payment Method
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    when (order.paymentMethod) {
                        "wallet" -> Icons.Default.AccountBalanceWallet
                        "stripe" -> Icons.Default.CreditCard
                        else -> Icons.Default.Money
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    when (order.paymentMethod) {
                        "cash_on_delivery" -> "Cash on Delivery"
                        "wallet" -> "Wallet Payment"
                        "stripe" -> "Card Payment"
                        else -> order.paymentMethod
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(12.dp))

            // Refund Details
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (order.refundStatus) {
                        "PROCESSED" -> Color(0xFFC8E6C9)
                        "APPROVED" -> Color(0xFFBBDEFB)
                        "REJECTED" -> Color(0xFFFFCDD2)
                        else -> Color(0xFFFFE082)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Refund Amount:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "PKR ${String.format("%,.0f", order.refundedAmount)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = when (order.refundStatus) {
                                "PROCESSED" -> Color(0xFF2E7D32)
                                "REJECTED" -> Color(0xFFC62828)
                                else -> Color(0xFFE65100)
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Spacer(Modifier.height(8.dp))

                    // Status Message
                    when (order.refundStatus) {
                        "PROCESSED" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        "✅ Refund Completed",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                    when (order.paymentMethod) {
                                        "wallet" -> Text(
                                            "Amount credited to your wallet",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        "stripe" -> Text(
                                            "Amount refunded to your card (3-5 days)",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        "cash_on_delivery" -> Text(
                                            "No refund needed (COD order)",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        "APPROVED" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFF1565C0),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Refund approved, processing...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF1565C0)
                                )
                            }
                        }
                        "REJECTED" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = Color(0xFFC62828),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        "❌ Refund Rejected",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC62828)
                                    )
                                    if (order.refundRejectionReason.isNotEmpty()) {
                                        Text(
                                            "Reason: ${order.refundRejectionReason}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        "PENDING_SELLER_APPROVAL" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "⏳ Waiting for seller approval",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                        "PENDING_ADMIN_APPROVAL" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "⏳ Waiting for admin approval",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                    }

                    // Reason
                    if (order.cancellationReason.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Reason: ${order.cancellationReason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // View Details Button
            Button(
                onClick = { navController.navigate("order_detail/${order.orderId}") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Order Details")
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun RefundStatusBadge(status: String) {
    val (text, color, icon) = when (status) {
        "PROCESSED" -> Triple("Refunded", Color(0xFF4CAF50), Icons.Default.CheckCircle)
        "APPROVED" -> Triple("Approved", Color(0xFF2196F3), Icons.Default.ThumbUp)
        "REJECTED" -> Triple("Rejected", Color(0xFFF44336), Icons.Default.Cancel)
        "PENDING_SELLER_APPROVAL" -> Triple("Pending", Color(0xFFFF9800), Icons.Default.HourglassEmpty)
        "PENDING_ADMIN_APPROVAL" -> Triple("Under Review", Color(0xFFFF9800), Icons.Default.Schedule)
        else -> Triple(status, Color.Gray, Icons.Default.Info)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun EmptyRefundState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Receipt,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No Refund History",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your refund requests will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatRefundDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
