package com.example.myapplication.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class ChatModel(
    @get:PropertyName("chatId")
    @set:PropertyName("chatId")
    var chatId: String = "",
    
    @get:PropertyName("participants")
    @set:PropertyName("participants")
    var participants: List<String> = emptyList(),
    
    @get:PropertyName("productId")
    @set:PropertyName("productId")
    var productId: String = "",
    
    @get:PropertyName("productName")
    @set:PropertyName("productName")
    var productName: String = "",
    
    @get:PropertyName("productImage")
    @set:PropertyName("productImage")
    var productImage: String = "",
    
    @get:PropertyName("lastMessage")
    @set:PropertyName("lastMessage")
    var lastMessage: String = "",
    
    @get:PropertyName("lastMessageTime")
    @set:PropertyName("lastMessageTime")
    var lastMessageTime: Timestamp? = null,
    
    @get:PropertyName("lastMessageSenderId")
    @set:PropertyName("lastMessageSenderId")
    var lastMessageSenderId: String = "",
    
    @get:PropertyName("unreadCount")
    @set:PropertyName("unreadCount")
    var unreadCount: Map<String, Long> = emptyMap(),
    
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Timestamp? = null
)

data class MessageModel(
    @get:PropertyName("messageId")
    @set:PropertyName("messageId")
    var messageId: String = "",
    
    @get:PropertyName("senderId")
    @set:PropertyName("senderId")
    var senderId: String = "",
    
    @get:PropertyName("senderName")
    @set:PropertyName("senderName")
    var senderName: String = "",
    
    @get:PropertyName("text")
    @set:PropertyName("text")
    var text: String = "",
    
    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timestamp: Timestamp? = null,
    
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false
)
