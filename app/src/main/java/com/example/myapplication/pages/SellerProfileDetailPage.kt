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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myapplication.model.CoSellerStoreModel
import com.example.myapplication.model.CoSellerViewModel
import com.example.myapplication.model.ProductModel
import com.example.myapplication.model.SellerProfile
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerProfileDetailPage(
    sellerId: String,
    storeId: String?,
    navController: NavController,
    viewModel: CoSellerViewModel = viewModel()
) {
    var sellerProfile by remember { mutableStateOf<SellerProfile?>(null) }
    var sellerProducts by remember { mutableStateOf<List<ProductModel>>(emptyList()) }
    var store by remember { mutableStateOf<CoSellerStoreModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var isInviting by remember { mutableStateOf(false) }
    val inviteState by viewModel.inviteState
    
    val db = FirebaseFirestore.getInstance()
    
    LaunchedEffect(sellerId, storeId) {
        // Fetch seller profile
        db.collection("users").document(sellerId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    sellerProfile = SellerProfile(
                        sellerId = sellerId,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        profileImage = doc.getString("profileImage") ?: "",
                        role = doc.getString("role") ?: "seller"
                    )
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
        
        // Fetch seller's products
        db.collection("data").document("stock")
            .collection("products")
            .whereEqualTo("sellerId", sellerId)
            .get()
            .addOnSuccessListener { docs ->
                sellerProducts = docs.toObjects(ProductModel::class.java)
            }
        
        // Fetch store information if storeId provided
        if (storeId != null) {
            viewModel.fetchStoreById(storeId) { fetchedStore ->
                store = fetchedStore
            }
        }
    }
    
    LaunchedEffect(inviteState) {
        if (inviteState == "success") {
            showInviteDialog = false
            viewModel.inviteState.value = ""
        }
    }
    
    // ✅ UPDATED: Check if seller is already a member (support both old and new fields)
    val isMember = store?.let { 
        it.ownerSellerId == sellerId || 
        it.coSellerIds.contains(sellerId) ||
        (it.coSellerId.isNotEmpty() && it.coSellerId == sellerId)
    } ?: false

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seller Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (sellerProfile == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Seller not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Header
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Profile Image
                            Surface(
                                modifier = Modifier.size(100.dp),
                                shape = RoundedCornerShape(50.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                if (sellerProfile!!.profileImage.isNotEmpty()) {
                                    AsyncImage(
                                        model = sellerProfile!!.profileImage,
                                        contentDescription = "Profile",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            null,
                                            modifier = Modifier.size(50.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Text(
                                sellerProfile!!.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(Modifier.height(4.dp))
                            
                            Text(
                                sellerProfile!!.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            
                            Spacer(Modifier.height(8.dp))
                            
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "Seller",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
                
                // Stats Card
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${sellerProducts.size}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Products",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
                
                // Invite Button (if storeId provided)
                if (storeId != null) {
                    item {
                        if (isMember) {
                            // Show "Already Added" button (disabled)
                            OutlinedButton(
                                onClick = { },
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.CheckCircle, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Already Added to Store")
                            }
                        } else {
                            // Show "Invite to Store" button (enabled)
                            Button(
                                onClick = { showInviteDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PersonAdd, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Invite to Store")
                            }
                        }
                    }
                }
                
                // Products Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Products",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${sellerProducts.size} items",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                if (sellerProducts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Inventory,
                                    null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No products yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else {
                    items(sellerProducts) { product ->
                        SellerProductCard(product = product)
                    }
                }
            }
        }
        
        // Invite Dialog
        if (showInviteDialog && storeId != null) {
            AlertDialog(
                onDismissRequest = { 
                    if (!isInviting) {
                        showInviteDialog = false
                        viewModel.inviteState.value = ""
                    }
                },
                title = { Text("Invite Seller") },
                text = {
                    Column {
                        Text("Invite ${sellerProfile?.name} to your store?")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            sellerProfile?.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        
                        if (inviteState == "not_seller") {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "This user is not a seller",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else if (inviteState == "not_verified") {
                            Spacer(Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFF3E0)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFFF6F00),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                "Seller Not Verified",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFE65100)
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "This seller has not completed seller verification yet. Only verified sellers can be invited to co-seller stores.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFEF6C00)
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "Ask them to complete verification from Profile → Seller Verification.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFEF6C00),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (inviteState == "error") {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Failed to send invite. Please try again.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isInviting = true
                            viewModel.inviteCoSeller(sellerProfile?.email ?: "", storeId)
                            isInviting = false
                        },
                        enabled = !isInviting
                    ) {
                        if (isInviting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else {
                            Text("Send Invite")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            showInviteDialog = false
                            viewModel.inviteState.value = ""
                        },
                        enabled = !isInviting
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SellerProductCard(product: ProductModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Product Image
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (product.getImagesList().isNotEmpty()) {
                    AsyncImage(
                        model = product.getImagesList().first(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Inventory,
                            null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Product Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    product.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "PKR ${product.price}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (product.actualPrice != product.price) {
                        Text(
                            "PKR ${product.actualPrice}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    }
                }
            }
        }
    }
}
