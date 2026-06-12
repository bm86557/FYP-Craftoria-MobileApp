package com.example.myapplication.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.ReportItem

@Composable
fun ReportCard(
    report: ReportItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        report.reportType == "product" -> Color(0xFFFFF3E0)
                        report.reportType == "technical" -> Color(0xFFE3F2FD)
                        report.complaintType == "seller" -> Color(0xFFFFEBEE)
                        else -> Color(0xFFF5F5F5)
                    }
                ) {
                    Text(
                        text = report.reportType.ifEmpty { report.complaintType }.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            report.reportType == "product" -> Color(0xFFF57C00)
                            report.reportType == "technical" -> Color(0xFF1976D2)
                            report.complaintType == "seller" -> Color(0xFFC62828)
                            else -> Color.Gray
                        }
                    )
                }

                // Status Badge
                StatusBadge(status = report.status)
            }

            Spacer(Modifier.height(12.dp))

            // Title
            Text(
                text = report.targetName.ifEmpty { report.subject.ifEmpty { report.category } },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(Modifier.height(4.dp))

            // Category
            Text(
                text = report.category,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(Modifier.height(8.dp))

            // Description
            Text(
                text = report.description,
                fontSize = 14.sp,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Admin Response (if any)
            if (report.adminResponse.isNotEmpty() || report.resolution.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Admin Response Available",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4CAF50),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor, text) = when (status) {
        "pending" -> Triple(Color(0xFFFFF3E0), Color(0xFFF57C00), "Pending")
        "under_review" -> Triple(Color(0xFFE3F2FD), Color(0xFF1976D2), "Under Review")
        "resolved" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Resolved")
        "dismissed" -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Dismissed")
        else -> Triple(Color(0xFFF5F5F5), Color.Gray, status)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
