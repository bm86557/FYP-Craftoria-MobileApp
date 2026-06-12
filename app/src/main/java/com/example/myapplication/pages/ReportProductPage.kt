package com.example.myapplication.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.myapplication.model.ReportsViewModel

// ✅ Report Product Dialog
@Composable
fun ReportProductDialog(
    productId: String,
    productName: String,
    onDismiss: () -> Unit,
    viewModel: ReportsViewModel = viewModel()
) {
    var selectedCategory by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    val isSubmitting by viewModel.isSubmitting.collectAsState()

    val categories = listOf(
        "Fake/Counterfeit Product",
        "Misleading Description",
        "Inappropriate Content",
        "Prohibited Item",
        "Poor Quality",
        "Other"
    )

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50)) },
            title = { Text("Report Submitted") },
            text = { Text("Thank you for reporting. We'll review this product within 24 hours.") },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.Flag, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Report Product") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Product: $productName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Select reason:", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategory = category }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(category)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Additional details (optional)") },
                        placeholder = { Text("Describe the issue...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitProductReport(
                            productId = productId,
                            productName = productName,
                            category = selectedCategory,
                            description = description,
                            onSuccess = { 
                                android.util.Log.d("ReportDialog", "✅ Report submitted successfully")
                                showSuccess = true 
                            },
                            onError = { error ->
                                android.util.Log.e("ReportDialog", "❌ Error: $error")
                            }
                        )
                    },
                    enabled = selectedCategory.isNotEmpty() && !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ✅ Report Seller Dialog
@Composable
fun ReportSellerDialog(
    sellerId: String,
    sellerName: String,
    orderId: String = "",
    orderAmount: Double = 0.0,
    onDismiss: () -> Unit,
    viewModel: ReportsViewModel = viewModel()
) {
    var selectedCategory by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    val isSubmitting by viewModel.isSubmitting.collectAsState()

    val categories = listOf(
        "Fraud/Scam",
        "Fake Products",
        "Poor Service",
        "Delayed Delivery",
        "Rude Behavior",
        "Not Responding",
        "Other"
    )

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50)) },
            title = { Text("Complaint Submitted") },
            text = { Text("Your complaint has been submitted. Our team will investigate and contact you soon.") },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Report Seller") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Seller: $sellerName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (orderId.isNotEmpty()) {
                        Text(
                            "Order: #${orderId.take(8).uppercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Select complaint type:", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategory = category }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(category)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        placeholder = { Text("Brief summary") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description *") },
                        placeholder = { Text("Describe the issue in detail...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitSellerComplaint(
                            sellerId = sellerId,
                            sellerName = sellerName,
                            orderId = orderId,
                            orderAmount = orderAmount,
                            category = selectedCategory,
                            subject = subject.ifEmpty { selectedCategory },
                            description = description,
                            onSuccess = { showSuccess = true },
                            onError = { /* Handle error */ }
                        )
                    },
                    enabled = selectedCategory.isNotEmpty() && description.isNotEmpty() && !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Submit Complaint")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ✅ Report Order Issue Dialog
@Composable
fun ReportOrderIssueDialog(
    orderId: String,
    sellerId: String,
    sellerName: String,
    orderAmount: Double,
    onDismiss: () -> Unit,
    viewModel: ReportsViewModel = viewModel()
) {
    var selectedCategory by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    val isSubmitting by viewModel.isSubmitting.collectAsState()

    val categories = listOf(
        "Wrong Item Received",
        "Damaged Product",
        "Missing Items",
        "Delayed Delivery",
        "Poor Packaging",
        "Not as Described",
        "Other"
    )

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50)) },
            title = { Text("Issue Reported") },
            text = { Text("Your order issue has been reported. We'll investigate and get back to you soon.") },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.ReportProblem, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Report Order Issue") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Order #${orderId.take(8).uppercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Amount: PKR ${String.format("%,.0f", orderAmount)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Seller: $sellerName",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("What's the issue?", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategory = category }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(category)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Describe the issue *") },
                        placeholder = { Text("Provide details...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitOrderIssue(
                            orderId = orderId,
                            sellerId = sellerId,
                            sellerName = sellerName,
                            orderAmount = orderAmount,
                            category = selectedCategory,
                            description = description,
                            onSuccess = { showSuccess = true },
                            onError = { /* Handle error */ }
                        )
                    },
                    enabled = selectedCategory.isNotEmpty() && description.isNotEmpty() && !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ✅ Report Technical Issue Dialog
@Composable
fun ReportTechnicalIssueDialog(
    onDismiss: () -> Unit,
    viewModel: ReportsViewModel = viewModel()
) {
    var selectedCategory by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }
    val isSubmitting by viewModel.isSubmitting.collectAsState()

    val categories = listOf(
        "App Crash",
        "Payment Issue",
        "Login Problem",
        "Slow Performance",
        "Feature Not Working",
        "UI/Display Issue",
        "Other"
    )

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50)) },
            title = { Text("Report Submitted") },
            text = { Text("Thank you for reporting. Our technical team will investigate this issue.") },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.BugReport, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Report Technical Issue") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Select issue type:", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategory = category }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(category)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        placeholder = { Text("Brief summary") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description *") },
                        placeholder = { Text("Describe what happened, steps to reproduce...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitTechnicalIssue(
                            category = selectedCategory,
                            subject = subject.ifEmpty { selectedCategory },
                            description = description,
                            onSuccess = { showSuccess = true },
                            onError = { /* Handle error */ }
                        )
                    },
                    enabled = selectedCategory.isNotEmpty() && description.isNotEmpty() && !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
