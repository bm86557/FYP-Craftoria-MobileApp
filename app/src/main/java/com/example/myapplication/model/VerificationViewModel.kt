package com.example.myapplication.model

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.CloudinaryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class VerificationState(
    val status: String = "NOT_SUBMITTED", // NOT_SUBMITTED, PENDING, VERIFIED, REJECTED
    val selfieUrl: String = "",
    val rejectionReason: String = "",
    val submittedAt: Long? = null,
    val verifiedAt: Long? = null
)

class VerificationViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _verificationState = MutableStateFlow(VerificationState())
    val verificationState = _verificationState.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError = _uploadError.asStateFlow()

    init {
        loadVerificationStatus()
        // Also do an immediate refresh to ensure latest data
        refreshVerificationStatus()
    }

    private fun loadVerificationStatus() {
        val userId = auth.currentUser?.uid ?: return

        android.util.Log.d("VerificationVM", "Setting up listener for user: $userId")

        viewModelScope.launch {
            db.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("VerificationVM", "Listener error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        android.util.Log.w("VerificationVM", "User document does not exist")
                        return@addSnapshotListener
                    }

                    val status = snapshot.getString("verificationStatus") ?: "NOT_SUBMITTED"
                    val selfieUrl = snapshot.getString("verificationSelfieUrl") ?: ""
                    val rejectionReason = snapshot.getString("verificationRejectionReason") ?: ""
                    val submittedAt = snapshot.getLong("verificationSubmittedAt")
                    val verifiedAt = snapshot.getLong("verifiedAt")

                    android.util.Log.d("VerificationVM", "Status updated: $status")
                    android.util.Log.d("VerificationVM", "verifiedAt: $verifiedAt")
                    android.util.Log.d("VerificationVM", "All fields: ${snapshot.data}")

                    _verificationState.value = VerificationState(
                        status = status,
                        selfieUrl = selfieUrl,
                        rejectionReason = rejectionReason,
                        submittedAt = submittedAt,
                        verifiedAt = verifiedAt
                    )
                }
        }
    }

    /**
     * Manually refresh verification status from Firestore
     * Call this if real-time updates are not working
     */
    fun refreshVerificationStatus() {
        val userId = auth.currentUser?.uid ?: return

        android.util.Log.d("VerificationVM", "Manual refresh for user: $userId")

        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(userId).get().await()
                
                if (!snapshot.exists()) {
                    android.util.Log.w("VerificationVM", "User document does not exist")
                    return@launch
                }

                val status = snapshot.getString("verificationStatus") ?: "NOT_SUBMITTED"
                val selfieUrl = snapshot.getString("verificationSelfieUrl") ?: ""
                val rejectionReason = snapshot.getString("verificationRejectionReason") ?: ""
                val submittedAt = snapshot.getLong("verificationSubmittedAt")
                val verifiedAt = snapshot.getLong("verifiedAt")

                android.util.Log.d("VerificationVM", "Manual refresh - Status: $status")
                android.util.Log.d("VerificationVM", "Manual refresh - All fields: ${snapshot.data}")

                _verificationState.value = VerificationState(
                    status = status,
                    selfieUrl = selfieUrl,
                    rejectionReason = rejectionReason,
                    submittedAt = submittedAt,
                    verifiedAt = verifiedAt
                )
            } catch (e: Exception) {
                android.util.Log.e("VerificationVM", "Manual refresh error: ${e.message}")
            }
        }
    }

    fun submitVerification(
        context: Context,
        imageUri: Uri,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onError("User not logged in")
            return
        }

        viewModelScope.launch {
            try {
                _isUploading.value = true
                _uploadError.value = null

                // Upload image to Cloudinary using existing repository
                val imageUrl = uploadToCloudinary(context, imageUri)

                // Get user info
                val userDoc = db.collection("users").document(userId).get().await()
                val userName = userDoc.getString("name") ?: ""
                val userEmail = userDoc.getString("email") ?: ""

                // Create verification request
                val verificationRequest = hashMapOf(
                    "sellerId" to userId,
                    "sellerName" to userName,
                    "sellerEmail" to userEmail,
                    "selfieUrl" to imageUrl,
                    "status" to "PENDING",
                    "submittedAt" to FieldValue.serverTimestamp(),
                    "reviewedAt" to null,
                    "reviewedBy" to null,
                    "rejectionReason" to null
                )

                db.collection("verificationRequests")
                    .add(verificationRequest)
                    .await()

                // Update user profile
                db.collection("users").document(userId)
                    .update(
                        mapOf(
                            "verificationStatus" to "PENDING",
                            "verificationSelfieUrl" to imageUrl,
                            "verificationSubmittedAt" to System.currentTimeMillis()
                        )
                    )
                    .await()

                _isUploading.value = false
                onSuccess()

            } catch (e: Exception) {
                _isUploading.value = false
                _uploadError.value = e.message
                onError(e.message ?: "Upload failed")
            }
        }
    }

    private suspend fun uploadToCloudinary(context: Context, uri: Uri): String {
        return suspendCoroutine { continuation ->
            CloudinaryRepository.uploadImageToCloudinary(
                context = context,
                uri = uri,
                preset = "profile_upload", // Use existing preset
                onSuccess = { imageUrl ->
                    continuation.resume(imageUrl)
                },
                onError = { error ->
                    continuation.resumeWith(Result.failure(Exception(error)))
                }
            )
        }
    }
}
