package com.example.myapplication.model



import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class WalletViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _balance = MutableStateFlow(0.0)
    val balance = _balance.asStateFlow()

    private val _userRole = MutableStateFlow("")
    val userRole = _userRole.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName = _userName.asStateFlow()

    private val _transactions = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val transactions = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    // Listener references — cleanup ke liye
    private var walletListener: ListenerRegistration? = null
    private var txnListener: ListenerRegistration? = null

    // ── Real-time wallet balance + role listener ───────────────────
    fun loadWallet() {
        val userId = auth.currentUser?.uid ?: return

        walletListener?.remove()
        _isLoading.value = true

        walletListener = db.collection("users")
            .document(userId)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null) {
                    _isLoading.value = false
                    return@addSnapshotListener
                }
                _balance.value = snap.getDouble("walletBalance") ?: 0.0
                _userRole.value = snap.getString("role") ?: ""
                _userName.value = snap.getString("name") ?: ""
                _isLoading.value = false
            }
    }

    // ── Real-time transactions listener ───────────────────────────
    fun loadTransactions() {
        val userId = auth.currentUser?.uid ?: return

        txnListener?.remove()

        txnListener = db.collection("wallet_transactions")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50) // Last 50 transactions
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    // Error log karo but empty list mat karo
                    android.util.Log.e("WalletViewModel", "Transaction load error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snap == null || snap.isEmpty) {
                    _transactions.value = emptyList()
                    return@addSnapshotListener
                }
                val txnList = snap.documents.mapNotNull { doc ->
                    try {
                        doc.data?.toMutableMap()?.apply {
                            put("id", doc.id) // Document ID bhi add karo
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("WalletViewModel", "Parse error: ${e.message}")
                        null
                    }
                }
                _transactions.value = txnList
            }
    }

    // ── ViewModel destroy hone par listeners hata do ──────────────
    override fun onCleared() {
        super.onCleared()
        walletListener?.remove()
        txnListener?.remove()
    }
}