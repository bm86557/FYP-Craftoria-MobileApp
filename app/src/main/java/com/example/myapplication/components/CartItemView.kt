package com.example.myapplication.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.AppUtil
import com.example.myapplication.model.ProductModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@Composable
fun CartItemView(
    modifier: Modifier = Modifier,
    productId: String,
    qty: Long
) {
    var product by remember { mutableStateOf(ProductModel()) }
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        Firebase.firestore.collection("data").document("stock")
            .collection("products").document(productId).get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val result = it.result.toObject(ProductModel::class.java)
                    // ✅ BUYER SIDE: Only show approved products in cart
                    if (result != null && result.status == "approved") {
                        product = result
                    } else if (result != null && result.status != "approved") {
                        // Product not approved - remove from cart or show warning
                        android.util.Log.d("CartItemView", "Product not approved: ${result.status}")
                    }
                }
            }
    }

    Card(
        modifier = modifier.padding(12.dp).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = product.getImagesList().firstOrNull(),
                contentDescription = product.title,
                modifier = Modifier.height(100.dp).width(100.dp)
            )
            Column(modifier = Modifier.padding(8.dp).weight(1f)) {
                Text(
                    text = product.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(8.dp)
                )
                
                // Store/Seller Info
                if (product.isCoStoreProduct && product.coStoreName.isNotEmpty()) {
                    Text(
                        text = "${product.coStoreName} • ${product.sellerName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                } else if (product.sellerName.isNotEmpty()) {
                    Text(
                        text = "by ${product.sellerName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                Text(text = "PKR " + product.price, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = {
                        AppUtil.removeItemToCart(productId, context, if (qty < 1) true else false)
                    }) {
                        Text(text = "-", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Text(text = "$qty", fontSize = 14.sp)
                    IconButton(onClick = {
                        AppUtil.addItemToCart(productId, context)
                    }) {
                        Text(text = "+", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }
            }

            IconButton(onClick = {
                AppUtil.removeItemToCart(product.id, context, removeAll = true)
            }) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "")
            }
        }
    }
}
