package com.example.myapplication.sellerscreens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerRefundRequestsPage(
    navController: NavHostController,
    viewModel: OrdersViewModel = viewModel()
) {
    val sellerOrders by viewModel.sellerOrders.collectAsState()

    // Filter orders with pending refund requests
    val pendingRefunds = sellerOrders.filter {
        it.refundStatus == "PENDING_SELLER_APPROVAL"
    }

    LaunchedEffect(Unit) {
        viewModel.loadSellerOrders()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Refund Requests (${pendingRefunds.size})") },
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
        if (pendingRefunds.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No pending refund requests",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pendingRefunds) { order ->
                    RefundRequestCard(
                        order = order,
                        viewModel = viewModel,
                        onSuccess = {
                            // Refresh list
                            viewModel.loadSellerOrders()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RefundRequestCard(
    order: Order,
    viewModel: OrdersViewModel,
    onSuccess: () -> Unit
) {
    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Order ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Order #${order.orderId.take(8).uppercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE65100)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Refund details
            Text(
                "Refund Amount: Rs. ${String.format("%.2f", order.refundAmount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )

            Spacer(Modifier.height(8.dp))

            // Reason
            Text(
                "Reason:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                order.refundReason,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showRejectDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reject")
                }

                Button(
                    onClick = { showApproveDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Approve")
                }
            }
        }
    }

    // Approve Dialog
    if (showApproveDialog) {
        AlertDialog(
            onDismissRequest = { if (!isProcessing) showApproveDialog = false },
            title = { Text("Approve Refund?") },
            text = {
                Text("Rs. ${String.format("%.2f", order.refundAmount)} will be refunded to the buyer.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessing = true
                        viewModel.sellerRespondToRefund(
                            orderId = order.orderId,
                            approved = true,
                            onSuccess = {
                                isProcessing = false
                                showApproveDialog = false
                                onSuccess()
                            },
                            onError = {
                                isProcessing = false
                            }
                        )
                    },
                    enabled = !isProcessing
                ) {
                    Text("Approve")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showApproveDialog = false },
                    enabled = !isProcessing
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reject Dialog
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { if (!isProcessing) showRejectDialog = false },
            title = { Text("Reject Refund?") },
            text = {
                Column {
                    Text("Please provide a reason for rejection:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectReason.isNotBlank()) {
                            isProcessing = true
                            viewModel.sellerRespondToRefund(
                                orderId = order.orderId,
                                approved = false,
                                reason = rejectReason,
                                onSuccess = {
                                    isProcessing = false
                                    showRejectDialog = false
                                    onSuccess()
                                },
                                onError = {
                                    isProcessing = false
                                }
                            )
                        }
                    },
                    enabled = !isProcessing && rejectReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRejectDialog = false },
                    enabled = !isProcessing
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
