package com.example.myapplication.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplication.model.NegotiationRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

@Composable
fun SellerNegotiationRequestScreen(modifier: Modifier = Modifier, navController: NavHostController) {
    val sellerId = FirebaseAuth.getInstance().currentUser?.uid
    val requests = remember { mutableStateOf<List<NegotiationRequest>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        if (sellerId == null) return@LaunchedEffect
        
        // Fetch all pending negotiation requests
        Firebase.firestore.collection("negotiation_requests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, _ ->
                val allRequests = snapshot?.toObjects(NegotiationRequest::class.java) ?: emptyList()
                
                // Filter requests for this seller
                val filteredRequests = mutableListOf<NegotiationRequest>()
                
                allRequests.forEach { request ->
                    // Check if product belongs to this seller
                    Firebase.firestore.collection("data").document("stock")
                        .collection("products")
                        .document(request.productId)
                        .get()
                        .addOnSuccessListener { productDoc ->
                            val product = productDoc.toObject(com.example.myapplication.model.ProductModel::class.java)
                            
                            if (product != null) {
                                val shouldShow = if (product.isCoStoreProduct && product.coStoreId.isNotEmpty()) {
                                    // Store product - check if seller is owner or co-seller
                                    Firebase.firestore.collection("coSellerStores")
                                        .document(product.coStoreId)
                                        .get()
                                        .addOnSuccessListener { storeDoc ->
                                            val ownerSellerId = storeDoc.getString("ownerSellerId")
                                            val coSellerId = storeDoc.getString("coSellerId")
                                            
                                            if (sellerId == ownerSellerId || sellerId == coSellerId) {
                                                if (!filteredRequests.any { it.requestId == request.requestId }) {
                                                    filteredRequests.add(request)
                                                    requests.value = filteredRequests.toList()
                                                }
                                            }
                                        }
                                    false // Will be handled in callback
                                } else {
                                    // Personal product - check if seller matches
                                    product.sellerId == sellerId
                                }
                                
                                if (shouldShow) {
                                    if (!filteredRequests.any { it.requestId == request.requestId }) {
                                        filteredRequests.add(request)
                                        requests.value = filteredRequests.toList()
                                    }
                                }
                            }
                        }
                }
            }
    }
    
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Your Negotiation Requests :", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        if (requests.value.isEmpty()) {
            Text(
                "No pending requests",
                fontSize = 16.sp,
                color = androidx.compose.ui.graphics.Color.Gray,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn {
                items(requests.value) { req ->
                    NegotiationRequestCard(req)
                }
            }
        }
    }
}

@Composable
fun NegotiationRequestCard(req: NegotiationRequest) {
    var productTitle by remember { mutableStateOf("Loading...") }
    var isStoreProduct by remember { mutableStateOf(false) }
    var storeName by remember { mutableStateOf("") }
    
    LaunchedEffect(req.productId) {
        Firebase.firestore.collection("data").document("stock")
            .collection("products")
            .document(req.productId)
            .get()
            .addOnSuccessListener { doc ->
                val product = doc.toObject(com.example.myapplication.model.ProductModel::class.java)
                productTitle = product?.title ?: "Unknown Product"
                isStoreProduct = product?.isCoStoreProduct ?: false
                storeName = product?.coStoreName ?: ""
            }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Product: $productTitle",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            if (isStoreProduct && storeName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Store: $storeName",
                    fontSize = 14.sp,
                    color = androidx.compose.ui.graphics.Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Buyer Offer: Rs ${req.offeredPrice}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        Firebase.firestore.collection("negotiation_requests")
                            .document(req.requestId)
                            .update("status", "accepted")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Accept")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        Firebase.firestore.collection("negotiation_requests")
                            .document(req.requestId)
                            .update("status", "rejected")
                    },
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Red
                    )
                ) {
                    Text("Reject")
                }
            }
        }
    }
}