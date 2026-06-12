package com.example.myapplication.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.AppRoutes
import com.example.myapplication.AppUtil
import com.example.myapplication.pages.ReportTechnicalIssueDialog
import com.example.myapplication.model.AuthViewModel
import com.example.myapplication.globalNavigation
import com.example.myapplication.globalNavigation.navController
import com.example.myapplication.model.UserModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

@Composable
fun ProfilePage(modifier: Modifier = Modifier, authViewModel: AuthViewModel = viewModel()) {
    val context = LocalContext.current
    val usermodel = remember { mutableStateOf(UserModel()) }
    var addressInput by remember { mutableStateOf(usermodel.value.address) }
    var showTechnicalIssueDialog by remember { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                 authViewModel.uploadProfileImage(context,uri)
            }
        }
    var menuExpanded by remember { mutableStateOf(false) }




    DisposableEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            onDispose { }
        } else {
            val listener = Firebase.firestore.collection("users")
                .document(uid)
                .addSnapshotListener { value, _ ->

                    val result = value?.toObject(UserModel::class.java)
                    if (result != null) {
                        usermodel.value = result
                        addressInput = usermodel.value.address
                    }

                }
            onDispose {
                listener.remove()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp),
    ) {
            Text(
                text = "My Profile",
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier.wrapContentSize(), contentAlignment = Alignment.BottomEnd) {
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Add Image") },
                        onClick = {
                            menuExpanded = false
                            launcher.launch("image/*")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove Image") },
                        onClick = {
                            menuExpanded = false
                            authViewModel.removeProfileImage()
                        }
                    )
                }
                
                // Profile Image
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (usermodel.value.profileImage.isNotEmpty()) {
                        AsyncImage(
                            model = usermodel.value.profileImage,
                            contentDescription = "Profile Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Show first letter of name as placeholder
                        Text(
                            text = usermodel.value.name.firstOrNull()?.uppercase() ?: "U",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile Image",
                    modifier = Modifier
                        .size(30.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { menuExpanded = true }
                        .padding(5.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Address :",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = addressInput,
                onValueChange = {
                    addressInput = it
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    //update to firebase
                    if (addressInput.isNotEmpty()) {
                        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                        if (currentUid == null) {
                            AppUtil.showToast(context, "Please login again")
                            return@KeyboardActions
                        }
                        Firebase.firestore.collection("users")
                            .document(currentUid)
                            .update("address", addressInput).addOnCompleteListener {
                                if (it.isSuccessful) {
                                    AppUtil.showToast(context, "Address Updated Successfully")
                                }
                            }
                    } else {
                        AppUtil.showToast(context, "Address can't be Empty")
                    }
                })
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Email :", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(text= usermodel.value.email)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Name :", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(text = usermodel.value.name)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Role :", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(text = usermodel.value.role)
            Spacer(modifier = Modifier.height(12.dp))
            // ProfilePage.kt mein navController parameter hona chahiye
// Phir jahan "Orders", "Settings" jaise options hain wahan add karo:

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        navController.navigate(AppRoutes.WALLET)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Wallet",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "My Wallet",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                }
            }
            
            // Payment History Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        navController.navigate("payment_history")
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "Payment History",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Payment History",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "View all payment transactions",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                }
            }
            
            // Messages Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        navController.navigate("chats_list")
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "💬",
                            fontSize = 24.sp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Messages",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Chat with buyers/sellers",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                }
            }
            
            // Report Technical Issue Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { showTechnicalIssueDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = null,
                            tint = Color(0xFFE91E63)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Report Technical Issue",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "App crash, bugs, or performance issues",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }
        if (usermodel.value.role == "seller") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        navController.navigate("seller_verification")
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✅",
                            fontSize = 24.sp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Seller Verification",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Verify Yourself as a Seller",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                }
            }
        }
        // Profile page mein "My Reports" card add karo
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable {
                    navController.navigate("my_reports")
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = null,
                        tint = Color(0xFFE91E63)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("My Reports & Complaints", fontWeight = FontWeight.Bold)
                        Text(
                            "View your submitted reports",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
            }
        }


        Spacer(modifier = Modifier.height(20.dp))
            
            // Sign Out Button
            TextButton(
                onClick = {
                    try {
                        FirebaseAuth.getInstance().signOut()
                        val navController = globalNavigation.navController
                        // Clear entire back stack and navigate to auth
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true }
                        }
                    } catch (e: Exception) {
                        // Handle error gracefully
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Sign Out", fontSize = 18.sp, color = MaterialTheme.colorScheme.error)
            }
            
            // Bottom spacing for safe scrolling above navigation bar
            Spacer(modifier = Modifier.height(20.dp))
        }
    
    // Technical Issue Dialog
    if (showTechnicalIssueDialog) {
        ReportTechnicalIssueDialog(
            onDismiss = { showTechnicalIssueDialog = false }
        )
    }
}