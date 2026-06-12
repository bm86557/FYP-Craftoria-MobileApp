package com.example.myapplication.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myapplication.model.CoSellerViewModel
import com.example.myapplication.model.CoSellerStoreModel
import com.example.myapplication.model.SellerInfo
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreSettingsPage(
    storeId: String,
    navController: NavController,
    viewModel: CoSellerViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    var store by remember { mutableStateOf<CoSellerStoreModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    
    // Edit mode states
    var isEditMode by remember { mutableStateOf(false) }
    var editStoreName by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var newLogoUri by remember { mutableStateOf<Uri?>(null) }
    var newBannerUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    
    val logoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> newLogoUri = uri }
    
    val bannerPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> newBannerUri = uri }

    LaunchedEffect(storeId) {
        viewModel.fetchStoreById(storeId) { fetchedStore ->
            store = fetchedStore
            editStoreName = fetchedStore?.storeName ?: ""
            editDescription = fetchedStore?.storeDescription ?: ""
            isLoading = false
        }
    }
    
    val isOwner = store?.ownerSellerId == currentUserId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Store Settings") },
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
        } else if (store == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Store not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("General") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Members") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Browse Sellers") }
                    )
                }
                
                // Tab Content
                when (selectedTab) {
                    0 -> GeneralSettingsTab(
                        store = store!!,
                        isEditMode = isEditMode,
                        editStoreName = editStoreName,
                        editDescription = editDescription,
                        newLogoUri = newLogoUri,
                        newBannerUri = newBannerUri,
                        isSaving = isSaving,
                        onEditModeChange = { isEditMode = it },
                        onStoreNameChange = { editStoreName = it },
                        onDescriptionChange = { editDescription = it },
                        onLogoClick = { logoPicker.launch("image/*") },
                        onBannerClick = { bannerPicker.launch("image/*") },
                        onCancel = {
                            isEditMode = false
                            editStoreName = store!!.storeName
                            editDescription = store!!.storeDescription
                            newLogoUri = null
                            newBannerUri = null
                        },
                        onSave = {
                            isSaving = true
                            viewModel.updateStore(
                                context = context,
                                storeId = storeId,
                                storeName = editStoreName.trim(),
                                storeDescription = editDescription.trim(),
                                logoUri = newLogoUri,
                                bannerUri = newBannerUri,
                                currentLogoUrl = store!!.storeLogo,
                                currentBannerUrl = store!!.storeBanner
                            ) { success ->
                                isSaving = false
                                if (success) {
                                    isEditMode = false
                                    newLogoUri = null
                                    newBannerUri = null
                                    // Refresh store data
                                    viewModel.fetchStoreById(storeId) { fetchedStore ->
                                        store = fetchedStore
                                        editStoreName = fetchedStore?.storeName ?: ""
                                        editDescription = fetchedStore?.storeDescription ?: ""
                                    }
                                }
                            }
                        },
                        isOwner = isOwner,  // ✅ ADD
                        storeId = storeId,  // ✅ ADD
                        viewModel = viewModel,  // ✅ ADD
                        navController = navController  // ✅ ADD
                    )
                    1 -> MembersTab(
                        store = store!!,
                        currentUserId = currentUserId,
                        isOwner = isOwner
                    )
                    2 -> BrowseSellersTab(
                        storeId = storeId,
                        store = store!!,
                        isOwner = isOwner,
                        viewModel = viewModel,
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun GeneralSettingsTab(
    store: CoSellerStoreModel,
    isEditMode: Boolean,
    editStoreName: String,
    editDescription: String,
    newLogoUri: Uri?,
    newBannerUri: Uri?,
    isSaving: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    onStoreNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onLogoClick: () -> Unit,
    onBannerClick: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    isOwner: Boolean = false,  // ✅ ADD
    storeId: String = "",  // ✅ ADD
    viewModel: CoSellerViewModel = viewModel(),  // ✅ ADD
    navController: NavController? = null  // ✅ ADD
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Edit Mode Toggle
        if (!isEditMode) {
            item {
                Button(
                    onClick = { onEditModeChange(true) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Store Information")
                }
            }
        }
        
        // Store Banner
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Store Banner",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = isEditMode) { onBannerClick() }
                    ) {
                        AsyncImage(
                            model = newBannerUri ?: store.storeBanner.ifEmpty { null },
                            contentDescription = "Store Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (store.storeBanner.isEmpty() && newBannerUri == null) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    if (isEditMode) {
                                        Text(
                                            "Tap to add banner",
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                        if (isEditMode && (store.storeBanner.isNotEmpty() || newBannerUri != null)) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "Tap to change",
                                    modifier = Modifier.padding(8.dp),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Store Logo
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Store Logo",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    Surface(
                        modifier = Modifier
                            .size(100.dp)
                            .clickable(enabled = isEditMode) { onLogoClick() },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (newLogoUri != null || store.storeLogo.isNotEmpty()) {
                            AsyncImage(
                                model = newLogoUri ?: store.storeLogo,
                                contentDescription = "Store Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Store,
                                    null,
                                    modifier = Modifier.size(50.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    
                    if (isEditMode) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tap logo to change",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
        
        // Store Name
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Store Name",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    if (isEditMode) {
                        OutlinedTextField(
                            value = editStoreName,
                            onValueChange = onStoreNameChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter store name") },
                            singleLine = true
                        )
                    } else {
                        Text(
                            store.storeName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
        
        // Description
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Description",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    if (isEditMode) {
                        OutlinedTextField(
                            value = editDescription,
                            onValueChange = onDescriptionChange,
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            placeholder = { Text("Describe your store") },
                            maxLines = 5
                        )
                    } else {
                        Text(
                            store.storeDescription.ifEmpty { "No description" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (store.storeDescription.isEmpty())
                                MaterialTheme.colorScheme.outline
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        
        // Save/Cancel buttons
        if (isEditMode) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving && editStoreName.isNotBlank()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
        
        // ✅ DELETE STORE SECTION (Only for Owner)
        if (isOwner && !isEditMode) {
            item {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
            }
            
            item {
                var showDeleteDialog by remember { mutableStateOf(false) }
                var isDeleting by remember { mutableStateOf(false) }
                var deleteError by remember { mutableStateOf<String?>(null) }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFF44336),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Danger Zone",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828)
                            )
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Delete this store permanently",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "⚠️ This action cannot be undone. All store products and data will be permanently deleted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF999999)
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Button(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isDeleting
                        ) {
                            if (isDeleting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Deleting...")
                            } else {
                                Icon(Icons.Default.Delete, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Delete Store")
                            }
                        }
                        
                        deleteError?.let { error ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                error,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF44336)
                            )
                        }
                    }
                }
                
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
                        icon = {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFF44336),
                                modifier = Modifier.size(48.dp)
                            )
                        },
                        title = { Text("Delete Store?") },
                        text = {
                            Column {
                                Text("Are you sure you want to delete \"${store.storeName}\"?")
                                Spacer(Modifier.height(12.dp))
                                Text("This will permanently delete:")
                                Text("• The store")
                                Text("• All store products")
                                Text("• All pending invites")
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "⚠️ This action cannot be undone!",
                                    color = Color(0xFFF44336),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    isDeleting = true
                                    viewModel.deleteStore(
                                        storeId = storeId,
                                        onSuccess = {
                                            navController?.popBackStack()
                                        },
                                        onError = { error ->
                                            isDeleting = false
                                            deleteError = error
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                                enabled = !isDeleting
                            ) {
                                Text("Delete Permanently")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { if (!isDeleting) showDeleteDialog = false },
                                enabled = !isDeleting
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MembersTab(
    store: CoSellerStoreModel,
    currentUserId: String,
    isOwner: Boolean
) {
    val viewModel: CoSellerViewModel = viewModel()
    var removingMemberId by remember { mutableStateOf<String?>(null) }
    
    // ✅ FIX: Force refresh store data to get latest coSellerIds
    var refreshedStore by remember { mutableStateOf(store) }
    
    LaunchedEffect(store.storeId) {
        android.util.Log.d("MembersTab", "Fetching store: ${store.storeId}")
        viewModel.fetchStoreById(store.storeId) { fetchedStore ->
            if (fetchedStore != null) {
                android.util.Log.d("MembersTab", "Store fetched - Partners: ${fetchedStore.coSellerIds.size}")
                android.util.Log.d("MembersTab", "Partner IDs: ${fetchedStore.coSellerIds}")
                android.util.Log.d("MembersTab", "Partner Names: ${fetchedStore.coSellerNames}")
                refreshedStore = fetchedStore
            } else {
                android.util.Log.e("MembersTab", "Failed to fetch store")
            }
        }
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Your Role",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    Surface(
                        color = if (isOwner) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isOwner) Icons.Default.Star else Icons.Default.Group,
                                null,
                                tint = if (isOwner) 
                                    MaterialTheme.colorScheme.onPrimaryContainer 
                                else 
                                    MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isOwner) "Owner" else "Partner",
                                fontWeight = FontWeight.Bold,
                                color = if (isOwner) 
                                    MaterialTheme.colorScheme.onPrimaryContainer 
                                else 
                                    MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
        
        // ✅ FIX: Use refreshedStore instead of store
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Total Members",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "1 Owner + ${refreshedStore.coSellerIds.size} Partners",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            "${1 + refreshedStore.coSellerIds.size}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
        
        // ✅ FIX: Owner card - use refreshedStore
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Owner",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Star,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                refreshedStore.ownerSellerName.ifEmpty { "Store Owner" },
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                refreshedStore.ownerSellerEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
        
        // ✅ FIX: All Co-Sellers (use refreshedStore)
        if (refreshedStore.coSellerIds.isNotEmpty()) {
            item {
                Text(
                    "Partners (${refreshedStore.coSellerIds.size})",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            items(refreshedStore.coSellerIds.size) { index ->
                CoSellerMemberCard(
                    sellerId = refreshedStore.coSellerIds[index],
                    name = refreshedStore.coSellerNames.getOrElse(index) { "Partner ${index + 1}" },
                    email = refreshedStore.coSellerEmails.getOrElse(index) { "" },
                    isOwner = isOwner,
                    storeId = refreshedStore.storeId,
                    isRemoving = removingMemberId == refreshedStore.coSellerIds[index],
                    onRemove = {
                        removingMemberId = refreshedStore.coSellerIds[index]
                        viewModel.removeCoSeller(refreshedStore.storeId, refreshedStore.coSellerIds[index]) { success ->
                            removingMemberId = null
                            if (success) {
                                // Refresh store data
                                viewModel.fetchStoreById(refreshedStore.storeId) { fetchedStore ->
                                    if (fetchedStore != null) {
                                        refreshedStore = fetchedStore
                                    }
                                }
                            }
                        }
                    }
                )
            }
        } else {
            // No co-sellers yet
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Group,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No partners yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            if (isOwner) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Go to Browse Sellers tab to invite partners",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ✅ NEW: Co-Seller Member Card with Remove option
@Composable
fun CoSellerMemberCard(
    sellerId: String,
    name: String,
    email: String,
    isOwner: Boolean,
    storeId: String,
    isRemoving: Boolean = false,
    onRemove: () -> Unit
) {
    var showRemoveDialog by remember { mutableStateOf(false) }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            // ✅ Remove button (only for owner)
            if (isOwner) {
                if (isRemoving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = { showRemoveDialog = true }) {
                        Icon(
                            Icons.Default.RemoveCircle,
                            "Remove member",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
    
    // Remove confirmation dialog
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Partner?") },
            text = { Text("Are you sure you want to remove $name from this store? They will lose access to all store products and settings.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        onRemove()
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BrowseSellersTab(
    storeId: String,
    store: CoSellerStoreModel,
    isOwner: Boolean,
    viewModel: CoSellerViewModel,
    navController: NavController
) {
    var allSellers by remember { mutableStateOf<List<SellerInfo>>(emptyList()) }
    var isLoadingSellers by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    
    LaunchedEffect(Unit) {
        // Fetch all sellers from users collection
        db.collection("users")
            .whereEqualTo("role", "seller")
            .get()
            .addOnSuccessListener { docs ->
                allSellers = docs.documents.mapNotNull { doc ->
                    SellerInfo(
                        sellerId = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        profileImage = doc.getString("profileImage") ?: ""
                    )
                }
                isLoadingSellers = false
            }
            .addOnFailureListener {
                isLoadingSellers = false
            }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-tabs
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Store Members") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("All Sellers") }
            )
        }
        
        when (selectedTab) {
            0 -> StoreMembersSection(
                store = store,
                storeId = storeId,
                navController = navController,
                viewModel = viewModel,
                currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                onInviteClick = { selectedTab = 1 }  // ✅ FIX: Switch to All Sellers tab
            )
            1 -> AllSellersSection(
                allSellers = allSellers,
                isLoading = isLoadingSellers,
                storeId = storeId,
                store = store,
                navController = navController
            )
        }
    }
}

@Composable
fun StoreMembersSection(
    store: CoSellerStoreModel,
    storeId: String,
    navController: NavController,
    viewModel: CoSellerViewModel,
    currentUserId: String,
    onInviteClick: () -> Unit = {}  // ✅ FIX: Add callback for invite button
) {
    val isOwner = store.ownerSellerId == currentUserId
    
    // ✅ FIX: Force refresh store data
    var refreshedStore by remember { mutableStateOf(store) }
    
    LaunchedEffect(storeId) {
        android.util.Log.d("StoreMembersSection", "Fetching store: $storeId")
        viewModel.fetchStoreById(storeId) { fetchedStore ->
            if (fetchedStore != null) {
                android.util.Log.d("StoreMembersSection", "Store fetched - Partners: ${fetchedStore.coSellerIds.size}")
                refreshedStore = fetchedStore
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Current Store Members",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${1 + refreshedStore.coSellerIds.size} members",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // Owner Card
        item {
            MemberCard(
                sellerId = refreshedStore.ownerSellerId,
                name = refreshedStore.ownerSellerName.ifEmpty { "Store Owner" },
                email = refreshedStore.ownerSellerEmail,
                role = "Owner",
                isOwner = true,
                canRemove = false,
                storeId = storeId,
                navController = navController,
                viewModel = viewModel,
                onRemove = {}
            )
        }

        // ✅ FIX: All Co-Sellers (use refreshedStore)
        items(refreshedStore.coSellerIds.size) { index ->
            MemberCard(
                sellerId = refreshedStore.coSellerIds[index],
                name = refreshedStore.coSellerNames.getOrNull(index) ?: "Co-Seller",
                email = refreshedStore.coSellerEmails.getOrNull(index) ?: "",
                role = "Partner",
                isOwner = false,
                canRemove = isOwner,
                storeId = storeId,
                navController = navController,
                viewModel = viewModel,
                onRemove = {
                    viewModel.removeCoSeller(
                        storeId = storeId,
                        coSellerId = refreshedStore.coSellerIds[index],
                        onResult = { success ->
                            if (success) {
                                // Refresh store data
                                viewModel.fetchStoreById(storeId) { fetchedStore ->
                                    if (fetchedStore != null) {
                                        refreshedStore = fetchedStore
                                    }
                                }
                            }
                        }
                    )
                }
            )
        }

        // Empty State with Invite Button
        if (refreshedStore.coSellerIds.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No partners yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Invite sellers to join your store",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        
                        // ✅ FIX: Add button to switch to All Sellers tab
                        if (isOwner) {
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onInviteClick,
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Icon(Icons.Default.PersonAdd, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Browse All Sellers")
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun MemberCard(
    sellerId: String,
    name: String,
    email: String,
    role: String,
    isOwner: Boolean,
    canRemove: Boolean,
    storeId: String,
    navController: NavController,
    viewModel: CoSellerViewModel,
    onRemove: () -> Unit
) {
    var productCount by remember { mutableStateOf(0) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    LaunchedEffect(sellerId) {
        db.collection("data").document("stock")
            .collection("products")
            .whereEqualTo("sellerId", sellerId)
            .whereEqualTo("coStoreId", storeId)
            .whereEqualTo("isCoStoreProduct", true)
            .get()
            .addOnSuccessListener { docs ->
                productCount = docs.size()
            }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    navController.navigate("sellerProfile/$sellerId/$storeId")
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isOwner) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            role,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOwner) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    "$productCount products in store",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (canRemove) {
                IconButton(onClick = { showRemoveDialog = true }) {
                    Icon(
                        Icons.Default.PersonRemove,
                        contentDescription = "Remove member",
                        tint = Color(0xFFF44336)
                    )
                }
            }
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Remove Member?") },
            text = {
                Column {
                    Text(
                        "Are you sure you want to remove $name from the store?",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("This will:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("• Remove their access to the store", style = MaterialTheme.typography.bodySmall)
                    Text("• Keep their $productCount products in the store", style = MaterialTheme.typography.bodySmall)
                    Text("• They can be re-invited later", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemove()
                        showRemoveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun AllSellersSection(
    allSellers: List<SellerInfo>,
    isLoading: Boolean,
    storeId: String,
    store: CoSellerStoreModel,
    navController: NavController
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Browse All Sellers",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${allSellers.size} sellers registered",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            if (allSellers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Person,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No sellers found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(allSellers) { seller ->
                    // ✅ UPDATED: Check if seller is already a member (support both old and new fields)
                    val isMember = seller.sellerId == store.ownerSellerId || 
                                   store.coSellerIds.contains(seller.sellerId) ||
                                   (store.coSellerId.isNotEmpty() && seller.sellerId == store.coSellerId)
                    
                    SellerProfileCard(
                        sellerId = seller.sellerId,
                        name = seller.name,
                        email = seller.email,
                        profileImage = seller.profileImage,
                        role = if (isMember) "Member" else "Seller",
                        isOwner = false,
                        storeId = storeId,
                        productCount = 0,
                        navController = navController,
                        isMember = isMember
                    )
                }
            }
        }
    }
}

@Composable
fun SellerProfileCard(
    sellerId: String,
    name: String,
    email: String,
    profileImage: String,
    role: String,
    isOwner: Boolean,
    storeId: String,
    productCount: Int,
    navController: NavController,
    isMember: Boolean = false
) {
    // ✅ FIX: Remove product count fetch to improve performance
    // Product count will be shown on detail page instead
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate("sellerProfile/$sellerId/$storeId")
            },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(28.dp),
                color = if (isOwner) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.secondaryContainer
            ) {
                if (profileImage.isNotEmpty()) {
                    AsyncImage(
                        model = profileImage,
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
                            Icons.Default.Person,
                            null,
                            modifier = Modifier.size(32.dp),
                            tint = if (isOwner) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (isOwner) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Star,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = if (isMember) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        role,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isMember)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Arrow icon to indicate clickable
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View profile",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
