package com.example.myapplication.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.myapplication.AppUtil
import com.example.myapplication.model.ChatViewModel
import com.example.myapplication.model.ProductModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import com.tbuonomo.viewpagerdotsindicator.compose.model.DotGraphic
import com.tbuonomo.viewpagerdotsindicator.compose.type.ShiftIndicatorType

@Composable
fun ProductDetailPge(
    modifier: Modifier,
    productId: String,
    navController: NavHostController? = null
) {
    var product by remember { mutableStateOf(ProductModel()) }
    var context = LocalContext.current
    var showReportDialog by remember { mutableStateOf(false) }
    val chatViewModel: ChatViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var isFav = remember {
        mutableStateOf(AppUtil.checkFavourite(context, productId))
    }
    LaunchedEffect(Unit) {
        Firebase.firestore.collection("data").document("stock")
            .collection("products").document(productId).get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    var result = it.result.toObject(ProductModel::class.java)
                    // ✅ BUYER SIDE: Only show approved products
                    if (result != null && result.status == "approved") {
                        product = result
                    } else if (result != null && result.status != "approved") {
                        // Product not approved - don't show to buyer
                        android.util.Log.d("ProductDetailPage", "Product not approved: ${result.status}")
                    }
                }
            }
    }

    Column(
        modifier = modifier.fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = product.title,
            fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(8.dp)
        )

        // Store and Seller Info
        if (product.isCoStoreProduct && product.coStoreName.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Store,
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "From ${product.coStoreName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (product.sellerName.isNotEmpty()) {
                            Text(
                                "Added by ${product.sellerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        } else if (product.sellerName.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Sold by ${product.sellerName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = modifier.height(8.dp))
        Column() {
            val pagerState = rememberPagerState(0) {
                product.getImagesList().size
            }
            HorizontalPager(
                state = pagerState,
                pageSpacing = 24.dp,
                modifier = Modifier.height(220.dp)
            ) {

                AsyncImage(
                    model = product.getImagesList().getOrNull(it),
                    contentDescription = "banner",
                    modifier = Modifier.height(220.dp).fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            com.tbuonomo.viewpagerdotsindicator.compose.DotsIndicator(
                dotCount = product.getImagesList().size,
                pagerState = pagerState,
                type = ShiftIndicatorType(
                    DotGraphic(
                        color = MaterialTheme.colorScheme.primary,
                        size = 6.dp
                    )
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "PKR " + product.actualPrice,
                    fontSize = 16.sp,
                    style = TextStyle(textDecoration = TextDecoration.LineThrough)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PKR " + product.price,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    AppUtil.addOrRemoveFromFavourite(context,productId)
                   isFav.value = AppUtil.checkFavourite(context,productId)
                }) {
                    Icon(
                        imageVector = if(isFav.value) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Add To Favourite"
                        
                    )

                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(onClick = {
                AppUtil.addItemToCart(productId,context)
            }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(text = "Add To Cart", fontSize = 16.sp)
            }
            
            // Chat with Seller Button (only show if not own product)
            if (product.sellerId.isNotEmpty() && product.sellerId != currentUserId) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val chatId = chatViewModel.getOrCreateChat(
                                sellerId = product.sellerId,
                                productId = product.id,
                                productName = product.title,
                                productImage = product.getImagesList().firstOrNull() ?: ""
                            )
                            if (chatId.isNotEmpty()) {
                                navController?.navigate("chat_screen/$chatId/${product.sellerId}")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(text = "💬 Chat with Seller", fontSize = 16.sp)
                }
            }
            OutlinedButton(
    onClick = { showReportDialog = true },
    modifier = Modifier.fillMaxWidth()
) {
    Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text("Report Product")
}

if (showReportDialog) {
    ReportProductDialog(
        productId = product.id,
        productName = product.title,
        onDismiss = { showReportDialog = false }
    )
}
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Product Description : ",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = product.description, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Other Product Details : ",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            product.getOtherDetailsMap().forEach { (key, value) ->
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(text = "$key", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = " $value", fontSize = 16.sp)
                }
            }

        }
    }
}
