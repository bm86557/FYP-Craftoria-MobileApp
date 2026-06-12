package com.example.myapplication.model

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class SearchResult(
    val id: String,
    val type: String, // "product", "store", "seller"
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val data: Any? = null
)

class SearchViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _userRole = MutableStateFlow("")
    val userRole = _userRole.asStateFlow()

    init {
        loadUserRole()
    }

    private fun loadUserRole() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                _userRole.value = doc.getString("role") ?: "buyer"
            }
    }

    suspend fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        _isSearching.value = true
        val results = mutableListOf<SearchResult>()

        try {
            val searchQuery = query.trim().lowercase()
            val role = _userRole.value

            if (role == "buyer") {
                // Buyer: Search products, stores, and sellers
                searchProducts(searchQuery, results)
                searchStores(searchQuery, results)
                searchSellers(searchQuery, results)
            } else if (role == "seller") {
                // Seller: Search stores and sellers
                searchStores(searchQuery, results)
                searchSellers(searchQuery, results)
            }

            _searchResults.value = results
        } catch (e: Exception) {
            android.util.Log.e("SearchViewModel", "Search error: ${e.message}")
            _searchResults.value = emptyList()
        } finally {
            _isSearching.value = false
        }
    }

    private suspend fun searchProducts(query: String, results: MutableList<SearchResult>) {
        try {
            val snapshot = db.collection("data")
                .document("stock")
                .collection("products")
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                val product = doc.toObject(ProductModel::class.java)
                if (product != null) {
                    val title = product.title.lowercase()
                    val category = product.category.lowercase()
                    
                    if (title.contains(query) || category.contains(query)) {
                        results.add(
                            SearchResult(
                                id = doc.id,
                                type = "product",
                                title = product.title,
                                subtitle = "PKR ${product.price} • ${product.category}",
                                imageUrl = product.getImagesList().firstOrNull() ?: "",
                                data = product
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SearchViewModel", "Product search error: ${e.message}")
        }
    }

    private suspend fun searchStores(query: String, results: MutableList<SearchResult>) {
        try {
            val snapshot = db.collection("coSellerStores")
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                val store = doc.toObject(CoSellerStoreModel::class.java)
                if (store != null) {
                    val storeName = store.storeName.lowercase()
                    val description = store.storeDescription.lowercase()
                    
                    if (storeName.contains(query) || description.contains(query)) {
                        results.add(
                            SearchResult(
                                id = store.storeId,
                                type = "store",
                                title = store.storeName,
                                subtitle = if (store.storeDescription.isNotEmpty()) 
                                    store.storeDescription 
                                else 
                                    "Co-Seller Store",
                                imageUrl = store.storeLogo,
                                data = store
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SearchViewModel", "Store search error: ${e.message}")
        }
    }

    private suspend fun searchSellers(query: String, results: MutableList<SearchResult>) {
        try {
            val snapshot = db.collection("users")
                .whereEqualTo("role", "seller")
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                val name = doc.getString("name")?.lowercase() ?: ""
                val email = doc.getString("email")?.lowercase() ?: ""
                
                if (name.contains(query) || email.contains(query)) {
                    results.add(
                        SearchResult(
                            id = doc.id,
                            type = "seller",
                            title = doc.getString("name") ?: "Unknown Seller",
                            subtitle = doc.getString("email") ?: "",
                            imageUrl = doc.getString("profileImage") ?: "",
                            data = SellerProfile(
                                sellerId = doc.id,
                                name = doc.getString("name") ?: "",
                                email = doc.getString("email") ?: "",
                                profileImage = doc.getString("profileImage") ?: "",
                                role = "seller"
                            )
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SearchViewModel", "Seller search error: ${e.message}")
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }
}
