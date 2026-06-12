package com.example.myapplication.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.ReportItem
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportDetailsDialog(
    report: ReportItem,
    onDismiss: () -> Unit
) {
    // Log report details for debugging
    android.util.Log.d("ReportDetailsDialog", """
        📋 Report Details:
        - ID: ${report.id}
        - Status: ${report.status}
        - Admin Response: ${report.adminResponse}
        - Resolution: ${report.resolution}
        - Admin Notes: ${report.adminNotes}
        - Reviewed At: ${report.reviewedAt}
    """.trimIndent())
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Report Details")
                StatusBadge(status = report.status)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Report Info Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        DetailRow("Type", report.reportType.ifEmpty { report.complaintType })
                        DetailRow("Category", report.category)
                        DetailRow("Priority", report.priority.uppercase())
                        
                        if (report.targetName.isNotEmpty()) {
                            DetailRow("Target", report.targetName)
                        }
                        
                        if (report.subject.isNotEmpty()) {
                            DetailRow("Subject", report.subject)
                        }
                        
                        DetailRow(
                            "Submitted",
                            formatTimestamp(report.createdAt)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Description
                Text("Description:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = report.description,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp
                    )
                }

                // Admin Response Section
                if (report.status == "resolved" || report.status == "dismissed") {
                    Spacer(Modifier.height(20.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (report.status == "resolved") Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (report.status == "resolved") Color(0xFF4CAF50) else Color(0xFFC62828),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Admin Response",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (report.status == "resolved") 
                                Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (report.resolution.isNotEmpty()) {
                                Text(
                                    "Resolution:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    report.resolution,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(12.dp))
                            }

                            if (report.adminResponse.isNotEmpty()) {
                                Text(
                                    "Admin Notes:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    report.adminResponse,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(12.dp))
                            }

                            if (report.adminNotes.isNotEmpty()) {
                                Text(
                                    "Additional Notes:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    report.adminNotes,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(12.dp))
                            }

                            if (report.reviewedAt != null) {
                                Text(
                                    "Reviewed: ${formatTimestamp(report.reviewedAt)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else if (report.status == "under_review") {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE3F2FD)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF1976D2)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Your report is being reviewed by our team",
                                fontSize = 14.sp,
                                color = Color(0xFF1976D2)
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFF57C00),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Your report is pending review",
                                fontSize = 14.sp,
                                color = Color(0xFFF57C00)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatTimestamp(timestamp: Any?): String {
    return try {
        when (timestamp) {
            is Timestamp -> {
                val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                sdf.format(timestamp.toDate())
            }
            is Long -> {
                val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
            else -> "N/A"
        }
    } catch (e: Exception) {
        "N/A"
    }
}
