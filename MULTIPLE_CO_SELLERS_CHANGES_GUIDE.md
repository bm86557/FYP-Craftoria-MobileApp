# Multiple Co-Sellers Implementation Guide

## 🎯 Goal:
Current: 1 Owner + 1 Co-Seller (2 total)
Target: 1 Owner + Multiple Co-Sellers (e.g., 1 Owner + 4 Co-Sellers = 5 total)

---

## 📋 Required Changes:

### 1️⃣ Data Model Changes

#### File: `CoSellerStoreModel.kt`

**Current:**
```kotlin
data class CoSellerStoreModel(
    val storeId: String = "",
    val storeName: String = "",
    val ownerSellerId: String = "",
    val ownerSellerEmail: String = "",
    val ownerSellerName: String = "",
    val coSellerId: String = "",           // ❌ Single co-seller
    val coSellerEmail: String = "",
    val coSellerName: String = "",
    val memberCount: Int = 1
)
```

**Change To:**
```kotlin
data class CoSellerStoreModel(
    val storeId: String = "",
    val storeName: String = "",
    val ownerSellerId: String = "",
    val ownerSellerEmail: String = "",
    val ownerSellerName: String = "",
    
    // ✅ Change to Lists for multiple co-sellers
    val coSellerIds: List<String> = emptyList(),
    val coSellerEmails: List<String> = emptyList(),
    val coSellerNames: List<String> = emptyList(),
    
    // ✅ Add max members limit
    val maxMembers: Int = 5,  // Configurable (e.g., 5, 10, unlimited)
    val memberCount: Int = 1
)
```

**Impact:** 
- Firestore documents mein field names change honge
- Existing stores migrate karne padenge

---

### 2️⃣ ViewModel Changes

#### File: `CoSellerViewModel.kt`

#### Change 1: `inviteCoSeller()` Function
**Add validation:**
```kotlin
fun inviteCoSeller(email: String, storeId: String) {
    // ✅ ADD: Check if store is full
    db.collection("coSellerStores").document(storeId).get()
        .addOnSuccessListener { storeDoc ->
            val coSellerIds = storeDoc.get("coSellerIds") as? List<String> ?: emptyList()
            val maxMembers = storeDoc.getLong("maxMembers")?.toInt() ?: 5
            
            // Check if store is full
            if (coSellerIds.size >= maxMembers - 1) {  // -1 for owner
                inviteState.value = "store_full"
                return@addOnSuccessListener
            }
            
            // ✅ ADD: Check if user is already a member
            if (coSellerIds.contains(invitedSellerId)) {
                inviteState.value = "already_member"
                return@addOnSuccessListener
            }
            
            // Continue with existing invite logic...
        }
}
```

#### Change 2: `acceptInvite()` Function
**Update to add to list instead of replacing:**
```kotlin
fun acceptInvite(inviteId: String, storeId: String, onResult: (Boolean) -> Unit) {
    db.collection("coSellerStores").document(storeId).get()
        .addOnSuccessListener { storeDoc ->
            val coSellerIds = storeDoc.get("coSellerIds") as? List<String> ?: emptyList()
            val coSellerEmails = storeDoc.get("coSellerEmails") as? List<String> ?: emptyList()
            val coSellerNames = storeDoc.get("coSellerNames") as? List<String> ?: emptyList()
            val maxMembers = storeDoc.getLong("maxMembers")?.toInt() ?: 5
            
            // ✅ Check if store is full
            if (coSellerIds.size >= maxMembers - 1) {
                onResult(false)
                return@addOnSuccessListener
            }
            
            // ✅ Check if already a member
            if (coSellerIds.contains(uid)) {
                onResult(false)
                return@addOnSuccessListener
            }
            
            // ✅ Add to lists instead of replacing
            val updatedIds = coSellerIds + uid
            val updatedEmails = coSellerEmails + userEmail
            val updatedNames = coSellerNames + userName
            
            db.collection("coSellerStores").document(storeId)
                .update(
                    mapOf(
                        "coSellerIds" to updatedIds,
                        "coSellerEmails" to updatedEmails,
                        "coSellerNames" to updatedNames,
                        "memberCount" to (updatedIds.size + 1)  // +1 for owner
                    )
                )
        }
}
```

#### Change 3: Add `removeCoSeller()` Function
**New function to remove a co-seller:**
```kotlin
fun removeCoSeller(storeId: String, coSellerId: String, onResult: (Boolean) -> Unit) {
    db.collection("coSellerStores").document(storeId).get()
        .addOnSuccessListener { storeDoc ->
            val coSellerIds = storeDoc.get("coSellerIds") as? List<String> ?: emptyList()
            val coSellerEmails = storeDoc.get("coSellerEmails") as? List<String> ?: emptyList()
            val coSellerNames = storeDoc.get("coSellerNames") as? List<String> ?: emptyList()
            
            // Find index and remove
            val index = coSellerIds.indexOf(coSellerId)
            if (index != -1) {
                val updatedIds = coSellerIds.toMutableList().apply { removeAt(index) }
                val updatedEmails = coSellerEmails.toMutableList().apply { removeAt(index) }
                val updatedNames = coSellerNames.toMutableList().apply { removeAt(index) }
                
                db.collection("coSellerStores").document(storeId)
                    .update(
                        mapOf(
                            "coSellerIds" to updatedIds,
                            "coSellerEmails" to updatedEmails,
                            "coSellerNames" to updatedNames,
                            "memberCount" to (updatedIds.size + 1)
                        )
                    )
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(false) }
            } else {
                onResult(false)
            }
        }
}
```

#### Change 4: `fetchMyStores()` Function
**Update query to check in list:**
```kotlin
fun fetchMyStores() {
    val uid = auth.currentUser?.uid ?: return
    
    // Owner stores (no change)
    db.collection("coSellerStores")
        .whereEqualTo("ownerSellerId", uid)
        .get()
    
    // ✅ CHANGE: Co-seller stores - check in array
    db.collection("coSellerStores")
        .whereArrayContains("coSellerIds", uid)  // Changed from whereEqualTo
        .get()
}
```

---

### 3️⃣ UI Changes

#### File: `StoreSettingsPage.kt`

#### Change 1: Members Tab
**Display all co-sellers in a list:**
```kotlin
@Composable
fun MembersTab(store: CoSellerStoreModel, ...) {
    LazyColumn {
        // Owner card
        item { OwnerCard(store.ownerSellerId, ...) }
        
        // ✅ CHANGE: Loop through all co-sellers
        items(store.coSellerIds.size) { index ->
            CoSellerCard(
                sellerId = store.coSellerIds[index],
                name = store.coSellerNames[index],
                email = store.coSellerEmails[index],
                onRemove = { 
                    viewModel.removeCoSeller(store.storeId, store.coSellerIds[index]) 
                }
            )
        }
        
        // ✅ ADD: Show available slots
        item {
            Text("Members: ${store.memberCount} / ${store.maxMembers}")
        }
        
        // ✅ CHANGE: Invite button enabled only if not full
        item {
            Button(
                onClick = { showInviteDialog = true },
                enabled = store.coSellerIds.size < store.maxMembers - 1
            ) {
                Text(
                    if (store.coSellerIds.size < store.maxMembers - 1) 
                        "Invite Co-Seller (${store.maxMembers - store.memberCount} slots left)"
                    else 
                        "Store Full"
                )
            }
        }
    }
}
```

#### Change 2: Browse Sellers Tab
**Check if seller is already a member:**
```kotlin
@Composable
fun BrowseSellersTab(store: CoSellerStoreModel, ...) {
    // ✅ CHANGE: Check in list
    val isMember = seller.sellerId == store.ownerSellerId || 
                   store.coSellerIds.contains(seller.sellerId)
    
    // ✅ CHANGE: Check if store is full
    val isFull = store.coSellerIds.size >= store.maxMembers - 1
    
    Button(
        onClick = { /* send invite */ },
        enabled = !isMember && !isFull
    ) {
        Text(
            when {
                isMember -> "Already Member"
                isFull -> "Store Full"
                else -> "Invite"
            }
        )
    }
}
```

#### File: `StoreDetailPage.kt`

#### Change 3: Store Members Section
**Display all members:**
```kotlin
@Composable
fun StoreMembersSection(store: CoSellerStoreModel, ...) {
    Column {
        Text("Store Members (${store.memberCount}/${store.maxMembers})")
        
        // Owner
        MemberCard(
            sellerId = store.ownerSellerId,
            name = store.ownerSellerName,
            role = "Owner"
        )
        
        // ✅ CHANGE: All co-sellers
        store.coSellerIds.forEachIndexed { index, sellerId ->
            MemberCard(
                sellerId = sellerId,
                name = store.coSellerNames[index],
                role = "Co-Seller"
            )
        }
    }
}
```

---

### 4️⃣ Product Access Changes

#### File: `SellerRequestScreen.kt`

**Check if seller is in co-sellers list:**
```kotlin
// ✅ CHANGE: Check in array
val isStoreMember = sellerId == ownerSellerId || 
                    coSellerIds.contains(sellerId)

if (isStoreMember) {
    // Show negotiation request
}
```

#### File: `AddProductPage.kt` (helper function)

**No change needed** - already checks sellerId, which works for all members

---

### 5️⃣ Firestore Rules Changes

#### File: `firestore.rules`

**Update to check in array:**
```javascript
match /data/stock/products/{productId} {
  // ✅ CHANGE: Check if seller is owner OR in coSellerIds array
  allow create: if request.auth != null 
    && (request.auth.uid == request.resource.data.sellerId)
    && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.verificationStatus == 'VERIFIED';
}

match /coSellerStores/{storeId} {
  // ✅ CHANGE: Check if user is owner OR in coSellerIds array
  allow update: if request.auth != null 
    && (request.auth.uid == resource.data.ownerSellerId 
        || resource.data.coSellerIds.hasAny([request.auth.uid]))
    && get(/databases/$(database)/documents/users/$(request.auth.uid)).data.verificationStatus == 'VERIFIED';
}
```

---

### 6️⃣ Data Migration

#### Existing Stores Ko Migrate Karna Padega:

**Migration Script (Cloud Function ya Manual):**
```javascript
// For each existing store:
{
  // Old fields (keep for backward compatibility)
  coSellerId: "user_123",
  coSellerEmail: "user@example.com",
  coSellerName: "User Name",
  
  // ✅ ADD: New fields
  coSellerIds: ["user_123"],  // Convert single to array
  coSellerEmails: ["user@example.com"],
  coSellerNames: ["User Name"],
  maxMembers: 5,
  memberCount: 2  // 1 owner + 1 co-seller
}
```

---

## 📊 Summary of Changes:

| Component | File | Change Type | Complexity |
|-----------|------|-------------|------------|
| Data Model | `CoSellerStoreModel.kt` | Major | High |
| ViewModel | `CoSellerViewModel.kt` | Major | High |
| UI - Members Tab | `StoreSettingsPage.kt` | Major | Medium |
| UI - Store Detail | `StoreDetailPage.kt` | Medium | Medium |
| UI - Browse Sellers | `StoreSettingsPage.kt` | Medium | Medium |
| Product Access | `SellerRequestScreen.kt` | Minor | Low |
| Firestore Rules | `firestore.rules` | Medium | Medium |
| Data Migration | Cloud Function | Major | High |

---

## ⚠️ Important Considerations:

### 1. Backward Compatibility
- Keep old fields (`coSellerId`, `coSellerEmail`, `coSellerName`) for existing stores
- Gradually migrate to new fields
- Support both old and new formats during transition

### 2. Performance
- Firestore `whereArrayContains` query has limitations
- Consider indexing for better performance
- May need composite indexes

### 3. UI/UX
- Show member count: "3/5 members"
- Show available slots: "2 slots remaining"
- Disable invite button when full
- Add "Remove Member" option for owner

### 4. Permissions
- Only owner can remove co-sellers
- Co-sellers can leave voluntarily
- Owner cannot be removed (transfer ownership feature needed)

### 5. Testing
- Test with 0 co-sellers (owner only)
- Test with 1 co-seller (current scenario)
- Test with max co-sellers (full store)
- Test invite when store is full
- Test removing co-sellers
- Test product access for all members

---

## 🎯 Implementation Order:

1. **Phase 1: Data Model** (Breaking change)
   - Update `CoSellerStoreModel.kt`
   - Create migration script

2. **Phase 2: Backend Logic**
   - Update `CoSellerViewModel.kt`
   - Add validation functions
   - Add remove member function

3. **Phase 3: UI Updates**
   - Update Members Tab
   - Update Store Detail Page
   - Update Browse Sellers Tab

4. **Phase 4: Access Control**
   - Update product access checks
   - Update Firestore rules

5. **Phase 5: Testing**
   - Test all scenarios
   - Test migration
   - Test edge cases

---

## 📝 Estimated Effort:

- **Data Model Changes:** 2-3 hours
- **ViewModel Changes:** 4-5 hours
- **UI Changes:** 5-6 hours
- **Firestore Rules:** 1-2 hours
- **Data Migration:** 2-3 hours
- **Testing:** 3-4 hours

**Total: ~20-25 hours of development**

---

## 🚀 Alternative: Simpler Approach

Agar sirf **3-4 members** chahiye (not unlimited), to:
 
**Option 1: Fixed Fields**
```kotlin
data class CoSellerStoreModel(
    val ownerSellerId: String = "",
    val coSeller1Id: String = "",
    val coSeller2Id: String = "",
    val coSeller3Id: String = "",
    // ... up to desired limit
)
```

**Pros:**
- Simpler to implement
- No array handling
- Easier queries

**Cons:**
- Not scalable
- Lots of null/empty checks
- Harder to maintain

---

## ✅ Recommendation:

**Use List/Array approach** kyunki:
- Scalable hai
- Clean code
- Easy to add/remove members
- Future-proof

Bas data migration carefully karna padega! 🎯
