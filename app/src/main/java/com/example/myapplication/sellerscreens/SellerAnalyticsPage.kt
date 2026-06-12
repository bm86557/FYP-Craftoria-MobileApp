package com.example.myapplication.sellerscreens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.myapplication.model.SellerAnalyticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerAnalyticsPage(
    navController: NavHostController,
    viewModel: SellerAnalyticsViewModel = viewModel()
) {
    val analytics by viewModel.analytics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadAnalytics()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Sales Analytics",
                        fontWeight = FontWeight.Bold
                    )
                },
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
        
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Revenue Section
                item {
                    Text(
                        "💰 Revenue Overview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnalyticsCard(
                            title = "Total Revenue",
                            value = "Rs. ${String.format("%,.0f", analytics.totalRevenue)}",
                            icon = Icons.Default.AttachMoney,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                        
                        AnalyticsCard(
                            title = "Your Profit",
                            value = "Rs. ${String.format("%,.0f", analytics.totalProfit)}",
                            subtitle = "After 5% commission",
                            icon = Icons.Default.TrendingUp,
                            color = Color(0xFF2196F3),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Orders Section
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "📦 Orders Summary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SmallAnalyticsCard(
                            title = "Total Orders",
                            value = analytics.totalOrders.toString(),
                            icon = Icons.Default.ShoppingBag,
                            color = Color(0xFF9C27B0),
                            modifier = Modifier.weight(1f)
                        )
                        
                        SmallAnalyticsCard(
                            title = "Completed",
                            value = analytics.completedOrders.toString(),
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SmallAnalyticsCard(
                            title = "Pending",
                            value = analytics.pendingOrders.toString(),
                            icon = Icons.Default.Schedule,
                            color = Color(0xFFFF9800),
                            modifier = Modifier.weight(1f)
                        )
                        
                        SmallAnalyticsCard(
                            title = "Processing",
                            value = analytics.processingOrders.toString(),
                            icon = Icons.Default.Autorenew,
                            color = Color(0xFF2196F3),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SmallAnalyticsCard(
                            title = "Cancelled",
                            value = analytics.cancelledOrders.toString(),
                            icon = Icons.Default.Cancel,
                            color = Color(0xFFF44336),
                            modifier = Modifier.weight(1f)
                        )
                        
                        SmallAnalyticsCard(
                            title = "Refunded",
                            value = analytics.refundedOrders.toString(),
                            icon = Icons.Default.MoneyOff,
                            color = Color(0xFFE91E63),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Products Section
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "📊 Products",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                item {
                    AnalyticsCard(
                        title = "Products Sold",
                        value = analytics.productsSold.toString(),
                        subtitle = "Total items delivered",
                        icon = Icons.Default.Inventory,
                        color = Color(0xFF673AB7),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Success Rate
                item {
                    Spacer(Modifier.height(8.dp))
                    val successRate = if (analytics.totalOrders > 0) {
                        (analytics.completedOrders.toFloat() / analytics.totalOrders * 100).toInt()
                    } else 0
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Success Rate",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "$successRate% orders completed",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                }
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            
                            LinearProgressIndicator(
                                progress = successRate / 100f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = Color(0xFF4CAF50),
                                trackColor = Color(0xFFE0E0E0)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun SmallAnalyticsCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}
