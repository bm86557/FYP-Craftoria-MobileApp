package com.example.myapplication.pages

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myapplication.model.CoSellerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCoSellerStorePage(
    context: Context,
    navController: NavController,
    viewModel: CoSellerViewModel = viewModel()
) {
    var storeName by remember { mutableStateOf("") }
    var storeDescription by remember { mutableStateOf("") }
    var logoUri by remember { mutableStateOf<Uri?>(null) }
    var bannerUri by remember { mutableStateOf<Uri?>(null) }
    var inviteEmail by remember { mutableStateOf("") }

    val createState by viewModel.createStoreState
    val inviteState by viewModel.inviteState
    val createdStoreId by viewModel.createdStoreId

    val logoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> logoUri = uri }

    val bannerPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> bannerUri = uri }

    LaunchedEffect(createState) {
        if (createState == "success" && createdStoreId.isEmpty()) {
            // Store created, navigate back
            try {
                navController.popBackStack()
            } catch (e: Exception) {
                // Handle navigation error gracefully
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Create New Store", fontWeight = FontWeight.Bold)
                        Text("Set up your collaborative store",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Store Information
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Store Information",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = storeName,
                            onValueChange = { storeName = it },
                            label = { Text("Store Name *") },
                            placeholder = { Text("Enter store name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = storeDescription,
                            onValueChange = { storeDescription = it },
                            label = { Text("Store Description") },
                            placeholder = { Text("Describe your collaborative store") },
                            modifier = Modifier.fillMaxWidth().height(120.dp)
                        )
                    }
                }
            }

            // Store Media
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Store Media",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))

                        Text("Store Logo", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        SimpleImageBox(
                            uri = logoUri,
                            label = "Tap to upload logo",
                            height = 120.dp,
                            onClick = { logoPicker.launch("image/*") }
                        )

                        Spacer(Modifier.height(12.dp))

                        Text("Store Banner", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        SimpleImageBox(
                            uri = bannerUri,
                            label = "Tap to upload banner",
                            height = 150.dp,
                            onClick = { bannerPicker.launch("image/*") }
                        )
                    }
                }
            }

            // Invite Seller (Only show after store is created)
            if (createdStoreId.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Invite Co-Seller",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "✅ Store created successfully! You can now invite a co-seller.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50)
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = inviteEmail,
                                    onValueChange = { inviteEmail = it },
                                    placeholder = { Text("Enter seller's email") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email
                                    )
                                )
                                Button(
                                    onClick = {
                                        if (inviteEmail.isNotBlank()) {
                                            viewModel.inviteCoSeller(inviteEmail.trim(), createdStoreId)
                                        }
                                    },
                                    enabled = inviteEmail.isNotBlank() && inviteState != "loading"
                                ) {
                                    if (inviteState == "loading") {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White
                                        )
                                    } else {
                                        Text("Invite")
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Only registered sellers can be invited",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            when (inviteState) {
                                "success" -> {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "✅ Co-Seller invited successfully!",
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                                "not_seller" -> {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "❌ This email is not registered as a seller",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                "error" -> {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "❌ Something went wrong. Please try again.",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Done button after inviting
                item {
                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done")
                    }
                }
            }

            // Buttons (Only show if store not created yet)
            if (createdStoreId.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (storeName.isNotBlank() && createState != "loading") {
                                    viewModel.createCoStore(
                                        context = context,
                                        storeName = storeName.trim(),
                                        storeDescription = storeDescription.trim(),
                                        logoUri = logoUri,
                                        bannerUri = bannerUri
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = storeName.isNotBlank() && createState != "loading"
                        ) {
                            if (createState == "loading") {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("Create Store")
                            }
                        }
                    }
                }
            }
        }
    }
}



// Simple Image Upload Box
@Composable
fun SimpleImageBox(
    uri: Uri?,
    label: String,
    height: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddPhotoAlternate, null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(6.dp))
                Text(label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}