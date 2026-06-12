# 🗑️ Co-Seller Store Delete Feature - Implementation Guide

## 📊 Current Analysis:

### **Existing Files:**
1. ✅ `CoSellerStoreModel.kt` - Store data model
2. ✅ `CoSellerViewModel.kt` - Store management logic
3. ✅ `StoreSettingsPage.kt` - Store settings UI (3 tabs)
4. ✅ `CoSellerStorePage.kt` - Store listing page

### **Current Features:**
- ✅ Create store
- ✅ Update store (name, description, logo, banner)
- ✅ Invite co-sellers
- ✅ Remove co-sellers
- ❌ Delete store (MISSING)

---

## 🎯 Implementation Plan:

### **Step 1: Add Delete Function in ViewModel**
**File:** `CoSellerViewModel.kt`

**Location:** After `removeCoSeller()` function (around line 400)

```kotlin
/**
 * Delete store - Only owner can delete
 * Deletes:
 * - Store document
 * - All store products
 * - All pending invites
 */
fun deleteStore(
    storeId: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val currentUid = auth.currentUser?.uid ?: run {
        onError("User not logged in")
        return
    }

    // Step 1: Verify user is owner
    db.collection("coSellerStores").document(storeId).get()
        .addOnSuccessListener { storeDoc ->
            if (!storeDoc.exists()) {
                onError("Store not found")
                return@addOnSuccessListener
            }

            val ownerId = storeDoc.getString("ownerSellerId") ?: ""
            if (ownerId != currentUid) {
                onError("Only store owner can delete the store")
                return@addOnSuccessListener
            }

            // Step 2: Delete all store products
            db.collection("data").document("stock")
                .collection("products")
                .whereEqualTo("coStoreId", storeId)
                .whereEqualTo("isCoStoreProduct", true)
                .get()
                .addOnSuccessListener { productsSnapshot ->
                    // Delete each product
                    val batch = db.batch()
                    productsSnapshot.documents.forEach { productDoc ->
                        batch.delete(productDoc.reference)
                    }
                    
                    // Step 3: Delete pending invites
                    db.collection("storeInvites")
                        .whereEqualTo("storeId", storeId)
                        .get()
                        .addOnSuccessListener { invitesSnapshot ->
                            invitesSnapshot.documents.forEach { inviteDoc ->
                                batch.delete(inviteDoc.reference)
                            }
                            
                            // Step 4: Delete store document
                            batch.delete(db.collection("coSellerStores").document(storeId))
                            
                            // Commit all deletions
                            batch.commit()
                                .addOnSuccessListener {
                                    // Refresh store list
                                    fetchMyStores()
                                    onSuccess()
                                }
                                .addOnFailureListener { e ->
                                    onError("Failed to delete store: ${e.message}")
                                }
                        }
                        .addOnFailureListener { e ->
                            onError("Failed to delete invites: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    onError("Failed to delete products: ${e.message}")
                }
        }
        .addOnFailureListener { e ->
            onError("Failed to verify ownership: ${e.message}")
        }
}
```

---

### **Step 2: Add Delete Button in StoreSettingsPage**
**File:** `StoreSettingsPage.kt`

**Location:** In `GeneralSettingsTab` composable, after the save/cancel buttons

#### **2.1: Add State Variables (Top of StoreSettingsPage)**
```kotlin
// Add these with other state variables (around line 50)
var showDeleteDialog by remember { mutableStateOf(false) }
var isDeleting by remember { mutableStateOf(false) }
var deleteError by remember { mutableStateOf<String?>(null) }
```

#### **2.2: Add Delete Button in GeneralSettingsTab**
```kotlin
// Add this at the bottom of GeneralSettingsTab, after edit/save buttons
// Location: Around line 200-250 in GeneralSettingsTab

Spacer(Modifier.height(32.dp))

// ✅ DELETE STORE SECTION (Only for Owner)
if (isOwner) {
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)  // Light red
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
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
            
            // Show error if any
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
}
```

#### **2.3: Add Delete Confirmation Dialog**
```kotlin
// Add this at the bottom of StoreSettingsPage, before the closing braces
// Location: Around line 800-900

// ✅ DELETE CONFIRMATION DIALOG
if (showDeleteDialog) {
    AlertDialog(
        onDismissRequest = { 
            if (!isDeleting) showDeleteDialog = false 
        },
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFF44336),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Delete Store?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "Are you sure you want to delete \"${store?.storeName}\"?",
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "This will permanently delete:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text("• The store", style = MaterialTheme.typography.bodySmall)
                Text("• All store products", style = MaterialTheme.typography.bodySmall)
                Text("• All pending invites", style = MaterialTheme.typography.bodySmall)
                Text("• All store data", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Text(
                    "⚠️ This action cannot be undone!",
                    color = Color(0xFFF44336),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isDeleting = true
                    deleteError = null
                    viewModel.deleteStore(
                        storeId = storeId,
                        onSuccess = {
                            isDeleting = false
                            showDeleteDialog = false
                            // Navigate back to store list
                            navController.popBackStack()
                        },
                        onError = { error ->
                            isDeleting = false
                            deleteError = error
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                ),
                enabled = !isDeleting
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Delete Permanently")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { 
                    if (!isDeleting) showDeleteDialog = false 
                },
                enabled = !isDeleting
            ) {
                Text("Cancel")
            }
        }
    )
}
```

---

### **Step 3: Update Firestore Rules (Optional)**
**File:** `firestore.rules`

```javascript
// Add this rule for store deletion
match /coSellerStores/{storeId} {
  // Only owner can delete
  allow delete: if request.auth != null && 
                   request.auth.uid == resource.data.ownerSellerId;
}
```

---

## 🎨 UI Design:

### **General Settings Tab - Delete Section:**
```
┌─────────────────────────────────────┐
│ General Settings                    │
├─────────────────────────────────────┤
│                                     │
│ Store Name: [My Store]             │
│ Description: [...]                  │
│ Logo: [Image]                       │
│ Banner: [Image]                     │
│                                     │
│ [Save Changes]                      │
│                                     │
├─────────────────────────────────────┤
│                                     │
│ ⚠️ Danger Zone                      │
│                                     │
│ Delete this store permanently       │
│                                     │
│ ⚠️ This action cannot be undone.   │
│ All store products and data will    │
│ be permanently deleted.             │
│                                     │
│ [🗑️ Delete Store]                  │
│                                     │
└─────────────────────────────────────┘
```

### **Delete Confirmation Dialog:**
```
┌─────────────────────────────────────┐
│         ⚠️                          │
│                                     │
│     Delete Store?                   │
│                                     │
│ Are you sure you want to delete     │
│ "My Store"?                         │
│                                     │
│ This will permanently delete:       │
│ • The store                         │
│ • All store products                │
│ • All pending invites               │
│ • All store data                    │
│                                     │
│ ⚠️ This action cannot be undone!   │
│                                     │
│ [Cancel] [Delete Permanently]       │
└─────────────────────────────────────┘
```

---

## 🔄 Delete Flow:

```
1. Owner opens Store Settings
   ↓
2. Goes to "General" tab
   ↓
3. Scrolls to "Danger Zone"
   ↓
4. Clicks "Delete Store"
   ↓
5. Confirmation dialog appears
   ↓
6. Owner confirms deletion
   ↓
7. ViewModel.deleteStore() called
   ↓
8. Verify owner (security check)
   ↓
9. Delete all store products
   ↓
10. Delete all pending invites
    ↓
11. Delete store document
    ↓
12. Navigate back to store list
    ↓
13. Success!
```

---

## 🔒 Security Checks:

### **1. Owner Verification:**
```kotlin
val ownerId = storeDoc.getString("ownerSellerId") ?: ""
if (ownerId != currentUid) {
    onError("Only store owner can delete the store")
    return
}
```

### **2. Co-Sellers Cannot Delete:**
- Only `isOwner` sees delete button
- Backend verifies ownership
- Co-sellers get error if they try

### **3. Firestore Rules:**
```javascript
allow delete: if request.auth.uid == resource.data.ownerSellerId;
```

---

## 📦 What Gets Deleted:

### **1. Store Document:**
```
/coSellerStores/{storeId}
```

### **2. All Store Products:**
```
/data/stock/products
  where coStoreId == storeId
  where isCoStoreProduct == true
```

### **3. All Pending Invites:**
```
/storeInvites
  where storeId == storeId
```

### **4. What DOESN'T Get Deleted:**
- ❌ User accounts (owner & co-sellers)
- ❌ Personal products (non-store products)
- ❌ Orders (historical data preserved)
- ❌ Chat messages

---

## ⚠️ Important Notes:

### **1. Only Owner Can Delete:**
```kotlin
val isOwner = store?.ownerSellerId == currentUserId

if (isOwner) {
    // Show delete button
}
```

### **2. Co-Sellers Cannot Delete:**
- Delete button hidden for co-sellers
- Backend verification prevents unauthorized deletion

### **3. Confirmation Required:**
- User must confirm in dialog
- Shows what will be deleted
- Warning about permanent action

### **4. Batch Operation:**
```kotlin
val batch = db.batch()
// Delete products
// Delete invites
// Delete store
batch.commit()
```

---

## 🧪 Testing Checklist:

### **Test 1: Owner Can Delete**
- [ ] Login as store owner
- [ ] Open store settings
- [ ] See "Danger Zone" section
- [ ] Click "Delete Store"
- [ ] Confirm deletion
- [ ] Store deleted successfully
- [ ] Redirected to store list

### **Test 2: Co-Seller Cannot Delete**
- [ ] Login as co-seller
- [ ] Open store settings
- [ ] "Danger Zone" section NOT visible
- [ ] No delete button

### **Test 3: Products Deleted**
- [ ] Store has 5 products
- [ ] Delete store
- [ ] Check Firestore
- [ ] All 5 products deleted

### **Test 4: Invites Deleted**
- [ ] Store has pending invites
- [ ] Delete store
- [ ] Check Firestore
- [ ] All invites deleted

### **Test 5: Cancel Works**
- [ ] Click "Delete Store"
- [ ] Dialog appears
- [ ] Click "Cancel"
- [ ] Dialog closes
- [ ] Store NOT deleted

### **Test 6: Error Handling**
- [ ] Disconnect internet
- [ ] Try to delete
- [ ] Error message shown
- [ ] Store NOT deleted

---

## 📝 Code Locations Summary:

### **1. ViewModel Function:**
```
File: CoSellerViewModel.kt
Location: After removeCoSeller() function (line ~400)
Function: deleteStore()
```

### **2. UI State Variables:**
```
File: StoreSettingsPage.kt
Location: Top of function (line ~50)
Variables: showDeleteDialog, isDeleting, deleteError
```

### **3. Delete Button:**
```
File: StoreSettingsPage.kt
Location: GeneralSettingsTab (line ~200-250)
Component: Danger Zone Card + Delete Button
```

### **4. Confirmation Dialog:**
```
File: StoreSettingsPage.kt
Location: Bottom of StoreSettingsPage (line ~800-900)
Component: AlertDialog with confirmation
```

---

## 🎯 Implementation Steps:

### **Step 1: Add ViewModel Function**
1. Open `CoSellerViewModel.kt`
2. Find `removeCoSeller()` function
3. Add `deleteStore()` function after it
4. Copy code from above

### **Step 2: Add UI State**
1. Open `StoreSettingsPage.kt`
2. Find state variables (line ~50)
3. Add: `showDeleteDialog`, `isDeleting`, `deleteError`

### **Step 3: Add Delete Button**
1. Find `GeneralSettingsTab` composable
2. Scroll to bottom (after save button)
3. Add "Danger Zone" card
4. Add delete button with `if (isOwner)` check

### **Step 4: Add Confirmation Dialog**
1. Scroll to bottom of `StoreSettingsPage`
2. Before closing braces
3. Add `if (showDeleteDialog)` block
4. Add AlertDialog with confirmation

### **Step 5: Test**
1. Build project: `./gradlew assembleDebug`
2. Login as store owner
3. Open store settings
4. Try deleting store
5. Verify all data deleted

---

## 🚀 Build Command:

```bash
./gradlew assembleDebug
```

---

## ✅ Summary:

### **What to Add:**
1. ✅ `deleteStore()` function in ViewModel
2. ✅ State variables in StoreSettingsPage
3. ✅ "Danger Zone" section in General tab
4. ✅ Delete button (owner only)
5. ✅ Confirmation dialog

### **What Gets Deleted:**
1. ✅ Store document
2. ✅ All store products
3. ✅ All pending invites

### **Security:**
1. ✅ Only owner can delete
2. ✅ Backend verification
3. ✅ Confirmation required
4. ✅ Firestore rules

### **User Experience:**
1. ✅ Clear warning messages
2. ✅ Confirmation dialog
3. ✅ Loading states
4. ✅ Error handling
5. ✅ Auto-redirect after deletion

---

**Implementation Guide Complete! Follow the steps above to add store delete functionality.** 🎊
