package com.example.myapplication.model

import com.google.firebase.firestore.PropertyName

data class ReportItem(
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = "",
    
    @get:PropertyName("reportType")
    @set:PropertyName("reportType")
    var reportType: String = "",
    
    @get:PropertyName("complaintType")
    @set:PropertyName("complaintType")
    var complaintType: String = "",
    
    @get:PropertyName("category")
    @set:PropertyName("category")
    var category: String = "",
    
    @get:PropertyName("description")
    @set:PropertyName("description")
    var description: String = "",
    
    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = "",
    
    @get:PropertyName("priority")
    @set:PropertyName("priority")
    var priority: String = "",
    
    @get:PropertyName("targetName")
    @set:PropertyName("targetName")
    var targetName: String = "",
    
    @get:PropertyName("subject")
    @set:PropertyName("subject")
    var subject: String = "",
    
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Any? = null,
    
    @get:PropertyName("adminResponse")
    @set:PropertyName("adminResponse")
    var adminResponse: String = "",
    
    @get:PropertyName("adminNotes")
    @set:PropertyName("adminNotes")
    var adminNotes: String = "",
    
    @get:PropertyName("resolution")
    @set:PropertyName("resolution")
    var resolution: String = "",
    
    @get:PropertyName("reviewedAt")
    @set:PropertyName("reviewedAt")
    var reviewedAt: Any? = null,
    
    @get:PropertyName("reportedBy")
    @set:PropertyName("reportedBy")
    var reportedBy: String = "",
    
    @get:PropertyName("reportedByName")
    @set:PropertyName("reportedByName")
    var reportedByName: String = "",
    
    @get:PropertyName("reportedByEmail")
    @set:PropertyName("reportedByEmail")
    var reportedByEmail: String = "",
    
    @get:PropertyName("complainantId")
    @set:PropertyName("complainantId")
    var complainantId: String = "",
    
    @get:PropertyName("complainantName")
    @set:PropertyName("complainantName")
    var complainantName: String = "",
    
    @get:PropertyName("complainantEmail")
    @set:PropertyName("complainantEmail")
    var complainantEmail: String = ""
)
