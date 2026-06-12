package com.example.myapplication.model

import com.google.firebase.firestore.PropertyName

data class LearningCategory(
    @get:PropertyName("categoryId")
    @set:PropertyName("categoryId")
    var categoryId: String = "",
    
    @get:PropertyName("categoryName")
    @set:PropertyName("categoryName")
    var categoryName: String = "",
    
    @get:PropertyName("icon")
    @set:PropertyName("icon")
    var icon: String = "", // emoji or icon name
    
    @get:PropertyName("description")
    @set:PropertyName("description")
    var description: String = "",
    
    @get:PropertyName("order")
    @set:PropertyName("order")
    var order: Int = 0,
    
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,
    
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Any? = null
)

data class LearningTutorial(
    @get:PropertyName("tutorialId")
    @set:PropertyName("tutorialId")
    var tutorialId: String = "",
    
    @get:PropertyName("categoryId")
    @set:PropertyName("categoryId")
    var categoryId: String = "",
    
    @get:PropertyName("title")
    @set:PropertyName("title")
    var title: String = "",
    
    @get:PropertyName("description")
    @set:PropertyName("description")
    var description: String = "",
    
    @get:PropertyName("type")
    @set:PropertyName("type")
    var type: String = "", // "video", "article", "pdf"
    
    @get:PropertyName("url")
    @set:PropertyName("url")
    var url: String = "", // YouTube link, article link, or PDF URL
    
    @get:PropertyName("thumbnailUrl")
    @set:PropertyName("thumbnailUrl")
    var thumbnailUrl: String = "",
    
    @get:PropertyName("duration")
    @set:PropertyName("duration")
    var duration: String = "", // "5 min", "10 min", etc.
    
    @get:PropertyName("order")
    @set:PropertyName("order")
    var order: Int = 0,
    
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,
    
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Any? = null,
    
    @get:PropertyName("createdBy")
    @set:PropertyName("createdBy")
    var createdBy: String = "" // admin user ID
)
