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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.pages.ReportOrderIssueDialog
import com.example.myapplication.pages.ReportSellerDialog
import com.example.myapplication.model.Order
import com.example.myapplication.model.OrdersViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailPage(
    orderId: String,
    navController: NavHostController,
    viewModel: OrdersViewModel = viewModel()
) {
    var order by remember { mutableStateOf<Order?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showReportOrderDialog by remember { mutableStateOf(false) }
    var showReportSellerDialog by remember { mutableStateOf(false) }
    var sellerName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Load order details
    LaunchedEffect(orderId) {
        try {
            isLoading = true
            val doc = FirebaseFirestore.getInstance()
                .collection("orders")
                .document(orderId)
                .get()
                .await()

            order = doc.toObject(Order::class.java)
            
            // Fetch seller name
            order?.let { ord ->
                if (ord.sellerId.isNotEmpty()) {
                    val sellerDoc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(ord.sellerId)
                        .get()
                        .await()
                    sellerName = sellerDoc.getString("name") ?: "Unknown Seller"
                }
            }
            
            isLoading = false
        } catch (e: Exception) {
            errorMessage = e.message
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Error loading order",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }
            order == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Order not found")
                }
            }
            else -> {
                OrderDetailContent(
                    order = order!!,
                    currentUserId = currentUserId,
                    viewModel = viewModel,
                    navController = navController,
                    modifier = Modifier.padding(padding),
                    showReportOrderDialog = showReportOrderDialog,
                    showReportSellerDialog = showReportSellerDialog,
                    onReportOrderClick = { showReportOrderDialog = true },
                    onReportSellerClick = { showReportSellerDialog = true },
                    sellerName = sellerName
                )
            }
        }
    }
    
    // Report Dialogs
    if (showReportOrderDialog && order != null) {
        ReportOrderIssueDialog(
            orderId = order!!.orderId,
            sellerId = order!!.sellerId,
            sellerName = sellerName,
            orderAmount = order!!.totalAmountPKR,
            onDismiss = { showReportOrderDialog = false }
        )
    }

    if (showReportSellerDialog && order != null) {
        ReportSellerDialog(
            sellerId = order!!.sellerId,
            sellerName = sellerName,
            orderId = order!!.orderId,
            orderAmount = order!!.totalAmountPKR,
            onDismiss = { showReportSellerDialog = false }
        )
    }
}

@Composable
private fun OrderDetailContent(
    order: Order,
    currentUserId: String,
    viewModel: OrdersViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    showReportOrderDialog: Boolean,
    showReportSellerDialog: Boolean,
    onReportOrderClick: () -> Unit,
    onReportSellerClick: () -> Unit,
    sellerName: String
) {
    val isBuyer = order.buyerId == currentUserId
    val isSeller = order.sellerId == currentUserId

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Order ID & Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Order ID",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Text(
                                text = "#${order.orderId.take(8).uppercase()}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        StatusBadge(status = order.status)
                    }

                    Spacer(Modifier.height(8.dp))

                    // Payment Method
                    Row(
                        verticalAlignment = Alignment.CenterVertically
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
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when (order.paymentMethod) {
                                "wallet" -> "Wallet"
                                "stripe" -> "Card Payment"
                                "cash_on_delivery" -> "Cash on Delivery"
                                else -> order.paymentMethod
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Delivery Address Card
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Delivery Address",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    val address = order.getAddressMap()
                    Text(
                        text = address["fullName"]?.toString() ?: "N/A",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = address["street"]?.toString() ?: "N/A",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = address["city"]?.toString() ?: "N/A",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = address["phone"]?.toString() ?: "N/A",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Products Header
        item {
            Text(
                text = "Products (${order.getItemsList().size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Product Items
        items(order.getItemsList()) { item ->
            ProductItemCard(item)
        }

        // Payment Breakdown Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Payment Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    // Buyer view
                    if (!isSeller) {
                        PaymentRow("Subtotal", order.totalAmountPKR)
                    }

                    // Seller view
                    if (isSeller) {
                        PaymentRow("Order Total", order.totalAmountPKR)

                        if (order.platformCommission > 0) {
                            PaymentRow(
                                "Platform Commission (5%)",
                                order.platformCommission,
                                isNegative = true
                            )
                        }

                        if (order.sellerAmount > 0) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            PaymentRow(
                                "Your Amount",
                                order.sellerAmount,
                                isHighlight = true
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Final total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSeller) "Order Total" else "Total Paid",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Rs. ${String.format("%.2f", order.totalAmountPKR)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // ✅ REFUND STATUS CARD (for cancelled orders)
        if (order.status == "CANCELLED" && order.refundStatus.isNotEmpty()) {
            item {
                RefundStatusCard(order = order, isBuyer = isBuyer)
            }
        }

        // ✅ REPORT BUTTONS (for buyers)
        if (isBuyer) {
            item {
                ReportButtonsSection(
                    order = order,
                    onReportOrderClick = onReportOrderClick,
                    onReportSellerClick = onReportSellerClick
                )
            }
        }

        // ✅ BUYER ACTION BUTTONS
        if (isBuyer && order.status !in listOf("COMPLETED", "CANCELLED")) {
            item {
                BuyerCancelButton(
                    order = order,
                    viewModel = viewModel,
                    navController = navController
                )
            }
        }

        // ✅ SELLER ACTION BUTTONS
        if (isSeller) {
            item {
                SellerActionButtons(
                    order = order,
                    viewModel = viewModel,
                    navController = navController
                )
            }
        }

        // Bottom spacing
        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ✅ REFUND STATUS CARD
@Composable
private fun RefundStatusCard(order: Order, isBuyer: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (order.refundStatus) {
                "PENDING_SELLER_APPROVAL", "PENDING_ADMIN_APPROVAL" -> Color(0xFFFFF3E0)
                "APPROVED", "PROCESSED" -> Color(0xFFE8F5E9)
                "REJECTED" -> Color(0xFFFFEBEE)
                else -> Color(0xFFF5F5F5)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Status header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (order.refundStatus) {
                        "PENDING_SELLER_APPROVAL" -> "⏳ Waiting for Seller Approval"
                        "PENDING_ADMIN_APPROVAL" -> "⏳ Waiting for Admin Approval"
                        "APPROVED" -> "✅ Refund Approved"
                        "PROCESSED" -> "✅ Refund Processed"
                        "REJECTED" -> "❌ Refund Rejected"
                        else -> "Refund Status: ${order.refundStatus}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (order.refundStatus) {
                        "PROCESSED" -> Color(0xFF2E7D32)
                        "REJECTED" -> Color(0xFFC62828)
                        else -> Color(0xFFE65100)
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Status details
            when (order.refundStatus) {
                "PENDING_SELLER_APPROVAL" -> {
                    Text(
                        "Your cancellation request has been sent to the seller for approval.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))

                    if (order.refundAmount > 0) {
                        Text(
                            "Refund Amount: Rs. ${String.format("%.2f", order.refundAmount)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    if (order.cancellationFee > 0) {
                        Text(
                            "Cancellation Fee (5%): Rs. ${String.format("%.2f", order.cancellationFee)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                "PENDING_ADMIN_APPROVAL" -> {
                    Text(
                        "Refund request is pending admin approval.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Refund Amount: Rs. ${String.format("%.2f", order.refundAmount)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }

                "APPROVED" -> {
                    Text(
                        "Your refund has been approved and is being processed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2E7D32)
                    )
                }

                "PROCESSED" -> {
                    Text(
                        "Rs. ${String.format("%.2f", order.refundedAmount)} has been refunded.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )

                    Spacer(Modifier.height(8.dp))

                    when (order.paymentMethod) {
                        "wallet" -> {
                            Text(
                                "✅ Refunded to your wallet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        "stripe" -> {
                            Text(
                                "✅ Refunded to your card",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF1565C0)
                            )
                            Text(
                                "Amount will appear within 5-10 business days",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        "cash_on_delivery" -> {
                            Text(
                                "ℹ️ No refund needed (COD order)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }

                "REJECTED" -> {
                    Text(
                        "Your refund request has been rejected.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFC62828)
                    )

                    if (order.refundRejectionReason.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Divider()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Reason: ${order.refundRejectionReason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Cancellation reason
            if (order.cancellationReason.isNotEmpty() && order.refundStatus != "REJECTED") {
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                Text(
                    "Cancellation Reason:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    order.cancellationReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

// ✅ BUYER CANCEL BUTTON
@Composable
private fun BuyerCancelButton(
    order: Order,
    viewModel: OrdersViewModel,
    navController: NavHostController
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column {
        Button(
            onClick = { showCancelDialog = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Cancel, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Cancel Order & Request Refund")
        }

        // Error message
        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE)
                )
            ) {
                Text(
                    text = "❌ $errorMessage",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFC62828)
                )
            }
        }
    }

    // Cancel Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { if (!isProcessing) showCancelDialog = false },
            title = { Text("Cancel Order?") },
            text = {
                Column {
                    Text("Are you sure you want to cancel this order?")

                    Spacer(Modifier.height(12.dp))

                    // Show cancellation fee
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "⚠️ Cancellation Fee: 5%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Spacer(Modifier.height(4.dp))
                            val fee = order.totalAmountPKR * 0.05
                            val refund = order.totalAmountPKR - fee
                            Text(
                                "Fee: Rs. ${String.format("%.2f", fee)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Refund: Rs. ${String.format("%.2f", refund)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Reason for cancellation") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        enabled = !isProcessing
                    )

                    if (isProcessing) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Sending request to seller...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cancelReason.isNotBlank()) {
                            isProcessing = true
                            errorMessage = null
                            viewModel.buyerCancelOrder(
                                orderId = order.orderId,
                                reason = cancelReason,
                                onSuccess = {
                                    isProcessing = false
                                    showCancelDialog = false
                                    navController.popBackStack()
                                },
                                onError = { error ->
                                    isProcessing = false
                                    errorMessage = error
                                }
                            )
                        }
                    },
                    enabled = !isProcessing && cancelReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Submit Request")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelDialog = false },
                    enabled = !isProcessing
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ✅ SELLER ACTION BUTTONS
@Composable
private fun SellerActionButtons(
    order: Order,
    viewModel: OrdersViewModel,
    navController: NavHostController
) {
    when (order.status) {
        "PAYMENT_CONFIRMED" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.updateStatus(order.orderId, "CONFIRMED")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("Accept")
                }

                OutlinedButton(
                    onClick = {
                        viewModel.sellerInitiateRefund(
                            orderId = order.orderId,
                            reason = "Order rejected by seller",
                            onSuccess = { navController.popBackStack() },
                            onError = { }
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reject")
                }
            }
        }
        "CONFIRMED" -> {
            Button(
                onClick = {
                    viewModel.updateStatus(order.orderId, "PROCESSING")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mark as Processing")
            }
        }
        "PROCESSING" -> {
            Button(
                onClick = {
                    viewModel.updateStatus(order.orderId, "SHIPPED")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mark as Shipped")
            }
        }
        "SHIPPED" -> {
            Button(
                onClick = {
                    viewModel.updateStatus(order.orderId, "DELIVERED")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mark as Delivered")
            }
        }
        "DELIVERED" -> {
            Button(
                onClick = {
                    viewModel.updateStatus(order.orderId, "COMPLETED")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Complete Order")
            }
        }
    }
}

@Composable
private fun ProductItemCard(item: Map<String, Any>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item["productName"]?.toString() ?: "Unknown Product",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(4.dp))

                val quantity = when (val qty = item["quantity"]) {
                    is Int -> qty
                    is Long -> qty.toInt()
                    is Double -> qty.toInt()
                    else -> 1
                }

                Text(
                    text = "Quantity: $quantity",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                val sellerName = item["sellerName"]?.toString()
                if (!sellerName.isNullOrEmpty()) {
                    Text(
                        text = "Seller: $sellerName",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val price = when (val p = item["price"]) {
                    is String -> p.toDoubleOrNull() ?: 0.0
                    is Double -> p
                    is Long -> p.toDouble()
                    is Int -> p.toDouble()
                    else -> 0.0
                }

                val quantity = when (val qty = item["quantity"]) {
                    is Int -> qty
                    is Long -> qty.toInt()
                    is Double -> qty.toInt()
                    else -> 1
                }

                val subtotal = price * quantity

                Text(
                    text = "Rs. ${String.format("%.2f", price)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Text(
                    text = "Rs. ${String.format("%.2f", subtotal)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PaymentRow(
    label: String,
    amount: Double,
    isNegative: Boolean = false,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isHighlight)
                MaterialTheme.typography.bodyLarge
            else
                MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isNegative) Color(0xFFC62828) else Color.Unspecified
        )
        Text(
            text = "${if (isNegative) "-" else ""}Rs. ${String.format("%.2f", amount)}",
            style = if (isHighlight)
                MaterialTheme.typography.bodyLarge
            else
                MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isNegative)
                Color(0xFFC62828)
            else if (isHighlight)
                MaterialTheme.colorScheme.primary
            else
                Color.Unspecified
        )
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (backgroundColor, textColor, displayText) = when (status) {
        "PAYMENT_CONFIRMED" -> Triple(Color(0xFFE3F2FD), Color(0xFF1976D2), "Payment Confirmed")
        "CONFIRMED" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Confirmed")
        "PROCESSING" -> Triple(Color(0xFFFFF3E0), Color(0xFFF57C00), "Processing")
        "SHIPPED" -> Triple(Color(0xFFE1F5FE), Color(0xFF0277BD), "Shipped")
        "DELIVERED" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Delivered")
        "COMPLETED" -> Triple(Color(0xFFC8E6C9), Color(0xFF1B5E20), "Completed")
        "CANCELLED" -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Cancelled")
        else -> Triple(Color.LightGray, Color.DarkGray, status)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Text(
            text = displayText,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

// ✅ REPORT BUTTONS SECTION
@Composable
private fun ReportButtonsSection(
    order: Order,
    onReportOrderClick: () -> Unit,
    onReportSellerClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF9C4)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Need Help?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "Report any issues with this order or seller",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReportOrderClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE91E63)
                    )
                ) {
                    Icon(
                        Icons.Default.ReportProblem,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Report Issue", fontSize = 13.sp)
                }
                
                OutlinedButton(
                    onClick = onReportSellerClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE91E63)
                    )
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Report Seller", fontSize = 13.sp)
                }
            }
        }
    }
}
