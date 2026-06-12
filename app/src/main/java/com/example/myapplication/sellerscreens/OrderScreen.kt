package com.example.myapplication.sellerscreens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.model.Order
import com.example.myapplication.model.OrdersViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OrderScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController? = null,
    viewModel: OrdersViewModel = viewModel()
) {
    val orders by viewModel.sellerOrders.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Pending", "Processing", "Completed", "Cancelled")

    LaunchedEffect(Unit) {
        viewModel.loadSellerOrders()
    }

    // Filter orders
    val filteredOrders = when (selectedFilter) {
        "Pending" -> orders.filter { it.status in listOf("PENDING", "PAYMENT_CONFIRMED") }
        "Processing" -> orders.filter { it.status in listOf("CONFIRMED", "PROCESSING", "SHIPPED") }
        "Completed" -> orders.filter { it.status in listOf("DELIVERED", "COMPLETED") }
        "Cancelled" -> orders.filter { it.status == "CANCELLED" }
        else -> orders
    }

    // Count pending refund requests
    val pendingRefunds = orders.count { it.refundStatus == "PENDING_SELLER_APPROVAL" }

    Column(
        modifier = modifier
            .fillMaxSize()
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
                    "Incoming Orders",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${orders.size} total orders",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Refund Requests Button
            if (pendingRefunds > 0 && navController != null) {
                Badge(
                    containerColor = Color(0xFFFF5722)
                ) {
                    IconButton(
                        onClick = { navController.navigate("seller_refund_requests") }
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Refund Requests",
                            tint = Color(0xFFFF5722)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Refund Alert Banner
        if (pendingRefunds > 0 && navController != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("seller_refund_requests") },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800)
                        )
                        Text(
                            "$pendingRefunds refund request${if (pendingRefunds > 1) "s" else ""} pending",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("Review →", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters.size) { index ->
                val filter = filters[index]
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) },
                    leadingIcon = if (selectedFilter == filter) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Orders List
        if (filteredOrders.isEmpty()) {
            EmptySellerOrdersState(selectedFilter)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredOrders) { order ->
                    SellerOrderCard(
                        order = order,
                        viewModel = viewModel,
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun SellerOrderCard(
    order: Order,
    viewModel: OrdersViewModel,
    navController: NavHostController?
) {
    val address = order.getAddressMap()
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                order.refundStatus == "PENDING_SELLER_APPROVAL" -> Color(0xFFFFF3E0)
                order.status == "CANCELLED" -> Color(0xFFFFEBEE)
                order.status == "COMPLETED" -> Color(0xFFE8F5E9)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                }

                // Refund Badge
                if (order.refundStatus == "PENDING_SELLER_APPROVAL") {
                    Badge(containerColor = Color(0xFFFF9800)) {
                        Text("Refund Pending", color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Order Details
            Text("Phone: ${address["phone"]}")
            Text("Address: ${address["street"]}, ${address["city"]}")

            Spacer(Modifier.height(8.dp))

            // Payment Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Total: Rs. ${String.format("%.0f", order.totalAmountPKR)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Payment: ${
                            when (order.paymentMethod) {
                                "cash_on_delivery" -> "💵 COD"
                                "wallet" -> "💰 Wallet"
                                "stripe" -> "💳 Card"
                                else -> order.paymentMethod
                            }
                        }",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Seller Amount
                if (order.sellerAmount > 0 && order.status != "CANCELLED") {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Your Amount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Rs. ${String.format("%.0f", order.sellerAmount)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "(after 5% commission)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Status
            SellerOrderStatusBadge(order.status)

            // Refund Info
            if (order.status == "CANCELLED") {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEBEE)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "❌ Order Cancelled",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828)
                            )

                            if (order.refundStatus == "PENDING_SELLER_APPROVAL") {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "⏳ Buyer requested refund - Action required",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE65100)
                                )
                            } else if (order.refundedAmount > 0) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Refunded: Rs. ${String.format("%.0f", order.refundedAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF2E7D32)
                                )
                            }

                            if (order.cancellationReason.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Reason: ${order.cancellationReason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (order.status) {
                        "PAYMENT_CONFIRMED", "PENDING" -> {
                            Button(
                                onClick = { viewModel.updateStatus(order.orderId, "CONFIRMED") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Confirm ✅")
                            }
                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Cancel")
                            }
                        }

                        "CONFIRMED" -> {
                            Button(
                                onClick = { viewModel.updateStatus(order.orderId, "COMPLETED") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Complete 🎉")
                            }
                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Cancel")
                            }
                        }

                        "COMPLETED" -> {
                            Text(
                                "✅ Completed",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        "CANCELLED" -> {
                            if (order.refundStatus == "PENDING_SELLER_APPROVAL" && navController != null) {
                                Button(
                                    onClick = { navController.navigate("seller_refund_requests") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFF9800)
                                    )
                                ) {
                                    Text("Review Refund Request")
                                }
                            } else {
                                Text(
                                    "❌ Cancelled",
                                    color = Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Success/Error Messages
                if (showSuccessMessage) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "✅ Order cancelled and refund initiated!",
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                errorMessage?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "❌ $error",
                        color = Color(0xFFC62828),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Cancel Dialog
        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = { Text("Cancel Order?") },
                text = {
                    Column {
                        Text("Are you sure you want to cancel this order?")
                        Spacer(Modifier.height(8.dp))

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF3E0)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Order #${order.orderId.take(8).uppercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Buyer: ${address["fullName"]}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Amount: Rs. ${String.format("%.0f", order.totalAmountPKR)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Refund info
                        when (order.paymentMethod) {
                            "wallet" -> Text(
                                "💰 Full refund to buyer's wallet",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32)
                            )

                            "stripe" -> Text(
                                "💳 Full refund to buyer's card (5-10 days)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1565C0)
                            )

                            "cash_on_delivery" -> Text(
                                "💵 No refund needed (COD order)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = cancelReason,
                            onValueChange = { cancelReason = it },
                            label = { Text("Reason (required)") },
                            placeholder = { Text("e.g., Out of stock, Wrong product") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "ℹ️ Admin approval required for seller-initiated refunds",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (cancelReason.isNotEmpty()) {
                                viewModel.sellerInitiateRefund(
                                    orderId = order.orderId,
                                    reason = cancelReason,
                                    onSuccess = {
                                        showCancelDialog = false
                                        showSuccessMessage = true
                                        errorMessage = null
                                        cancelReason = ""
                                    },
                                    onError = { error ->
                                        showCancelDialog = false
                                        errorMessage = error
                                        showSuccessMessage = false
                                    }
                                )
                            }
                        },
                        enabled = cancelReason.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Cancel & Request Refund")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelDialog = false }) {
                        Text("Keep Order")
                    }
                }
            )
        }
}

@Composable
fun SellerOrderStatusBadge(status: String) {
    val (text, color) = when (status) {
        "PAYMENT_CONFIRMED" -> "Payment Confirmed" to Color(0xFF2196F3)
        "PENDING" -> "Pending" to Color(0xFFFF9800)
        "CONFIRMED" -> "Confirmed" to Color(0xFF4CAF50)
        "PROCESSING" -> "Processing" to Color(0xFF2196F3)
        "SHIPPED" -> "Shipped" to Color(0xFF9C27B0)
        "DELIVERED" -> "Delivered" to Color(0xFF4CAF50)
        "COMPLETED" -> "Completed" to Color(0xFF4CAF50)
        "CANCELLED" -> "Cancelled" to Color(0xFFF44336)
        else -> status to Color.Gray
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun EmptySellerOrdersState(filter: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ShoppingCart,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No ${if (filter == "All") "" else filter} Orders",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Orders will appear here when customers place them",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
