package com.example.myapplication.model

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    
    // ✅ NEW: Verification status state
    private val _verificationStatus = kotlinx.coroutines.flow.MutableStateFlow("NOT_SUBMITTED")
    val verificationStatus = _verificationStatus.asStateFlow()
    
    private val _isVerifiedSeller = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isVerifiedSeller = _isVerifiedSeller.asStateFlow()
    
    init {
        // Load verification status on init
        loadVerificationStatus()
    }
    
    // ✅ NEW: Load verification status
    private fun loadVerificationStatus() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    _verificationStatus.value = "NOT_SUBMITTED"
                    _isVerifiedSeller.value = false
                    return@addSnapshotListener
                }
                
                val status = snapshot.getString("verificationStatus") ?: "NOT_SUBMITTED"
                val isVerified = status == "VERIFIED"
                
                _verificationStatus.value = status
                _isVerifiedSeller.value = isVerified
                
                android.util.Log.d("AuthViewModel", "Verification Status: $status, IsVerified: $isVerified")
            }
    }
    
    // ✅ NEW: Manual refresh
    fun refreshVerificationStatus() {
        loadVerificationStatus()
    }

    fun login(email: String,password: String, onResult:(Boolean, String?, String?)-> Unit){
        auth.signInWithEmailAndPassword(email,password)
            .addOnCompleteListener {
                if(it.isSuccessful){
                    val uid = auth.currentUser?.uid
                    Firebase.firestore.collection("users").
                        document(uid!!).get().addOnCompleteListener {doc->
                            if (doc.isSuccessful){
                                val role = doc.result.getString("role")
                                onResult(true,null,role)
                            }

                    }


                }
                else{
                    onResult(false,"Something went Wrong",null)
                }
            }

    }

    fun Signup(email:String, name: String, password:String, role: String, onResult:(Boolean, String?)-> Unit){
        auth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener {
                if (it.isSuccessful){
                    var userid = it.result?.user?.uid

                   val usermodel = UserModel(name,email,userid!!,role=role, walletBalance = if (role == "seller")3000.0 else 4000.0)
                    firestore.collection("users").document(userid)
                        .set(usermodel)
                        .addOnCompleteListener { dbTask->
                            if (dbTask.isSuccessful){
                                onResult(true,null)
                            }
                            else{
                                onResult(false,"Something went Wrong")
                            }
                        }
                }
                else{
                    onResult(false,it.exception?.localizedMessage)
                }
            }//it is asynchronous func so we should know it is created or not

    }

    fun uploadProfileImage(context: Context, uri: Uri, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        CloudinaryRepository.uploadImageToCloudinary(
            context = context,
            uri = uri,
            preset = "profile_upload",
            onSuccess = { imageUrl ->
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    Firebase.firestore.collection("users").document(uid)
                        .update("profileImage", imageUrl)
                        .addOnSuccessListener {
                            onResult(true, null)
                        }
                        .addOnFailureListener {
                            onResult(false, "Failed to update profile")
                        }
                } else {
                    onResult(false, "User not logged in")
                }
            },
            onError = { error ->
                onResult(false, error)
            }
        )
    }
    fun removeProfileImage() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        Firebase.firestore.collection("users")
            .document(uid!!)
            .update("profileImage", "")
    }
    fun addProduct(
        title : String,
        description : String,
        price : String,
        actualPrice : String,
        category: String,
        minDealPrice : String,
        images: List<String> = emptyList(),
        otherDetails: Map<String, String> = emptyMap(),
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val sellerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // ✅ NEW: Check verification status first
        if (_verificationStatus.value != "VERIFIED") {
            onResult(false, "You must be a verified seller to add products")
            return
        }

        // Get seller name first
        Firebase.firestore.collection("users").document(sellerId).get()
            .addOnSuccessListener { userDoc ->
                val sellerName = userDoc.getString("name") ?: ""
                
                val docRef = Firebase.firestore.collection("data").document("stock")
                    .collection("products").document()
                val product = ProductModel(
                    id = docRef.id,
                    title = title,
                    sellerId = sellerId,
                    sellerName = sellerName,
                    description = description,
                    price = price,
                    actualPrice = actualPrice,
                    category = category,
                    minDealPrice = minDealPrice.toIntOrNull() ?: 0,
                    images = images,
                    otherDetails = otherDetails,
                    isCoStoreProduct = false,
                    coStoreId = "",
                    coStoreName = "",
                    status = "pending",
                    rejectionReason = null,
                    reviewedBy = null,
                    createdAt = com.google.firebase.Timestamp.now()
                )
                docRef.set(product)
                    .addOnSuccessListener {
                        onResult(true, null)
                    }
                    .addOnFailureListener { e ->
                        onResult(false, e.message)
                    }
            }
            .addOnFailureListener { e ->
                onResult(false, e.message)
            }
    }


}