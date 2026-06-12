package com.example.myapplication.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.model.ReportsViewModel

@Composable
fun CreateReportDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
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
        "Seller Issue",
        "Product Issue",
        "Order Issue",
        "Other"
    )

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = {
                onDismiss()
                onSuccess()
            },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50)) },
            title = { Text("Report Submitted") },
            text = { Text("Thank you for reporting. Our team will review your report and get back to you soon.") },
            confirmButton = {
                Button(onClick = {
                    onDismiss()
                    onSuccess()
                }) {
                    Text("OK")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Submit Report to Admin") },
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
