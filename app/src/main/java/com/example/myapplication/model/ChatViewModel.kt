package com.example.myapplication.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _chats = MutableStateFlow<List<ChatModel>>(emptyList())
    val chats = _chats.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageModel>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _totalUnreadCount = MutableStateFlow(0)
    val totalUnreadCount = _totalUnreadCount.asStateFlow()

    private var chatsListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null

    // ── Load user ki saari chats ──────────────────────────────────
    fun loadUserChats() {
        val userId = auth.currentUser?.uid ?: return
        
        chatsListener?.remove()
        _isLoading.value = true

        chatsListener = db.collection("chats")
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snap, error ->
                _isLoading.value = false
                if (error != null) {
                    android.util.Log.e("ChatViewModel", "Error loading chats: ${error.message}")
                    return@addSnapshotListener
                }
                
                if (snap == null || snap.isEmpty) {
                    _chats.value = emptyList()
                    return@addSnapshotListener
                }
                
                try {
                    // Get all chats and sort them client-side
                    val chatsList = snap.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(ChatModel::class.java)?.apply {
                                chatId = doc.id
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ChatViewModel", "Error parsing chat: ${e.message}")
                            null
                        }
                    }.sortedByDescending { chat ->
                        chat.lastMessageTime?.toDate()?.time ?: 0L
                    }
                    
                    _chats.value = chatsList
                    
                    // Calculate total unread count
                    val unreadTotal = chatsList.sumOf { chat ->
                        (chat.unreadCount[userId] ?: 0L).toInt()
                    }
                    _totalUnreadCount.value = unreadTotal
                } catch (e: Exception) {
                    android.util.Log.e("ChatViewModel", "Error processing chats: ${e.message}")
                    _chats.value = emptyList()
                }
            }
    }

    // ── Specific chat ke messages load karo ───────────────────────
    fun loadMessages(chatId: String) {
        messagesListener?.remove()

        messagesListener = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    android.util.Log.e("ChatViewModel", "Error loading messages: ${error.message}")
                    return@addSnapshotListener
                }
                
                if (snap == null || snap.isEmpty) {
                    _messages.value = emptyList()
                    return@addSnapshotListener
                }
                
                try {
                    // Get all messages and sort them client-side
                    val messagesList = snap.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(MessageModel::class.java)?.apply {
                                messageId = doc.id
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ChatViewModel", "Error parsing message: ${e.message}")
                            null
                        }
                    }.sortedBy { message ->
                        message.timestamp?.toDate()?.time ?: 0L
                    }
                    
                    _messages.value = messagesList
                } catch (e: Exception) {
                    android.util.Log.e("ChatViewModel", "Error processing messages: ${e.message}")
                    _messages.value = emptyList()
                }
            }
    }

    // ── Naya chat create karo ya existing return karo ─────────────
    suspend fun getOrCreateChat(
        sellerId: String,
        productId: String,
        productName: String,
        productImage: String
    ): String {
        val buyerId = auth.currentUser?.uid ?: return ""

        try {
            // Check if chat already exists
            val existingChats = db.collection("chats")
                .whereArrayContains("participants", buyerId)
                .get()
                .await()

            val existingChat = existingChats.documents.firstOrNull { doc ->
                val participants = doc.get("participants") as? List<*>
                val pId = doc.getString("productId")
                participants?.contains(sellerId) == true && pId == productId
            }

            if (existingChat != null) {
                return existingChat.id
            }

            // Create new chat
            val chatRef = db.collection("chats").document()
            val chat = hashMapOf(
                "chatId" to chatRef.id,
                "participants" to listOf(buyerId, sellerId),
                "productId" to productId,
                "productName" to productName,
                "productImage" to productImage,
                "lastMessage" to "",
                "lastMessageTime" to FieldValue.serverTimestamp(),
                "lastMessageSenderId" to "",
                "unreadCount" to mapOf(buyerId to 0, sellerId to 0),
                "createdAt" to FieldValue.serverTimestamp()
            )
            chatRef.set(chat).await()
            return chatRef.id
        } catch (e: Exception) {
            return ""
        }
    }

    // ── Message send karo ──────────────────────────────────────────
    fun sendMessage(chatId: String, text: String, receiverId: String) {
        viewModelScope.launch {
            try {
                val senderId = auth.currentUser?.uid ?: return@launch
                val senderName = getUserName(senderId)

                // Message add karo
                val messageRef = db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document()

                val message = hashMapOf(
                    "messageId" to messageRef.id,
                    "senderId" to senderId,
                    "senderName" to senderName,
                    "text" to text,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "isRead" to false
                )
                messageRef.set(message).await()

                // Chat document update karo
                db.collection("chats").document(chatId).update(
                    mapOf(
                        "lastMessage" to text,
                        "lastMessageTime" to FieldValue.serverTimestamp(),
                        "lastMessageSenderId" to senderId,
                        "unreadCount.$receiverId" to FieldValue.increment(1)
                    )
                ).await()

            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    // ── Unread count reset karo ────────────────────────────────────
    fun markChatAsRead(chatId: String) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                db.collection("chats").document(chatId)
                    .update("unreadCount.$userId", 0)
                    .await()
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    // ── User name fetch karo ───────────────────────────────────────
    private suspend fun getUserName(userId: String): String {
        return try {
            val snap = db.collection("users").document(userId).get().await()
            snap.getString("name") ?: "User"
        } catch (e: Exception) {
            "User"
        }
    }
    
    // ── Get other participant info ─────────────────────────────────
    suspend fun getOtherParticipantName(chatId: String): String {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return "User"
            val chatDoc = db.collection("chats").document(chatId).get().await()
            val participants = chatDoc.get("participants") as? List<*> ?: return "User"
            val otherUserId = participants.firstOrNull { it != currentUserId } as? String ?: return "User"
            getUserName(otherUserId)
        } catch (e: Exception) {
            "User"
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatsListener?.remove()
        messagesListener?.remove()
    }
}
