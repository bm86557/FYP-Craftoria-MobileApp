package com.example.myapplication.verification

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.model.VerificationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerVerificationScreen(
    navController: NavHostController,
    viewModel: VerificationViewModel = viewModel()
) {
    val context = LocalContext.current
    val verificationState by viewModel.verificationState.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    
    var showCamera by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedUri by remember { mutableStateOf<Uri?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    
    // ✅ USE: Camera permission handler function
    val cameraPermission = rememberCameraPermission()
    
    // ✅ Load verification status on screen open AND when returning to screen
    LaunchedEffect(Unit) {
        android.util.Log.d("SellerVerificationScreen", "Screen opened - refreshing status")
        viewModel.refreshVerificationStatus()
    }
    
    // ✅ CRITICAL: Force close camera if status is VERIFIED or PENDING
    LaunchedEffect(verificationState.status) {
        android.util.Log.d("SellerVerificationScreen", "Status changed to: ${verificationState.status}")
        
        // If status is VERIFIED or PENDING, force close camera and reset
        if (verificationState.status == "VERIFIED" || verificationState.status == "PENDING") {
            android.util.Log.d("SellerVerificationScreen", "Closing camera - status is ${verificationState.status}")
            showCamera = false
            capturedBitmap = null
            capturedUri = null
        }
    }
    
    // ✅ Only open camera if permission granted AND status allows it
    LaunchedEffect(cameraPermission.hasPermission, verificationState.status) {
        android.util.Log.d("SellerVerificationScreen", "Permission: ${cameraPermission.hasPermission}, Status: ${verificationState.status}")
        
        if (cameraPermission.hasPermission && 
            (verificationState.status == "NOT_SUBMITTED" || verificationState.status == "REJECTED")) {
            android.util.Log.d("SellerVerificationScreen", "Opening camera")
            showCamera = true
        } else {
            android.util.Log.d("SellerVerificationScreen", "NOT opening camera")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seller Verification") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(
                        onClick = {
                            isRefreshing = true
                            android.util.Log.d("SellerVerificationScreen", "Manual refresh clicked")
                            viewModel.refreshVerificationStatus()
                            // Reset refreshing state after a delay
                            coroutineScope.launch {
                                delay(1000)
                                isRefreshing = false
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Status",
                            tint = Color.White
                        )
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
        // ✅ CRITICAL: Only show camera if status allows AND showCamera is true
        val shouldShowCamera = showCamera && 
                              (verificationState.status == "NOT_SUBMITTED" || verificationState.status == "REJECTED")
        
        android.util.Log.d("SellerVerificationScreen", "Render - shouldShowCamera: $shouldShowCamera, showCamera: $showCamera, status: ${verificationState.status}")
        
        when {
            shouldShowCamera -> {
                FaceDetectionCamera(
                    onImageCaptured = { bitmap ->
                        // Save bitmap and close camera
                        val file = File(context.cacheDir, "selfie_${System.currentTimeMillis()}.jpg")
                        val outputStream = FileOutputStream(file)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                        outputStream.close()
                        
                        capturedUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            file
                        )
                        capturedBitmap = bitmap
                        showCamera = false
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Show refreshing indicator
                    if (isRefreshing) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    
                    // Debug info card (helpful for troubleshooting)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5F5)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Current Status: ${verificationState.status}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            val submittedTime = verificationState.submittedAt
                            if (submittedTime != null) {
                                Text(
                                    "Submitted: ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(submittedTime))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            val verifiedTime = verificationState.verifiedAt
                            if (verifiedTime != null) {
                                Text(
                                    "Verified: ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(verifiedTime))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    when (verificationState.status) {
                        "NOT_SUBMITTED" -> {
                            NotSubmittedContent(
                                capturedBitmap = capturedBitmap,
                                capturedUri = capturedUri,
                                isUploading = isUploading,
                                uploadError = uploadError,
                                onTakeSelfie = {
                                    // ✅ USE: Camera permission handler
                                    cameraPermission.requestPermission()
                                },
                                onSubmit = {
                                    capturedUri?.let { uri ->
                                        viewModel.submitVerification(
                                            context = context,
                                            imageUri = uri,
                                            onSuccess = {
                                                // Success handled by state
                                            },
                                            onError = { error ->
                                                // Error handled by state
                                            }
                                        )
                                    }
                                }
                            )
                        }
                        "PENDING" -> {
                            PendingContent(verificationState)
                        }
                        "VERIFIED" -> {
                            VerifiedContent(verificationState)
                        }
                        "REJECTED" -> {
                            RejectedContent(
                                verificationState = verificationState,
                                onRetry = {
                                    capturedBitmap = null
                                    capturedUri = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotSubmittedContent(
    capturedBitmap: Bitmap?,
    capturedUri: Uri?,
    isUploading: Boolean,
    uploadError: String?,
    onTakeSelfie: () -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.VerifiedUser,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Get Verified as a Seller",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Verified sellers get more trust and visibility",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
    
    Spacer(Modifier.height(24.dp))
    
    Text(
        "Instructions:",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(12.dp))
    
    InstructionItem("✓ Take a clear selfie")
    InstructionItem("✓ Look straight at the camera")
    InstructionItem("✓ Ensure good lighting")
    InstructionItem("✓ Remove sunglasses/mask")
    
    Spacer(Modifier.height(24.dp))
    
    if (capturedBitmap != null) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Preview:", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Image(
                    bitmap = capturedBitmap.asImageBitmap(),
                    contentDescription = "Captured Selfie",
                    modifier = Modifier
                        .size(200.dp)
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onTakeSelfie,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Retake")
                    }
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.weight(1f),
                        enabled = !isUploading
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text("Submit")
                        }
                    }
                }
            }
        }
    } else {
        Button(
            onClick = onTakeSelfie,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CameraAlt, null)
            Spacer(Modifier.width(8.dp))
            Text("Take Selfie")
        }
    }
    
    uploadError?.let {
        Spacer(Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = "Error: $it",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun PendingContent(state: com.example.myapplication.model.VerificationState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFFF9800)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Verification Pending",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your verification request is under review. We'll notify you once it's approved.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Tip: Use the refresh button (↻) in the top bar to check for updates.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF9800),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun VerifiedContent(state: com.example.myapplication.model.VerificationState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF4CAF50)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "✅ Verified Seller",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Congratulations! Your seller account is verified.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            
            // Show verification date if available
            state.verifiedAt?.let { timestamp ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "Verified on: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(timestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun RejectedContent(
    verificationState: com.example.myapplication.model.VerificationState,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Cancel,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFF44336)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Verification Rejected",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Reason: ${verificationState.rejectionReason}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun InstructionItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
