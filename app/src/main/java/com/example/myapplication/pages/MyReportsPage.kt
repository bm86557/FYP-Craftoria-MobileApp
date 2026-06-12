package com.example.myapplication.pages

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.components.CreateReportDialog
import com.example.myapplication.components.ReportCard
import com.example.myapplication.components.ReportDetailsDialog
import com.example.myapplication.model.ReportItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsPage(
    navController: NavHostController
) {
    var reports by remember { mutableStateOf<List<ReportItem>>(emptyList()) }
    var complaints by remember { mutableStateOf<List<ReportItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedReport by remember { mutableStateOf<ReportItem?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val db = FirebaseFirestore.getInstance()

    // Function to load data
    suspend fun loadData() {
        try {
            isLoading = true
            android.util.Log.d("MyReportsPage", "📥 Fetching reports for user: $currentUserId")
            
            // Fetch Reports (without orderBy to avoid index requirement)
            val reportsSnap = db.collection("reports")
                .whereEqualTo("reportedBy", currentUserId)
                .get()
                .await()
            
            android.util.Log.d("MyReportsPage", "✅ Fetched ${reportsSnap.documents.size} reports")
            
            // Sort manually by createdAt
            reports = reportsSnap.documents.mapNotNull { doc ->
                val report = doc.toObject(ReportItem::class.java)?.copy(id = doc.id)
                android.util.Log.d("MyReportsPage", "Report ${doc.id}: status=${report?.status}, adminResponse=${report?.adminResponse}, resolution=${report?.resolution}")
                report
            }.sortedByDescending { 
                when (val timestamp = it.createdAt) {
                    is com.google.firebase.Timestamp -> timestamp.seconds
                    else -> 0L
                }
            }
            
            // Fetch Complaints (without orderBy to avoid index requirement)
            val complaintsSnap = db.collection("complaints")
                .whereEqualTo("complainantId", currentUserId)
                .get()
                .await()
            
            android.util.Log.d("MyReportsPage", "✅ Fetched ${complaintsSnap.documents.size} complaints")
            
            // Sort manually by createdAt
            complaints = complaintsSnap.documents.mapNotNull { doc ->
                val complaint = doc.toObject(ReportItem::class.java)?.copy(id = doc.id)
                android.util.Log.d("MyReportsPage", "Complaint ${doc.id}: status=${complaint?.status}, adminResponse=${complaint?.adminResponse}, resolution=${complaint?.resolution}")
                complaint
            }.sortedByDescending { 
                when (val timestamp = it.createdAt) {
                    is com.google.firebase.Timestamp -> timestamp.seconds
                    else -> 0L
                }
            }
            
            isLoading = false
        } catch (e: Exception) {
            android.util.Log.e("MyReportsPage", "❌ Error: ${e.message}", e)
            isLoading = false
        }
    }

    // Load reports and complaints
    LaunchedEffect(refreshTrigger) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Reports & Complaints") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Create Report", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Reports (${reports.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Complaints (${complaints.size})") }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val items = if (selectedTab == 0) reports else complaints
                
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No ${if (selectedTab == 0) "reports" else "complaints"} yet",
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items) { item ->
                            ReportCard(
                                report = item,
                                onClick = { selectedReport = item }
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Report Dialog
    if (showCreateDialog) {
        CreateReportDialog(
            onDismiss = { showCreateDialog = false },
            onSuccess = {
                showCreateDialog = false
                refreshTrigger++ // Trigger refresh
            }
        )
    }

    // Report Details Dialog
    if (selectedReport != null) {
        ReportDetailsDialog(
            report = selectedReport!!,
            onDismiss = { 
                selectedReport = null
                refreshTrigger++ // Trigger refresh when closing details
            }
        )
    }
}
