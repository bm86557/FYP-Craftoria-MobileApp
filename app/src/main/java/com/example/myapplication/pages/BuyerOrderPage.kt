package com.example.myapplication.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.model.Order
import com.example.myapplication.model.OrdersViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerOrdersPage(
    navController: NavHostController,
    viewModel: OrdersViewModel = viewModel()
) {
    val orders by viewModel.buyerOrders.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Pending", "Processing", "Delivered", "Cancelled")

    LaunchedEffect(Unit) {
        viewModel.loadBuyerOrders()
    }

    // Filter orders
    val filteredOrders = when (selectedFilter) {
        "Pending" -> orders.filter { it.status in listOf("PENDING", "PAYMENT_CONFIRMED", "CONFIRMED") }
        "Processing" -> orders.filter { it.status in listOf("PROCESSING", "SHIPPED") }
        "Delivered" -> orders.filter { it.status in listOf("DELIVERED", "COMPLETED") }
        "Cancelled" -> orders.filter { it.status == "CANCELLED" }
        else -> orders
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("My Orders", fontWeight = FontWeight.Bold)
                        Text(
                            "${orders.size} total orders",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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

            Divider()

            // Orders List
            if (filteredOrders.isEmpty()) {
                EmptyOrdersState(selectedFilter)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredOrders) { order ->
                        BuyerOrderCard(
                            order = order,
                            viewModel = viewModel,
                            onClick = {
                                navController.navigate("order_detail/${order.orderId}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BuyerOrderCard(
    order: Order,
    viewModel: OrdersViewModel,
    onClick: () -> Unit
) {
    val address = order.getAddressMap()
    val items = order.getItemsList()
    var showCancelDialog by remember { mutableStateOf(false) }
    val isCancelling by viewModel.isCancelling.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Order ID and Status
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
                        formatDate(System.currentTimeMillis()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                OrderStatusBadge(order.status)
            }

            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(12.dp))

            // Items Summary
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.ShoppingBag,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${items.size} item(s)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(8.dp))

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

            // Total Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Total Amount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "PKR ${String.format("%,.0f", order.totalAmountPKR)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Refund Info (if refund approved/processed)
            if (order.status == "CANCELLED" && order.refundStatus.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                
                val (refundText, refundColor, refundIcon) = when (order.refundStatus) {
                    "PENDING_SELLER_APPROVAL" -> Triple(
                        "Refund Pending: PKR ${String.format("%,.0f", order.refundAmount)} (Awaiting Seller Approval)",
                        MaterialTheme.colorScheme.tertiary,
                        Icons.Default.Schedule
                    )
                    "PENDING_ADMIN_APPROVAL" -> Triple(
                        "Refund Pending: PKR ${String.format("%,.0f", order.refundAmount)} (Awaiting Admin Approval)",
                        MaterialTheme.colorScheme.tertiary,
                        Icons.Default.Schedule
                    )
                    "APPROVED" -> Triple(
                        "Refund Approved: PKR ${String.format("%,.0f", order.refundAmount)} (Processing...)",
                        MaterialTheme.colorScheme.primary,
                        Icons.Default.CheckCircle
                    )
                    "PROCESSED" -> Triple(
                        "Refunded: PKR ${String.format("%,.0f", order.refundedAmount)}",
                        Color(0xFF4CAF50),
                        Icons.Default.DoneAll
                    )
                    "REJECTED" -> Triple(
                        "Refund Rejected: PKR ${String.format("%,.0f", order.refundAmount)}",
                        MaterialTheme.colorScheme.error,
                        Icons.Default.Cancel
                    )
                    else -> Triple(
                        "Refund Status: ${order.refundStatus}",
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        Icons.Default.Info
                    )
                }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = refundColor.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            refundIcon,
                            contentDescription = null,
                            tint = refundColor
                        )
                        Column {
                            Text(
                                refundText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = refundColor
                            )
                            if (order.cancellationReason.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Reason: ${order.cancellationReason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (order.refundStatus == "REJECTED" && order.refundRejectionReason.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Rejection: ${order.refundRejectionReason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons
            if (order.status in listOf("PAYMENT_CONFIRMED", "PENDING", "CONFIRMED")) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier.weight(1f),
                        enabled = !isCancelling
                    ) {
                        if (isCancelling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Cancel, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = onClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Track Order")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Details")
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    // Cancel Dialog
    if (showCancelDialog) {
        CancelOrderDialog(
            order = order,
            viewModel = viewModel,
            onDismiss = { showCancelDialog = false }
        )
    }
}

@Composable
fun OrderStatusBadge(status: String) {
    val (text, color, icon) = when (status) {
        "PAYMENT_CONFIRMED" -> Triple("Confirmed", Color(0xFF2196F3), Icons.Default.CheckCircle)
        "PENDING" -> Triple("Pending", Color(0xFFFF9800), Icons.Default.Schedule)
        "CONFIRMED" -> Triple("Confirmed", Color(0xFF4CAF50), Icons.Default.CheckCircle)
        "PROCESSING" -> Triple("Processing", Color(0xFF2196F3), Icons.Default.Autorenew)
        "SHIPPED" -> Triple("Shipped", Color(0xFF9C27B0), Icons.Default.LocalShipping)
        "DELIVERED" -> Triple("Delivered", Color(0xFF4CAF50), Icons.Default.Done)
        "COMPLETED" -> Triple("Completed", Color(0xFF4CAF50), Icons.Default.DoneAll)
        "CANCELLED" -> Triple("Cancelled", Color(0xFFF44336), Icons.Default.Cancel)
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
fun EmptyOrdersState(filter: String) {
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
            "Your orders will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CancelOrderDialog(
    order: Order,
    viewModel: OrdersViewModel,
    onDismiss: () -> Unit
) {
    var cancelReason by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isCancelling by viewModel.isCancelling.collectAsState()

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50)) },
            title = { Text("Order Cancelled") },
            text = { Text("Your order has been cancelled successfully. Refund will be processed shortly.") },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("Cancel Order?") },
            text = {
                Column {
                    Text("Are you sure you want to cancel this order?")
                    Spacer(Modifier.height(12.dp))
                    
                    Card(colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Order #${order.orderId.take(8).uppercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Amount: PKR ${String.format("%,.0f", order.totalAmountPKR)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    when (order.paymentMethod) {
                        "wallet" -> Text(
                            "💰 Refund will be credited to your wallet instantly",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        "stripe" -> Text(
                            "💳 Refund will be processed to your card (3-5 business days)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        "cash_on_delivery" -> Text(
                            "💵 No refund needed (COD order)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Reason (optional)") },
                        placeholder = { Text("e.g., Changed my mind") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                    
                    errorMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.buyerCancelOrder(
                            orderId = order.orderId,
                            reason = cancelReason.ifEmpty { "Cancelled by buyer" },
                            onSuccess = {
                                showSuccess = true
                            },
                            onError = { error ->
                                errorMessage = error
                            }
                        )
                    },
                    enabled = !isCancelling,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (isCancelling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Yes, Cancel")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isCancelling
                ) {
                    Text("Keep Order")
                }
            }
        )
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
