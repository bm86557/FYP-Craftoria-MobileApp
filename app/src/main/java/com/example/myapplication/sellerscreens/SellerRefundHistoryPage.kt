package com.example.myapplication.sellerscreens

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
fun SellerRefundHistoryPage(
    navController: NavHostController,
    viewModel: OrdersViewModel = viewModel()
) {
    val orders by viewModel.sellerOrders.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadSellerOrders()
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
                            "${refundOrders.size} refund cases",
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
            EmptySellerRefundState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(refundOrders) { order ->
                    SellerRefundCard(order = order, navController = navController)
                }
            }
        }
    }
}

@Composable
fun SellerRefundCard(
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
                        "Buyer: ${address["fullName"]}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatSellerDate(order.getCreatedAtLong()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SellerRefundStatusBadge(order.refundStatus)
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

            // Payment & Amount Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            when (order.paymentMethod) {
                                "wallet" -> Icons.Default.AccountBalanceWallet
                                "stripe" -> Icons.Default.CreditCard
                                else -> Icons.Default.Money
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            when (order.paymentMethod) {
                                "cash_on_delivery" -> "COD"
                                "wallet" -> "Wallet"
                                "stripe" -> "Card"
                                else -> order.paymentMethod
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Order: Rs. ${String.format("%,.0f", order.totalAmountPKR)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Seller Impact
                if (order.refundStatus == "PROCESSED") {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Your Loss",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC62828)
                        )
                        Text(
                            "Rs. ${String.format("%,.0f", order.sellerAmount)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )
                        Text(
                            "(order cancelled)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
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
                    // Refund Initiated By
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Initiated By:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            when (order.refundRequestedBy) {
                                "buyer" -> "🛒 Buyer"
                                "seller" -> "🏪 You (Seller)"
                                else -> order.refundRequestedBy
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Refund to Buyer:",
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
                                            "Buyer received wallet credit",
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        "stripe" -> Text(
                                            "Buyer received card refund",
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        "cash_on_delivery" -> Text(
                                            "No refund needed (COD)",
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
                                    Text(
                                        "Order restored to active",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF2E7D32)
                                    )
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
                                    "⏳ Waiting for your approval",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE65100),
                                    fontWeight = FontWeight.Bold
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

            // Action Button
            if (order.refundStatus == "PENDING_SELLER_APPROVAL") {
                Button(
                    onClick = { navController.navigate("seller_refund_requests") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Text("Review Request")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            } else {
                OutlinedButton(
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
}

@Composable
fun SellerRefundStatusBadge(status: String) {
    val (text, color, icon) = when (status) {
        "PROCESSED" -> Triple("Refunded", Color(0xFF4CAF50), Icons.Default.CheckCircle)
        "APPROVED" -> Triple("Approved", Color(0xFF2196F3), Icons.Default.ThumbUp)
        "REJECTED" -> Triple("Rejected", Color(0xFFF44336), Icons.Default.Cancel)
        "PENDING_SELLER_APPROVAL" -> Triple(
            "Action Needed",
            Color(0xFFFF5722),
            Icons.Default.Warning
        )
        "PENDING_ADMIN_APPROVAL" -> Triple(
            "Under Review",
            Color(0xFFFF9800),
            Icons.Default.Schedule
        )
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
fun EmptySellerRefundState() {
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
            "Refund cases will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatSellerDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
