package com.example.myapplication.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.myapplication.model.AuthViewModel
import com.example.myapplication.model.CoSellerViewModel
import com.example.myapplication.model.CoSellerStoreModel

import com.example.myapplication.model.CloudinaryRepository.uploadImageToCloudinary

@Composable
fun AddProductPage(
    modifier: Modifier = Modifier, 
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
    coSellerViewModel: CoSellerViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var actualPrice by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    val context = LocalContext.current
    var minDealPrice by remember { mutableStateOf("") }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    val detailsMap = remember { mutableStateMapOf<String, String>() }
    
    // ✅ NEW: Get verification status
    val verificationStatus by authViewModel.verificationStatus.collectAsState()
    val isVerifiedSeller by authViewModel.isVerifiedSeller.collectAsState()
    
    // Co-Store related states
    var selectedDestination by remember { mutableStateOf("personal") } // "personal" or "costore"
    var selectedStore by remember { mutableStateOf<CoSellerStoreModel?>(null) }
    var showStoreDropdown by remember { mutableStateOf(false) }
    val myStores by coSellerViewModel.myStores
    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Fetch user's co-stores
    LaunchedEffect(Unit) {
        coSellerViewModel.fetchMyStores()
    }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
        ) { uris ->
            imageUris = uris
        }
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Text(text = "Add Product", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            // ✅ NEW: Verification Status Warning
            if (!isVerifiedSeller) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = when (verificationStatus) {
                            "PENDING" -> androidx.compose.ui.graphics.Color(0xFFFFF3E0)
                            "REJECTED" -> androidx.compose.ui.graphics.Color(0xFFFFEBEE)
                            else -> androidx.compose.ui.graphics.Color(0xFFF5F5F5)
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (verificationStatus) {
                                    "PENDING" -> "⏳"
                                    "REJECTED" -> "❌"
                                    else -> "🔒"
                                },
                                fontSize = 24.sp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = when (verificationStatus) {
                                        "PENDING" -> "Verification Pending"
                                        "REJECTED" -> "Verification Rejected"
                                        else -> "Verification Required"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = when (verificationStatus) {
                                        "PENDING" -> "Your verification is under review. You can add products once approved."
                                        "REJECTED" -> "Your verification was rejected. Please resubmit for verification."
                                        else -> "You must be a verified seller to add products."
                                    },
                                    fontSize = 14.sp,
                                    color = androidx.compose.ui.graphics.Color.Gray
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { navController.navigate("seller_verification") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                when (verificationStatus) {
                                    "PENDING" -> "Check Status"
                                    "REJECTED" -> "Resubmit Verification"
                                    else -> "Get Verified"
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Destination Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Add Product To:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Personal Product Option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedDestination == "personal",
                            onClick = { 
                                selectedDestination = "personal"
                                selectedStore = null
                            }
                        )
                        Text("My Personal Products")
                    }
                    
                    // Co-Store Option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedDestination == "costore",
                            onClick = { selectedDestination = "costore" }
                        )
                        Text("Co-Seller Store")
                    }
                    
                    // Store Selection Dropdown (only show if costore is selected)
                    if (selectedDestination == "costore") {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (myStores.isEmpty()) {
                            Text(
                                text = "No co-seller stores available. Create one first!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 40.dp)
                            )
                        } else {
                            Box(modifier = Modifier.padding(start = 40.dp)) {
                                OutlinedButton(
                                    onClick = { showStoreDropdown = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = selectedStore?.storeName ?: "Select Store",
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                                
                                DropdownMenu(
                                    expanded = showStoreDropdown,
                                    onDismissRequest = { showStoreDropdown = false }
                                ) {
                                    myStores.forEach { store ->
                                        DropdownMenuItem(
                                            text = { Text(store.storeName) },
                                            onClick = {
                                                selectedStore = store
                                                showStoreDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = isVerifiedSeller
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5,
                enabled = isVerifiedSeller
            )
            OutlinedTextField(
                value = actualPrice,
                onValueChange = { actualPrice = it },
                label = { Text("Original Price (will show strikethrough)") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = isVerifiedSeller
            )
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Discounted Price (will show bold)") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = isVerifiedSeller
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = isVerifiedSeller
            )

            OutlinedTextField(
                value = minDealPrice,
                onValueChange = { minDealPrice = it },
                label = { Text("Min Deal Price") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = isVerifiedSeller
            )
            Button(
                onClick = {
                    launcher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                enabled = isVerifiedSeller
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Images")
            }
            LazyRow {
                items(imageUris){uri ->
                    AsyncImage(model = uri,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp).padding(4.dp))

                }
            }
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text("Key (e.g.Brand)") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = isVerifiedSeller
            )
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Value (e.g.Samsung)") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = isVerifiedSeller
            )
            Button(
                onClick = {
                    if (key.isNotEmpty() && value.isNotEmpty()){
                        detailsMap[key] = value
                        key = ""
                        value = ""
                    }
                },
                enabled = isVerifiedSeller
            ) {
                Text("Add Detail")
            }
            detailsMap.forEach { (k , v)->
                Text("$k : $v")
            }
            
            // ✅ NEW: Show error message if any
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFFFEBEE)
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = androidx.compose.ui.graphics.Color(0xFFC62828)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    // ✅ NEW: Check verification first
                    if (!isVerifiedSeller) {
                        errorMessage = "You must be a verified seller to add products"
                        return@Button
                    }
                    
                    // Validation
                    if (title.isBlank() || description.isBlank() || price.isBlank() || 
                        actualPrice.isBlank() || category.isBlank() || isUploading) {
                        return@Button
                    }
                    
                    // Check if co-store is selected but no store chosen
                    if (selectedDestination == "costore" && selectedStore == null) {
                        return@Button
                    }
                    
                    errorMessage = null
                    isUploading = true
                    
                    if (imageUris.isNotEmpty()) {
                        var uploadedCount = 0
                        val collectedUrls = mutableListOf<String>()

                        imageUris.forEach { uri ->
                            uploadImageToCloudinary(
                                context = context,
                                uri = uri,
                                preset = "product_upload",
                                onSuccess = { imageUrl ->
                                    collectedUrls.add(imageUrl)
                                    uploadedCount++
                                    if (uploadedCount == imageUris.size) {
                                        // All images uploaded, now save product
                                        if (selectedDestination == "costore" && selectedStore != null) {
                                            // Add to Co-Store with uploaded images
                                            saveToCoStore(
                                                coSellerViewModel,
                                                selectedStore!!.storeId,
                                                title.trim(),
                                                description.trim(),
                                                price.trim(),
                                                actualPrice.trim(),
                                                category.trim(),
                                                minDealPrice.trim(),
                                                collectedUrls.toList(),
                                                detailsMap.toMap()
                                            ) { success ->
                                                isUploading = false
                                                if (success) {
                                                    navController.popBackStack()
                                                }
                                            }
                                        } else {
                                            // Add to Personal Products
                                            authViewModel.addProduct(
                                                title.trim(),
                                                description.trim(),
                                                price.trim(),
                                                actualPrice.trim(),
                                                category.trim(),
                                                minDealPrice.trim(),
                                                images = collectedUrls.toList(),
                                                otherDetails = detailsMap.toMap()
                                            ) { success, error ->
                                                isUploading = false
                                                if (success) {
                                                    navController.popBackStack()
                                                } else {
                                                    errorMessage = error ?: "Failed to add product"
                                                }
                                            }
                                        }
                                    }
                                },
                                onError = { error ->
                                    isUploading = false
                                }
                            )
                        }
                    } else {
                        // No images
                        if (selectedDestination == "costore" && selectedStore != null) {
                            // Add to Co-Store without images
                            saveToCoStore(
                                coSellerViewModel,
                                selectedStore!!.storeId,
                                title.trim(),
                                description.trim(),
                                price.trim(),
                                actualPrice.trim(),
                                category.trim(),
                                minDealPrice.trim(),
                                emptyList(),
                                detailsMap.toMap()
                            ) { success ->
                                isUploading = false
                                if (success) {
                                    navController.popBackStack()
                                }
                            }
                        } else {
                            // Add to Personal Products without images
                            authViewModel.addProduct(
                                title.trim(),
                                description.trim(),
                                price.trim(),
                                actualPrice.trim(),
                                category.trim(),
                                minDealPrice.trim(),
                                images = emptyList(),
                                otherDetails = detailsMap.toMap()
                            ) { success, error ->
                                isUploading = false
                                if (success) {
                                    navController.popBackStack()
                                } else {
                                    errorMessage = error ?: "Failed to add product"
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = isVerifiedSeller && title.isNotBlank() && description.isNotBlank() && 
                         price.isNotBlank() && actualPrice.isNotBlank() && 
                         category.isNotBlank() && !isUploading &&
                         (selectedDestination == "personal" || 
                          (selectedDestination == "costore" && selectedStore != null))
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Uploading...")
                } else {
                    val buttonText = if (selectedDestination == "costore") {
                        "Add to ${selectedStore?.storeName ?: "Store"}"
                    } else {
                        "Add Product"
                    }
                    Text(buttonText)
                }
            }

        }
    }
}

// Helper function to save product to co-store with already uploaded images
private fun saveToCoStore(
    viewModel: CoSellerViewModel,
    storeId: String,
    title: String,
    description: String,
    price: String,
    actualPrice: String,
    category: String,
    minDealPrice: String,
    images: List<String>,
    otherDetails: Map<String, String>,
    onResult: (Boolean) -> Unit
) {
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val currentUid = auth.currentUser?.uid ?: return
    
    try {
        // ✅ NEW: Check verification status first
        db.collection("users").document(currentUid).get()
            .addOnSuccessListener { userDoc ->
                val verificationStatus = userDoc.getString("verificationStatus") ?: "NOT_SUBMITTED"
                
                // ✅ Block if not verified
                if (verificationStatus != "VERIFIED") {
                    android.util.Log.e("AddProductPage", "Seller not verified: $verificationStatus")
                    onResult(false)
                    return@addOnSuccessListener
                }
                
                val sellerName = userDoc.getString("name") ?: ""
                
                // Get store name
                db.collection("coSellerStores").document(storeId).get()
                    .addOnSuccessListener { storeDoc ->
                        val storeName = storeDoc.getString("storeName") ?: ""
                        
                        val docRef = db.collection("data").document("stock")
                            .collection("products").document()

                        val product = mapOf(
                            "id" to docRef.id,
                            "sellerId" to currentUid,
                            "sellerName" to sellerName,
                            "title" to title,
                            "description" to description,
                            "price" to price,
                            "actualPrice" to actualPrice,
                            "category" to category,
                            "minDealPrice" to (minDealPrice.toIntOrNull() ?: 0),
                            "images" to images,
                            "otherDetails" to otherDetails,
                            "coStoreId" to storeId,
                            "coStoreName" to storeName,
                            "isCoStoreProduct" to true,
                            "status" to "pending",  // ✅ Product approval required
                            "rejectionReason" to null,
                            "reviewedBy" to null,
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )

                        docRef.set(product)
                            .addOnSuccessListener {
                                onResult(true)
                            }
                            .addOnFailureListener {
                                onResult(false)
                            }
                    }
                    .addOnFailureListener {
                        onResult(false)
                    }
            }
            .addOnFailureListener {
                onResult(false)
            }
    } catch (e: Exception) {
        onResult(false)
    }
}