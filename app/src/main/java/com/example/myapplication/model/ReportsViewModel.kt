package com.example.myapplication.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ReportsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting = _isSubmitting.asStateFlow()

    private val _submitError = MutableStateFlow<String?>(null)
    val submitError = _submitError.asStateFlow()

    // ✅ Submit Product Report
    fun submitProductReport(
        productId: String,
        productName: String,
        category: String,
        description: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isSubmitting.value = true
                _submitError.value = null

                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                val userDoc = db.collection("users").document(userId).get().await()

                val reportRef = db.collection("reports").document()
                val report = hashMapOf(
                    "reportId" to reportRef.id,
                    "reportType" to "product",
                    "reportedBy" to userId,
                    "reportedByName" to (userDoc.getString("name") ?: "Unknown User"),
                    "reportedByEmail" to (userDoc.getString("email") ?: ""),

                    "targetType" to "product",
                    "targetId" to productId,
                    "targetName" to productName,

                    "category" to category,
                    "description" to description,
                    "evidence" to emptyList<String>(),

                    "status" to "pending",
                    "priority" to determinePriority(category),

                    "reviewedBy" to "",
                    "reviewedAt" to null,
                    "adminNotes" to "",
                    "action" to "",

                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                reportRef.set(report).await()

                android.util.Log.d("ReportsViewModel", "✅ Product report submitted: ${reportRef.id}")

                _isSubmitting.value = false
                onSuccess()

            } catch (e: Exception) {
                android.util.Log.e("ReportsViewModel", "❌ Error: ${e.message}", e)
                _isSubmitting.value = false
                _submitError.value = e.message
                onError(e.message ?: "Failed to submit report")
            }
        }
    }

    // ✅ Submit Seller Complaint
    fun submitSellerComplaint(
        sellerId: String,
        sellerName: String,
        orderId: String = "",
        orderAmount: Double = 0.0,
        category: String,
        subject: String,
        description: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isSubmitting.value = true
                _submitError.value = null

                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                val userDoc = db.collection("users").document(userId).get().await()

                val complaintRef = db.collection("complaints").document()
                val complaint = hashMapOf(
                    "complaintId" to complaintRef.id,
                    "complaintType" to "seller",

                    "complainantId" to userId,
                    "complainantName" to (userDoc.getString("name") ?: "Unknown User"),
                    "complainantEmail" to (userDoc.getString("email") ?: ""),

                    "againstType" to "seller",
                    "againstId" to sellerId,
                    "againstName" to sellerName,

                    "orderId" to orderId,
                    "orderAmount" to orderAmount,

                    "category" to category,
                    "subject" to subject,
                    "description" to description,
                    "evidence" to emptyList<String>(),

                    "status" to "pending",
                    "priority" to determinePriority(category),

                    "resolvedBy" to "",
                    "resolvedAt" to null,
                    "resolution" to "",
                    "actionTaken" to "",

                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                complaintRef.set(complaint).await()

                android.util.Log.d("ReportsViewModel", "✅ Seller complaint submitted: ${complaintRef.id}")

                _isSubmitting.value = false
                onSuccess()

            } catch (e: Exception) {
                android.util.Log.e("ReportsViewModel", "❌ Error: ${e.message}", e)
                _isSubmitting.value = false
                _submitError.value = e.message
                onError(e.message ?: "Failed to submit complaint")
            }
        }
    }

    // ✅ Submit Order Issue
    fun submitOrderIssue(
        orderId: String,
        sellerId: String,
        sellerName: String,
        orderAmount: Double,
        category: String,
        description: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isSubmitting.value = true
                _submitError.value = null

                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                val userDoc = db.collection("users").document(userId).get().await()

                val complaintRef = db.collection("complaints").document()
                val complaint = hashMapOf(
                    "complaintId" to complaintRef.id,
                    "complaintType" to "order",

                    "complainantId" to userId,
                    "complainantName" to (userDoc.getString("name") ?: "Unknown User"),
                    "complainantEmail" to (userDoc.getString("email") ?: ""),

                    "againstType" to "seller",
                    "againstId" to sellerId,
                    "againstName" to sellerName,

                    "orderId" to orderId,
                    "orderAmount" to orderAmount,

                    "category" to category,
                    "subject" to "Order Issue - #${orderId.take(8).uppercase()}",
                    "description" to description,
                    "evidence" to emptyList<String>(),

                    "status" to "pending",
                    "priority" to determinePriority(category),

                    "resolvedBy" to "",
                    "resolvedAt" to null,
                    "resolution" to "",
                    "actionTaken" to "",

                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                complaintRef.set(complaint).await()

                android.util.Log.d("ReportsViewModel", "✅ Order issue submitted: ${complaintRef.id}")

                _isSubmitting.value = false
                onSuccess()

            } catch (e: Exception) {
                android.util.Log.e("ReportsViewModel", "❌ Error: ${e.message}", e)
                _isSubmitting.value = false
                _submitError.value = e.message
                onError(e.message ?: "Failed to submit issue")
            }
        }
    }

    // ✅ Submit Technical Issue
    fun submitTechnicalIssue(
        category: String,
        description: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        subject: String
    ) {
        viewModelScope.launch {
            try {
                _isSubmitting.value = true
                _submitError.value = null

                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                val userDoc = db.collection("users").document(userId).get().await()

                val reportRef = db.collection("reports").document()
                val report = hashMapOf(
                    "reportId" to reportRef.id,
                    "reportType" to "technical",
                    "reportedBy" to userId,
                    "reportedByName" to (userDoc.getString("name") ?: "Unknown User"),
                    "reportedByEmail" to (userDoc.getString("email") ?: ""),

                    "targetType" to "app",
                    "targetId" to "technical_issue",
                    "targetName" to "Technical Issue",

                    "category" to category,
                    "description" to description,
                    "evidence" to emptyList<String>(),

                    "status" to "pending",
                    "priority" to "medium",

                    "reviewedBy" to "",
                    "reviewedAt" to null,
                    "adminNotes" to "",
                    "action" to "",

                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                reportRef.set(report).await()

                android.util.Log.d("ReportsViewModel", "✅ Technical issue submitted: ${reportRef.id}")

                _isSubmitting.value = false
                onSuccess()

            } catch (e: Exception) {
                android.util.Log.e("ReportsViewModel", "❌ Error: ${e.message}", e)
                _isSubmitting.value = false
                _submitError.value = e.message
                onError(e.message ?: "Failed to submit issue")
            }
        }
    }

    // Helper function to determine priority
    private fun determinePriority(category: String): String {
        return when (category.lowercase()) {
            "fraud", "scam", "fake_product", "prohibited_item" -> "high"
            "misleading", "poor_service", "delayed_delivery" -> "medium"
            else -> "low"
        }
    }

    fun resetError() {
        _submitError.value = null
    }
}
