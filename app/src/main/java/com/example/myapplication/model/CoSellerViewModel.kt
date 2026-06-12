package com.example.myapplication.model

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class CoSellerViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var myStores = mutableStateOf<List<CoSellerStoreModel>>(emptyList())
    var pendingInvites = mutableStateOf<List<StoreInviteModel>>(emptyList())
    var createStoreState = mutableStateOf("")
    var inviteState = mutableStateOf("")
    var createdStoreId = mutableStateOf("")  // ← yahan upar rakho

    fun createCoStore(
        context: Context,
        storeName: String,
        storeDescription: String,
        logoUri: Uri?,
        bannerUri: Uri?
    ) {
        val currentUid = auth.currentUser?.uid ?: return
        createStoreState.value = "loading"
        val storeId = UUID.randomUUID().toString()

        // Get current user info first
        db.collection("users").document(currentUid).get()
            .addOnSuccessListener { userDoc ->
                val ownerEmail = userDoc.getString("email") ?: ""
                val ownerName = userDoc.getString("name") ?: ""

                // Parallel upload for better performance
                var logoUrl: String? = null
                var bannerUrl: String? = null
                var logoCompleted = logoUri == null
                var bannerCompleted = bannerUri == null
                var hasError = false

                fun checkAndSave() {
                    if (hasError) return
                    if (logoCompleted && bannerCompleted) {
                        saveStoreToFirestore(
                            storeId, storeName, storeDescription, currentUid,
                            ownerEmail, ownerName, logoUrl ?: "", bannerUrl ?: ""
                        )
                    }
                }

                // Upload logo and banner in parallel
                if (logoUri != null) {
                    CloudinaryRepository.uploadImageToCloudinary(
                        context = context,
                        uri = logoUri,
                        preset = "logo_upload",
                        onSuccess = { url ->
                            logoUrl = url
                            logoCompleted = true
                            checkAndSave()
                        },
                        onError = { error ->
                            hasError = true
                            createStoreState.value = "error"
                        }
                    )
                }

                if (bannerUri != null) {
                    CloudinaryRepository.uploadImageToCloudinary(
                        context = context,
                        uri = bannerUri,
                        preset = "banner_upload",
                        onSuccess = { url ->
                            bannerUrl = url
                            bannerCompleted = true
                            checkAndSave()
                        },
                        onError = { error ->
                            hasError = true
                            createStoreState.value = "error"
                        }
                    )
                }

                // If no images, save immediately
                if (logoUri == null && bannerUri == null) {
                    saveStoreToFirestore(storeId, storeName, storeDescription, currentUid, ownerEmail, ownerName, "", "")
                }
            }
            .addOnFailureListener {
                createStoreState.value = "error"
            }
    }

    private fun saveStoreToFirestore(
        storeId: String,
        storeName: String,
        storeDescription: String,
        ownerUid: String,
        ownerEmail: String,
        ownerName: String,
        logoUrl: String,
        bannerUrl: String
    ) {
        try {
            val store = CoSellerStoreModel(
                storeId = storeId,
                storeName = storeName,
                storeDescription = storeDescription,
                storeLogo = logoUrl,
                storeBanner = bannerUrl,
                ownerSellerId = ownerUid,
                ownerSellerEmail = ownerEmail,
                ownerSellerName = ownerName,
                status = "ACTIVE",
                memberCount = 1
            )

            db.collection("coSellerStores")
                .document(storeId)
                .set(store)
                .addOnSuccessListener {
                    createdStoreId.value = storeId
                    createStoreState.value = "success"
                    fetchMyStores()
                }
                .addOnFailureListener { e ->
                    createStoreState.value = "error"
                }
        } catch (e: Exception) {
            createStoreState.value = "error"
        }
    }

    fun inviteCoSeller(email: String, storeId: String) {
        if (email.isBlank() || storeId.isBlank()) {
            inviteState.value = "error"
            return
        }

        inviteState.value = "loading"
        try {
            // First check if user exists and is a seller
            db.collection("users")
                .whereEqualTo("email", email.trim())
                .whereEqualTo("role", "seller")
                .get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        inviteState.value = "not_seller"
                    } else {
                        val invitedSellerId = result.documents.first().id
                        val invitedSellerName = result.documents.first().getString("name") ?: ""
                        val verificationStatus = result.documents.first().getString("verificationStatus") ?: "NOT_SUBMITTED"
                        
                        // ✅ DETAILED LOGGING for debugging
                        android.util.Log.d("CoSellerViewModel", "=== INVITE CHECK ===")
                        android.util.Log.d("CoSellerViewModel", "Seller ID: $invitedSellerId")
                        android.util.Log.d("CoSellerViewModel", "Seller Name: $invitedSellerName")
                        android.util.Log.d("CoSellerViewModel", "Seller Email: ${email.trim()}")
                        android.util.Log.d("CoSellerViewModel", "Verification Status: $verificationStatus")
                        android.util.Log.d("CoSellerViewModel", "==================")
                        
                        // ✅ Check if seller is VERIFIED before sending invite
                        if (verificationStatus != "VERIFIED") {
                            android.util.Log.e("CoSellerViewModel", "❌ Invite BLOCKED - Seller not verified (Status: $verificationStatus)")
                            inviteState.value = "not_verified"
                            return@addOnSuccessListener
                        }
                        
                        android.util.Log.d("CoSellerViewModel", "✅ Seller is VERIFIED - Proceeding with invite")

                        // Get current user and store info
                        val currentUid = auth.currentUser?.uid ?: return@addOnSuccessListener

                        db.collection("users").document(currentUid).get()
                            .addOnSuccessListener { ownerDoc ->
                                val ownerName = ownerDoc.getString("name") ?: ""
                                val ownerEmail = ownerDoc.getString("email") ?: ""

                                db.collection("coSellerStores").document(storeId).get()
                                    .addOnSuccessListener { storeDoc ->
                                        val storeName = storeDoc.getString("storeName") ?: ""
                                        val storeLogo = storeDoc.getString("storeLogo") ?: ""
                                        
                                        // ✅ Check if user is already a member (support both old and new fields)
                                        val coSellerIds = storeDoc.get("coSellerIds") as? List<String> ?: emptyList()
                                        val oldCoSellerId = storeDoc.getString("coSellerId") ?: ""
                                        
                                        val isAlreadyMember = coSellerIds.contains(invitedSellerId) || 
                                                             (oldCoSellerId.isNotEmpty() && oldCoSellerId == invitedSellerId)
                                        
                                        if (isAlreadyMember) {
                                            inviteState.value = "already_member"
                                            return@addOnSuccessListener
                                        }

                                        // Create invite
                                        val inviteId = UUID.randomUUID().toString()
                                        val invite = StoreInviteModel(
                                            inviteId = inviteId,
                                            storeId = storeId,
                                            storeName = storeName,
                                            storeLogo = storeLogo,
                                            ownerSellerId = currentUid,
                                            ownerSellerName = ownerName,
                                            ownerSellerEmail = ownerEmail,
                                            invitedSellerId = invitedSellerId,
                                            invitedSellerEmail = email.trim(),
                                            status = "PENDING"
                                        )

                                        // Save invite to Firestore
                                        db.collection("storeInvites")
                                            .document(inviteId)
                                            .set(invite)
                                            .addOnSuccessListener {
                                                android.util.Log.d("CoSellerViewModel", "✅ Invite sent successfully to verified seller")
                                                inviteState.value = "success"
                                            }
                                            .addOnFailureListener { e ->
                                                android.util.Log.e("CoSellerViewModel", "❌ Failed to save invite: ${e.message}")
                                                inviteState.value = "error"
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        android.util.Log.e("CoSellerViewModel", "❌ Failed to fetch store: ${e.message}")
                                        inviteState.value = "error"
                                    }
                            }
                            .addOnFailureListener { e ->
                                android.util.Log.e("CoSellerViewModel", "❌ Failed to fetch owner: ${e.message}")
                                inviteState.value = "error"
                            }
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("CoSellerViewModel", "❌ Failed to find seller: ${e.message}")
                    inviteState.value = "error"
                }
        } catch (e: Exception) {
            android.util.Log.e("CoSellerViewModel", "❌ Exception in inviteCoSeller: ${e.message}")
            inviteState.value = "error"
        }
    }

    fun fetchPendingInvites() {
        val uid = auth.currentUser?.uid ?: return

        try {
            db.collection("storeInvites")
                .whereEqualTo("invitedSellerId", uid)
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener { docs ->
                    pendingInvites.value = docs.toObjects(StoreInviteModel::class.java)
                }
                .addOnFailureListener {
                    pendingInvites.value = emptyList()
                }
        } catch (e: Exception) {
            pendingInvites.value = emptyList()
        }
    }

    fun acceptInvite(inviteId: String, storeId: String, onResult: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        try {
            // Get user info
            db.collection("users").document(uid).get()
                .addOnSuccessListener { userDoc ->
                    // ✅ DETAILED LOGGING for debugging
                    android.util.Log.d("CoSellerViewModel", "=== ACCEPT INVITE CHECK ===")
                    android.util.Log.d("CoSellerViewModel", "User ID: $uid")
                    android.util.Log.d("CoSellerViewModel", "User exists: ${userDoc.exists()}")
                    
                    if (!userDoc.exists()) {
                        android.util.Log.e("CoSellerViewModel", "❌ User document not found!")
                        onResult(false)
                        return@addOnSuccessListener
                    }
                    
                    // ✅ CHECK: Seller must be VERIFIED
                    val verificationStatus = userDoc.getString("verificationStatus") ?: "NOT_SUBMITTED"
                    val userName = userDoc.getString("name") ?: ""
                    val userEmail = userDoc.getString("email") ?: ""
                    val userRole = userDoc.getString("role") ?: ""
                    
                    android.util.Log.d("CoSellerViewModel", "User Name: $userName")
                    android.util.Log.d("CoSellerViewModel", "User Email: $userEmail")
                    android.util.Log.d("CoSellerViewModel", "User Role: $userRole")
                    android.util.Log.d("CoSellerViewModel", "Verification Status: '$verificationStatus'")
                    android.util.Log.d("CoSellerViewModel", "Status Length: ${verificationStatus.length}")
                    android.util.Log.d("CoSellerViewModel", "==================")
                    
                    if (verificationStatus != "VERIFIED") {
                        android.util.Log.e("CoSellerViewModel", "❌ Accept BLOCKED - Seller not verified (Status: '$verificationStatus')")
                        onResult(false)
                        return@addOnSuccessListener
                    }
                    
                    android.util.Log.d("CoSellerViewModel", "✅ Seller is VERIFIED - Proceeding with accept")

                    // Get current store data
                    db.collection("coSellerStores").document(storeId).get()
                        .addOnSuccessListener { storeDoc ->
                            // ✅ NEW: Support both old and new fields
                            val coSellerIds = (storeDoc.get("coSellerIds") as? List<String>)?.toMutableList() ?: mutableListOf()
                            val coSellerEmails = (storeDoc.get("coSellerEmails") as? List<String>)?.toMutableList() ?: mutableListOf()
                            val coSellerNames = (storeDoc.get("coSellerNames") as? List<String>)?.toMutableList() ?: mutableListOf()
                            
                            // Check if already a member
                            if (coSellerIds.contains(uid)) {
                                android.util.Log.e("CoSellerViewModel", "❌ Already a member")
                                onResult(false)
                                return@addOnSuccessListener
                            }
                            
                            // Add to lists
                            coSellerIds.add(uid)
                            coSellerEmails.add(userEmail)
                            coSellerNames.add(userName)
                            
                            // ✅ Update with both old and new fields for backward compatibility
                            val updates = mutableMapOf<String, Any>(
                                "coSellerIds" to coSellerIds,
                                "coSellerEmails" to coSellerEmails,
                                "coSellerNames" to coSellerNames,
                                "memberCount" to (coSellerIds.size + 1)  // +1 for owner
                            )
                            
                            // If this is the first co-seller, also update old fields for backward compatibility
                            if (coSellerIds.size == 1) {
                                updates["coSellerId"] = uid
                                updates["coSellerEmail"] = userEmail
                                updates["coSellerName"] = userName
                            }

                            // Update store
                            db.collection("coSellerStores")
                                .document(storeId)
                                .update(updates)
                                .addOnSuccessListener {
                                    android.util.Log.d("CoSellerViewModel", "Store updated successfully")
                                    // Update invite status
                                    db.collection("storeInvites")
                                        .document(inviteId)
                                        .update("status", "ACCEPTED")
                                        .addOnSuccessListener {
                                            android.util.Log.d("CoSellerViewModel", "Invite accepted successfully")
                                            fetchPendingInvites()
                                            fetchMyStores()
                                            onResult(true)
                                        }
                                        .addOnFailureListener { e ->
                                            android.util.Log.e("CoSellerViewModel", "Failed to update invite: ${e.message}")
                                            onResult(false)
                                        }
                                }
                                .addOnFailureListener { e ->
                                    android.util.Log.e("CoSellerViewModel", "Failed to update store: ${e.message}")
                                    onResult(false)
                                }
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("CoSellerViewModel", "Failed to fetch store: ${e.message}")
                            onResult(false)
                        }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("CoSellerViewModel", "Failed to fetch user: ${e.message}")
                    onResult(false)
                }
        } catch (e: Exception) {
            android.util.Log.e("CoSellerViewModel", "Exception in acceptInvite: ${e.message}")
            onResult(false)
        }
    }

    fun rejectInvite(inviteId: String, onResult: (Boolean) -> Unit) {
        try {
            db.collection("storeInvites")
                .document(inviteId)
                .update("status", "REJECTED")
                .addOnSuccessListener {
                    fetchPendingInvites()
                    onResult(true)
                }
                .addOnFailureListener {
                    onResult(false)
                }
        } catch (e: Exception) {
            onResult(false)
        }
    }
    
    // ✅ NEW: Delete entire store (owner only)
    fun deleteStore(
        storeId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUid = auth.currentUser?.uid ?: run {
            onError("User not logged in")
            return
        }

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

                // Delete all store products
                db.collection("data").document("stock")
                    .collection("products")
                    .whereEqualTo("coStoreId", storeId)
                    .whereEqualTo("isCoStoreProduct", true)
                    .get()
                    .addOnSuccessListener { productsSnapshot ->
                        val batch = db.batch()
                        productsSnapshot.documents.forEach { productDoc ->
                            batch.delete(productDoc.reference)
                        }
                        
                        // Delete pending invites
                        db.collection("storeInvites")
                            .whereEqualTo("storeId", storeId)
                            .get()
                            .addOnSuccessListener { invitesSnapshot ->
                                invitesSnapshot.documents.forEach { inviteDoc ->
                                    batch.delete(inviteDoc.reference)
                                }
                                
                                // Delete store document
                                batch.delete(db.collection("coSellerStores").document(storeId))
                                
                                // Commit all deletions
                                batch.commit()
                                    .addOnSuccessListener {
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
    
    // ✅ NEW: Remove a co-seller from store
    fun removeCoSeller(storeId: String, coSellerId: String, onResult: (Boolean) -> Unit) {
        try {
            db.collection("coSellerStores").document(storeId).get()
                .addOnSuccessListener { storeDoc ->
                    val coSellerIds = (storeDoc.get("coSellerIds") as? List<String>)?.toMutableList() ?: mutableListOf()
                    val coSellerEmails = (storeDoc.get("coSellerEmails") as? List<String>)?.toMutableList() ?: mutableListOf()
                    val coSellerNames = (storeDoc.get("coSellerNames") as? List<String>)?.toMutableList() ?: mutableListOf()
                    
                    // Find index and remove
                    val index = coSellerIds.indexOf(coSellerId)
                    if (index != -1) {
                        coSellerIds.removeAt(index)
                        if (index < coSellerEmails.size) coSellerEmails.removeAt(index)
                        if (index < coSellerNames.size) coSellerNames.removeAt(index)
                        
                        val updates = mutableMapOf<String, Any>(
                            "coSellerIds" to coSellerIds,
                            "coSellerEmails" to coSellerEmails,
                            "coSellerNames" to coSellerNames,
                            "memberCount" to (coSellerIds.size + 1)  // +1 for owner
                        )
                        
                        // ✅ Update old fields for backward compatibility
                        if (coSellerIds.isEmpty()) {
                            // No co-sellers left, clear old fields
                            updates["coSellerId"] = ""
                            updates["coSellerEmail"] = ""
                            updates["coSellerName"] = ""
                        } else {
                            // Update old fields with first co-seller
                            updates["coSellerId"] = coSellerIds[0]
                            updates["coSellerEmail"] = coSellerEmails.getOrElse(0) { "" }
                            updates["coSellerName"] = coSellerNames.getOrElse(0) { "" }
                        }
                        
                        db.collection("coSellerStores")
                            .document(storeId)
                            .update(updates)
                            .addOnSuccessListener {
                                fetchMyStores()
                                onResult(true)
                            }
                            .addOnFailureListener {
                                onResult(false)
                            }
                    } else {
                        onResult(false)
                    }
                }
                .addOnFailureListener {
                    onResult(false)
                }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun addStoreProduct(
        context: Context,
        storeId: String,
        title: String,
        description: String,
        price: String,
        actualPrice: String,
        category: String,
        minDealPrice: String,
        imageUris: List<Uri>,
        otherDetails: Map<String, String>,
        onResult: (Boolean) -> Unit
    ) {
        val currentUid = auth.currentUser?.uid ?: return

        try {
            // First get store name
            db.collection("coSellerStores").document(storeId).get()
                .addOnSuccessListener { storeDoc ->
                    val storeName = storeDoc.getString("storeName") ?: ""

                    if (imageUris.isEmpty()) {
                        // No images, save directly
                        saveProductToFirestore(
                            storeId, storeName, currentUid, title, description, price,
                            actualPrice, category, minDealPrice, emptyList(),
                            otherDetails, onResult
                        )
                    } else {
                        // Upload images first
                        var uploadedCount = 0
                        val collectedUrls = mutableListOf<String>()
                        var hasError = false

                        imageUris.forEach { uri ->
                            CloudinaryRepository.uploadImageToCloudinary(
                                context = context,
                                uri = uri,
                                preset = "product_upload",
                                onSuccess = { imageUrl ->
                                    if (!hasError) {
                                        collectedUrls.add(imageUrl)
                                        uploadedCount++
                                        if (uploadedCount == imageUris.size) {
                                            saveProductToFirestore(
                                                storeId, storeName, currentUid, title, description,
                                                price, actualPrice, category, minDealPrice,
                                                collectedUrls.toList(), otherDetails, onResult
                                            )
                                        }
                                    }
                                },
                                onError = { error ->
                                    if (!hasError) {
                                        hasError = true
                                        onResult(false)
                                    }
                                }
                            )
                        }
                    }
                }
                .addOnFailureListener {
                    onResult(false)
                }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    private fun saveProductToFirestore(
        storeId: String,
        storeName: String,
        sellerId: String,
        title: String,
        description: String,
        price: String,
        actualPrice: String,
        category: String,
        minDealPrice: String,
        images: List<String>,
        otherDetails: Map<String, String>,
        onResult: (Boolean) -> Unit
    ) {
        try {
            db.collection("coSellerStores").document(storeId).get()
                .addOnSuccessListener { storeDoc ->
                    val storeStatus = storeDoc.getString("status") ?: "ACTIVE"
                    if (storeStatus == "FLAGGED" || storeStatus == "INACTIVE") {
                        android.util.Log.e("CoSellerViewModel", "❌ Store is $storeStatus - blocking product upload")
                        onResult(false)
                        return@addOnSuccessListener
                    }

                    db.collection("users").document(sellerId).get()
                        .addOnSuccessListener { userDoc ->
                            val verificationStatus = userDoc.getString("verificationStatus") ?: "NOT_SUBMITTED"
                            if (verificationStatus != "VERIFIED") {
                                onResult(false)
                                return@addOnSuccessListener
                            }

                            val sellerName = userDoc.getString("name") ?: ""

                            val docRef = db.collection("data").document("stock")
                                .collection("products").document()

                            val product = mapOf(
                                "id" to docRef.id,
                                "sellerId" to sellerId,
                                "sellerName" to sellerName,
                                "title" to title,
                                "description" to description,
                                "price" to price,
                                "actualPrice" to actualPrice,
                                "category" to category,
                                "minDealPrice" to (minDealPrice.toIntOrNull() ?: 0),
                                "images" to images,
                                "otherDetails" to otherDetails,
                                "coStoreId" to storeId,
                                "coStoreName" to storeName,
                                "isCoStoreProduct" to true,
                                "status" to "pending",
                                "rejectionReason" to null,
                                "reviewedBy" to null,
                                "createdAt" to com.google.firebase.Timestamp.now()
                            )

                            docRef.set(product)
                                .addOnSuccessListener {
                                    onResult(true)
                                }
                                .addOnFailureListener {
                                    onResult(false)
                                }
                        }
                        .addOnFailureListener {
                            onResult(false)
                        }
                }
                .addOnFailureListener {
                    onResult(false)
                }
        } catch (e: Exception) {
            onResult(false)
        }
    }

    fun fetchStoreProducts(storeId: String, onResult: (List<ProductModel>) -> Unit) {
        try {
            db.collection("data").document("stock")
                .collection("products")
                .whereEqualTo("coStoreId", storeId)
                .whereEqualTo("isCoStoreProduct", true)
                .whereEqualTo("status", "approved")  // ✅ BUYER SIDE: Only approved products
                .get()
                .addOnSuccessListener { docs ->
                    val products = docs.toObjects(ProductModel::class.java)
                    onResult(products)
                }
                .addOnFailureListener {
                    onResult(emptyList())
                }
        } catch (e: Exception) {
            onResult(emptyList())
        }
    }

    fun fetchAllStores(onResult: (List<CoSellerStoreModel>) -> Unit) {
        try {
            db.collection("coSellerStores")
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener { docs ->
                    val stores = docs.toObjects(CoSellerStoreModel::class.java)
                    onResult(stores)
                }
                .addOnFailureListener {
                    onResult(emptyList())
                }
        } catch (e: Exception) {
            onResult(emptyList())
        }
    }

    fun fetchMyStores() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            myStores.value = emptyList()
            return
        }

        try {
            // Parallel fetch for better performance
            var ownerStores: List<CoSellerStoreModel>? = null
            var coStoresOld: List<CoSellerStoreModel>? = null
            var coStoresNew: List<CoSellerStoreModel>? = null

            fun combineResults() {
                if (ownerStores != null && coStoresOld != null && coStoresNew != null) {
                    // Combine and remove duplicates by storeId
                    val allStores = (ownerStores!! + coStoresOld!! + coStoresNew!!)
                        .distinctBy { it.storeId }
                    myStores.value = allStores
                }
            }

            // Fetch stores where user is owner
            db.collection("coSellerStores")
                .whereEqualTo("ownerSellerId", uid)
                .get()
                .addOnSuccessListener { ownerDocs ->
                    ownerStores = ownerDocs.toObjects(CoSellerStoreModel::class.java)
                    combineResults()
                }
                .addOnFailureListener {
                    ownerStores = emptyList()
                    combineResults()
                }

            // ✅ Fetch stores where user is co-seller (old field - for backward compatibility)
            db.collection("coSellerStores")
                .whereEqualTo("coSellerId", uid)
                .get()
                .addOnSuccessListener { coDocs ->
                    coStoresOld = coDocs.toObjects(CoSellerStoreModel::class.java)
                    combineResults()
                }
                .addOnFailureListener {
                    coStoresOld = emptyList()
                    combineResults()
                }
            
            // ✅ NEW: Fetch stores where user is in coSellerIds array
            db.collection("coSellerStores")
                .whereArrayContains("coSellerIds", uid)
                .get()
                .addOnSuccessListener { newCoDocs ->
                    coStoresNew = newCoDocs.toObjects(CoSellerStoreModel::class.java)
                    combineResults()
                }
                .addOnFailureListener {
                    coStoresNew = emptyList()
                    combineResults()
                }
        } catch (e: Exception) {
            myStores.value = emptyList()
        }
    }

    fun fetchStoreById(storeId: String, onResult: (CoSellerStoreModel?) -> Unit) {
        try {
            db.collection("coSellerStores")
                .document(storeId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val store = doc.toObject(CoSellerStoreModel::class.java)
                        onResult(store)
                    } else {
                        onResult(null)
                    }
                }
                .addOnFailureListener {
                    onResult(null)
                }
        } catch (e: Exception) {
            onResult(null)
        }
    }

    fun updateStore(
        context: Context,
        storeId: String,
        storeName: String,
        storeDescription: String,
        logoUri: Uri?,
        bannerUri: Uri?,
        currentLogoUrl: String,
        currentBannerUrl: String,
        onResult: (Boolean) -> Unit
    ) {
        try {
            var finalLogoUrl = currentLogoUrl
            var finalBannerUrl = currentBannerUrl
            var logoCompleted = logoUri == null
            var bannerCompleted = bannerUri == null
            var hasError = false

            fun checkAndUpdate() {
                if (hasError) return
                if (logoCompleted && bannerCompleted) {
                    val updates = mapOf(
                        "storeName" to storeName,
                        "storeDescription" to storeDescription,
                        "storeLogo" to finalLogoUrl,
                        "storeBanner" to finalBannerUrl
                    )

                    db.collection("coSellerStores")
                        .document(storeId)
                        .update(updates)
                        .addOnSuccessListener {
                            onResult(true)
                        }
                        .addOnFailureListener {
                            onResult(false)
                        }
                }
            }

            // Upload new logo if provided
            if (logoUri != null) {
                CloudinaryRepository.uploadImageToCloudinary(
                    context = context,
                    uri = logoUri,
                    preset = "logo_upload",
                    onSuccess = { url ->
                        finalLogoUrl = url
                        logoCompleted = true
                        checkAndUpdate()
                    },
                    onError = {
                        hasError = true
                        onResult(false)
                    }
                )
            }

            // Upload new banner if provided
            if (bannerUri != null) {
                CloudinaryRepository.uploadImageToCloudinary(
                    context = context,
                    uri = bannerUri,
                    preset = "banner_upload",
                    onSuccess = { url ->
                        finalBannerUrl = url
                        bannerCompleted = true
                        checkAndUpdate()
                    },
                    onError = {
                        hasError = true
                        onResult(false)
                    }
                )
            }

            // If no new images, update immediately
            if (logoUri == null && bannerUri == null) {
                checkAndUpdate()
            }
        } catch (e: Exception) {
            onResult(false)
        }
    }
}