package com.example.myapplication.pages

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.globalNavigation
import com.example.myapplication.model.CloudinaryRepository
import com.example.myapplication.model.ProductModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import java.util.Collections.emptyList

@Composable
fun ProductListingPage(modifier: Modifier = Modifier) {
    val productList = remember{ mutableStateListOf<ProductModel>() }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<ProductModel?>(null) }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        Firebase.firestore.collection("data").document("stock")
            .collection("products")
            .whereEqualTo("sellerId", uid)
            .get()
            .addOnCompleteListener {
                productList.clear()
                if (it.isSuccessful){
                   val products = it.result.documents.mapNotNull { doc->
                       doc.toObject(ProductModel::class.java)
                           ?.copy(id = doc.id)
                   }
                   // Filter: Only show products where isCoStoreProduct is false OR coStoreId is empty
                   val personalProducts = products.filter { product ->
                       !product.isCoStoreProduct && product.coStoreId.isEmpty()
                   }
                   productList.addAll(personalProducts)
                }
            }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Your Products",
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(productList) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AsyncImage(
                                model = product.getImagesList().firstOrNull(),
                                contentDescription = product.title,
                                modifier = Modifier
                                    .height(100.dp)
                                    .width(100.dp)
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = product.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = "Price : ${product.price}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = "Actual Price : ${product.actualPrice}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = "Category : ${product.category}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = "Description : ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = product.description,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            
                            // ✅ Approval Status Display
                            when (product.status) {
                                "approved" -> {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFE8F5E9)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "✅",
                                                fontSize = 20.sp
                                            )
                                            Column {
                                                Text(
                                                    text = "Approved by Admin",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32)
                                                )
                                                if (!product.reviewedBy.isNullOrEmpty()) {
                                                    Text(
                                                        text = "Reviewed by: ${product.reviewedBy}",
                                                        fontSize = 12.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                "rejected" -> {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFFFEBEE)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "❌",
                                                fontSize = 20.sp
                                            )
                                            Column {
                                                Text(
                                                    text = "Rejected by Admin",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFC62828)
                                                )
                                                if (!product.rejectionReason.isNullOrEmpty()) {
                                                    Text(
                                                        text = "Reason: ${product.rejectionReason}",
                                                        fontSize = 12.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                                if (!product.reviewedBy.isNullOrEmpty()) {
                                                    Text(
                                                        text = "Reviewed by: ${product.reviewedBy}",
                                                        fontSize = 12.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                "pending" -> {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFFFF3E0)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "⏳",
                                                fontSize = 20.sp
                                            )
                                            Text(
                                                text = "Pending Admin Approval",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFE65100)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = "Details : ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            // otherDetails loop - sirf details
                            product.getOtherDetailsMap().forEach { (key, value) ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "$key",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(text = " $value", fontSize = 12.sp)
                                }
                            }

                            // ✅ Edit & Delete - forEach ke BAHAR
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = {
                                    selectedProduct = product
                                    showEditDialog = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = Color(0xFF1976D2)
                                    )
                                }
                                IconButton(onClick = {
                                    deleteProduct(product.id)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))
            Button(onClick = {
                globalNavigation.navigateSafely("addProductPage")
            }) {
                Text(text = "Add New Product")
            }
        }

        // ✅ Edit Dialog
        if (showEditDialog && selectedProduct != null) {
            EditProductDialog(
                product = selectedProduct!!,
                onDismiss = { showEditDialog = false },
                onSave = { updatedProduct ->
                    updateProduct(updatedProduct)
                    val index = productList.indexOfFirst { it.id == updatedProduct.id }
                    if (index != -1) productList[index] = updatedProduct
                    showEditDialog = false
                }
            )
        }
    }


}

@Composable
fun EditProductDialog(product: ProductModel,onDismiss : ()-> Unit,onSave : (ProductModel) -> Unit) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(product.title) }
    var price by remember { mutableStateOf(product.price) }
    var actualPrice by remember { mutableStateOf(product.actualPrice) }
    var category by remember { mutableStateOf(product.category) }
    var description by remember { mutableStateOf(product.description) }

    val existingImageUrls = remember { mutableStateListOf<String>().apply { addAll(product.getImagesList()) } }
    val newImageUris = remember { mutableStateListOf<Uri>() }
    var isUploading by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        newImageUris.addAll(uris)
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        title = { Text("Edit Product", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {

                // ── TEXT FIELDS ──
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = actualPrice,
                    onValueChange = { actualPrice = it },
                    label = { Text("Actual Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── IMAGES SECTION ──
                Text(
                    "Images",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Existing images
                if (existingImageUrls.isNotEmpty()) {
                    Text("Current Images:", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(existingImageUrls.size) { index ->
                            Box {
                                AsyncImage(
                                    model = existingImageUrls[index],
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                // Remove button
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.TopEnd)
                                        .background(Color.Red, CircleShape)
                                        .clickable {
                                            existingImageUrls.removeAt(index)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Newly selected images preview
                if (newImageUris.isNotEmpty()) {
                    Text("New Images:", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(newImageUris.size) { index ->
                            Box {
                                AsyncImage(
                                    model = newImageUris[index],
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.TopEnd)
                                        .background(Color.Red, CircleShape)
                                        .clickable {
                                            newImageUris.removeAt(index)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Add images button
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Images")
                }

                // Uploading indicator
                if (isUploading) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uploading to Cloudinary...", fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isUploading = true
                    uploadNewImages(
                        uris = newImageUris,
                        context = context,
                        onComplete = { newUrls ->
                            val allImages = existingImageUrls + newUrls
                            val updatedProduct = product.apply {
                                this.title = title
                                this.price = price
                                this.actualPrice = actualPrice
                                this.category = category
                                this.description = description
                                this.images = allImages
                            }
                            onSave(updatedProduct)
                            isUploading = false
                        }
                    )
                },
                enabled = !isUploading
            ) {
                Text(if (isUploading) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUploading
            ) {
                Text("Cancel")
            }
        }
    )
}

fun deleteProduct(productId : String){
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    Firebase.firestore.collection("data")
        .document("stock")
        .collection("products")
        .document(productId)
        .delete()
}
fun updateProduct(product: ProductModel){
    Firebase.firestore
        .collection("data")
        .document("stock")
        .collection("products")
        .document(product.id)
        .set(product)
}
fun uploadNewImages(
    uris: List<Uri>,
    context: Context,
    onComplete: (List<String>) -> Unit
) {
    if (uris.isEmpty()) {
        onComplete(emptyList())
        return
    }

    val uploadedUrls = mutableListOf<String>()
    var uploadCount = 0

    uris.forEach { uri ->
        CloudinaryRepository.uploadImageToCloudinary(
            context = context,
            uri = uri,
            preset = "product_upload", // apna preset name likho
            onSuccess = { imageUrl ->
                uploadedUrls.add(imageUrl)
                uploadCount++
                if (uploadCount == uris.size) {
                    onComplete(uploadedUrls)
                }
            },
            onError = { errorMsg ->
                uploadCount++
                if (uploadCount == uris.size) {
                    onComplete(uploadedUrls)
                }
            }
        )
    }
}