# 🔧 Store Members Tab - Complete Fix Guide

## 🐛 Current Issues:

### **Problem 1: Partners Nahi Dikh Rahe**
**Reason:** Members tab mein sirf old field check ho raha hai:
```kotlin
// ❌ CURRENT CODE (Line 860)
if (store.coSellerId.isNotEmpty()) {
    // Sirf 1 co-seller dikhta hai
}
```

**Issue:** New field `coSellerIds` (List) check nahi ho raha, jo unlimited co-sellers support karta hai.

---

### **Problem 2: Remove Button Nahi Hai**
**Reason:** `SellerProfileCard` mein remove functionality nahi hai.

---

### **Problem 3: Store Delete Option Nahi Hai**
**Reason:** General tab mein "Danger Zone" section nahi hai.

---

## ✅ Complete Solution:

### **Fix 1: StoreMembersSection - Show All Co-Sellers**

**File:** `StoreSettingsPage.kt`
**Location:** Line 830-870 (StoreMembersSection function)

#### **Replace Karo:**
```kotlin
@Composable
fun StoreMembersSection(
    store: CoSellerStoreModel,
    storeId: String,
    navController: NavController,
    viewModel: CoSellerViewModel,  // ✅ ADD: ViewModel parameter
    currentUserId: String  // ✅ ADD: Current user ID
) {
    val isOwner = store.ownerSellerId == currentUserId
    
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
                "${1 + store.coSellerIds.size} members",  // ✅ Dynamic count
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        
        // ✅ OWNER CARD
        item {
            MemberCard(
                sellerId = store.ownerSellerId,
                name = store.ownerSellerName.ifEmpty { "Store Owner" },
                email = store.ownerSellerEmail,
                role = "Owner",
                isOwner = true,
                canRemove = false,  // Owner cannot be removed
                storeId = storeId,
                navController = navController,
                onRemove = {}
            )
        }
        
        // ✅ NEW: Show all co-sellers from coSellerIds list
        items(store.coSellerIds.size) { index ->
            MemberCard(
                sellerId = store.coSellerIds[index],
                name = store.coSellerNames.getOrNull(index) ?: "Co-Seller",
                email = store.coSellerEmails.getOrNull(index) ?: "",
                role = "Partner",
                isOwner = false,
                canRemove = isOwner,  // ✅ Only owner can remove
                storeId = storeId,
                navController = navController,
                onRemove = {
                    // ✅ Remove co-seller
                    viewModel.removeCoSeller(
                        storeId = storeId,
                        coSellerId = store.coSellerIds[index],
                        onResult = { success ->
                            if (success) {
                                // Refresh store data
                                viewModel.fetchStoreById(storeId) { }
                            }
                        }
                    )
                }
            )
        }
        
        // ✅ BACKWARD COMPATIBILITY: Show old co-seller if exists
        if (store.coSellerId.isNotEmpty() && !store.coSellerIds.contains(store.coSellerId)) {
            item {
                MemberCard(
                    sellerId = store.coSellerId,
                    name = store.coSellerName.ifEmpty { "Partner" },
                    email = store.coSellerEmail,
                    role = "Partner (Legacy)",
                    isOwner = false,
                    canRemove = isOwner,
                    storeId = storeId,
                    navController = navController,
                    onRemove = {
                        viewModel.removeCoSeller(
                            storeId = storeId,
                            coSellerId = store.coSellerId,
                            onResult = { success ->
                                if (success) {
                                    viewModel.fetchStoreById(storeId) { }
                                }
                            }
                        )
                    }
                )
            }
        }
        
        // ✅ Empty state
        if (store.coSellerIds.isEmpty() && store.coSellerId.isEmpty()) {
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
                    }
                }
            }
        }
    }
}
```

---

### **Fix 2: New MemberCard Component with Remove Button**

**File:** `StoreSettingsPage.kt`
**Location:** Add after StoreMembersSection (around line 900)

```kotlin
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
    onRemove: () -> Unit
) {
    var productCount by remember { mutableStateOf(0) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    
    // Fetch product count
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
            // Profile Icon
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
            
            // Member Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    // Role Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isOwner) 
                            Color(0xFF4CAF50) 
                        else 
                            MaterialTheme.colorScheme.secondaryContainer
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
            
            // ✅ REMOVE BUTTON (Only if canRemove is true)
            if (canRemove) {
                IconButton(
                    onClick = { showRemoveDialog = true }
                ) {
                    Icon(
                        Icons.Default.PersonRemove,
                        contentDescription = "Remove member",
                        tint = Color(0xFFF44336)
                    )
                }
            }
        }
    }
    
    // ✅ REMOVE CONFIRMATION DIALOG
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
            title = {
                Text("Remove Member?")
            },
            text = {
                Column {
                    Text(
                        "Are you sure you want to remove $name from the store?",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "This will:",
                        style = MaterialTheme.typography.bodyMedium
                    )
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    )
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
```

---

### **Fix 3: Update Tab Call to Pass Parameters**

**File:** `StoreSettingsPage.kt`
**Location:** Line 120-130 (where tabs are called)

#### **Replace:**
```kotlin
when (selectedTab) {
    0 -> GeneralSettingsTab(...)
    1 -> StoreMembersSection(
        store = store!!,
        storeId = storeId,
        navController = navController,
        viewModel = viewModel,  // ✅ ADD
        currentUserId = currentUserId  // ✅ ADD
    )
    2 -> AllSellersSection(...)
}
```

---

### **Fix 4: Add Store Delete in General Tab**

**File:** `StoreSettingsPage.kt`
**Location:** In GeneralSettingsTab, after save button (around line 250)

```kotlin
// ✅ ADD: Delete Store Section (Only for Owner)
if (isOwner) {
    Spacer(Modifier.height(32.dp))
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
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
            
            var showDeleteDialog by remember { mutableStateOf(false) }
            var isDeleting by remember { mutableStateOf(false) }
            
            Button(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                ),
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
            
            // Delete Dialog
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
                                        navController.popBackStack()
                                    },
                                    onError = { error ->
                                        isDeleting = false
                                        // Show error
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336)
                            ),
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
```

---

## 📊 Summary - Kya Karna Hai:

### **1. StoreMembersSection Function Update:**
**Location:** Line 830-870
**Changes:**
- ✅ Add `viewModel` parameter
- ✅ Add `currentUserId` parameter
- ✅ Replace single co-seller check with `items(store.coSellerIds.size)`
- ✅ Show all co-sellers from list
- ✅ Add empty state
- ✅ Add member count

### **2. New MemberCard Component:**
**Location:** After StoreMembersSection (line 900)
**Features:**
- ✅ Profile icon
- ✅ Name, email, role badge
- ✅ Product count
- ✅ Remove button (only for owner)
- ✅ Remove confirmation dialog
- ✅ Clickable to view profile

### **3. Update Tab Call:**
**Location:** Line 120-130
**Changes:**
- ✅ Pass `viewModel` to StoreMembersSection
- ✅ Pass `currentUserId` to StoreMembersSection

### **4. Add Delete Store:**
**Location:** GeneralSettingsTab (line 250)
**Features:**
- ✅ "Danger Zone" section
- ✅ Delete button (owner only)
- ✅ Confirmation dialog
- ✅ Loading state

---

## 🎨 UI Preview:

### **Members Tab - With Partners:**
```
┌─────────────────────────────────────┐
│ Current Store Members               │
│ 3 members                           │
├─────────────────────────────────────┤
│ 👤 John Doe                         │
│    john@email.com        [Owner]    │
│    15 products in store             │
├─────────────────────────────────────┤
│ 👤 Alice Smith              [🗑️]    │
│    alice@email.com      [Partner]   │
│    8 products in store              │
├─────────────────────────────────────┤
│ 👤 Bob Wilson               [🗑️]    │
│    bob@email.com        [Partner]   │
│    12 products in store             │
└─────────────────────────────────────┘
```

### **Members Tab - Empty:**
```
┌─────────────────────────────────────┐
│ Current Store Members               │
│ 1 member                            │
├─────────────────────────────────────┤
│ 👤 John Doe                         │
│    john@email.com        [Owner]    │
│    15 products in store             │
├─────────────────────────────────────┤
│         👥                          │
│    No partners yet                  │
│    Invite sellers to join your      │
│    store                            │
└─────────────────────────────────────┘
```

### **Remove Dialog:**
```
┌─────────────────────────────────────┐
│         ⚠️                          │
│     Remove Member?                  │
│                                     │
│ Are you sure you want to remove     │
│ Alice Smith from the store?         │
│                                     │
│ This will:                          │
│ • Remove their access to the store  │
│ • Keep their 8 products in store    │
│ • They can be re-invited later      │
│                                     │
│ [Cancel]  [Remove]                  │
└─────────────────────────────────────┘
```

---

## ✅ Complete Implementation Checklist:

### **Members Tab:**
- [ ] Update `StoreMembersSection` function
- [ ] Add `viewModel` and `currentUserId` parameters
- [ ] Replace single co-seller with list iteration
- [ ] Create new `MemberCard` component
- [ ] Add remove button (owner only)
- [ ] Add remove confirmation dialog
- [ ] Add empty state
- [ ] Update tab call to pass parameters

### **Delete Store:**
- [ ] Add "Danger Zone" section in General tab
- [ ] Add delete button (owner only)
- [ ] Add delete confirmation dialog
- [ ] Add `deleteStore()` function in ViewModel
- [ ] Handle loading states
- [ ] Navigate back after deletion

---

**Complete guide ready! Ye sab changes karne se:**
1. ✅ Sab partners dikhengi (unlimited)
2. ✅ Owner partners ko remove kar sakta hai
3. ✅ Owner store delete kar sakta hai
4. ✅ Empty state dikhega agar koi partner nahi
5. ✅ Product count dikhega har member ka