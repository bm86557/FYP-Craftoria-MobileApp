package com.example.myapplication.model

import com.google.firebase.firestore.PropertyName

data class Report(
    @get:PropertyName("reportId")
    @set:PropertyName("reportId")
    var reportId: String = "",

    @get:PropertyName("reportType")
    @set:PropertyName("reportType")
    var reportType: String = "", // product, seller, order, technical

    @get:PropertyName("reportedBy")
    @set:PropertyName("reportedBy")
    var reportedBy: String = "",

    @get:PropertyName("reportedByName")
    @set:PropertyName("reportedByName")
    var reportedByName: String = "",

    @get:PropertyName("reportedByEmail")
    @set:PropertyName("reportedByEmail")
    var reportedByEmail: String = "",

    // Target details
    @get:PropertyName("targetType")
    @set:PropertyName("targetType")
    var targetType: String = "", // product, seller, order, user

    @get:PropertyName("targetId")
    @set:PropertyName("targetId")
    var targetId: String = "",

    @get:PropertyName("targetName")
    @set:PropertyName("targetName")
    var targetName: String = "",

    // Report details
    @get:PropertyName("category")
    @set:PropertyName("category")
    var category: String = "",

    @get:PropertyName("description")
    @set:PropertyName("description")
    var description: String = "",

    @get:PropertyName("evidence")
    @set:PropertyName("evidence")
    var evidence: List<String> = emptyList(),

    // Status
    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = "pending", // pending, under_review, resolved, dismissed

    @get:PropertyName("priority")
    @set:PropertyName("priority")
    var priority: String = "medium", // low, medium, high, urgent

    // Admin actions
    @get:PropertyName("reviewedBy")
    @set:PropertyName("reviewedBy")
    var reviewedBy: String = "",

    @get:PropertyName("reviewedAt")
    @set:PropertyName("reviewedAt")
    var reviewedAt: Any? = null,

    @get:PropertyName("adminNotes")
    @set:PropertyName("adminNotes")
    var adminNotes: String = "",

    @get:PropertyName("action")
    @set:PropertyName("action")
    var action: String = "",

    // Timestamps
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Any? = null,

    @get:PropertyName("updatedAt")
    @set:PropertyName("updatedAt")
    var updatedAt: Any? = null
)

data class Complaint(
    @get:PropertyName("complaintId")
    @set:PropertyName("complaintId")
    var complaintId: String = "",

    @get:PropertyName("complaintType")
    @set:PropertyName("complaintType")
    var complaintType: String = "", // seller, buyer, order, payment

    // Complainant
    @get:PropertyName("complainantId")
    @set:PropertyName("complainantId")
    var complainantId: String = "",

    @get:PropertyName("complainantName")
    @set:PropertyName("complainantName")
    var complainantName: String = "",

    @get:PropertyName("complainantEmail")
    @set:PropertyName("complainantEmail")
    var complainantEmail: String = "",

    // Against whom
    @get:PropertyName("againstType")
    @set:PropertyName("againstType")
    var againstType: String = "", // seller, buyer

    @get:PropertyName("againstId")
    @set:PropertyName("againstId")
    var againstId: String = "",

    @get:PropertyName("againstName")
    @set:PropertyName("againstName")
    var againstName: String = "",

    // Related order
    @get:PropertyName("orderId")
    @set:PropertyName("orderId")
    var orderId: String = "",

    @get:PropertyName("orderAmount")
    @set:PropertyName("orderAmount")
    var orderAmount: Double = 0.0,

    // Complaint details
    @get:PropertyName("category")
    @set:PropertyName("category")
    var category: String = "",

    @get:PropertyName("subject")
    @set:PropertyName("subject")
    var subject: String = "",

    @get:PropertyName("description")
    @set:PropertyName("description")
    var description: String = "",

    @get:PropertyName("evidence")
    @set:PropertyName("evidence")
    var evidence: List<String> = emptyList(),

    // Status
    @get:PropertyName("status")
    @set:PropertyName("status")
    var status: String = "pending", // pending, investigating, resolved, closed

    @get:PropertyName("priority")
    @set:PropertyName("priority")
    var priority: String = "medium",

    // Resolution
    @get:PropertyName("resolvedBy")
    @set:PropertyName("resolvedBy")
    var resolvedBy: String = "",

    @get:PropertyName("resolvedAt")
    @set:PropertyName("resolvedAt")
    var resolvedAt: Any? = null,

    @get:PropertyName("resolution")
    @set:PropertyName("resolution")
    var resolution: String = "",

    @get:PropertyName("actionTaken")
    @set:PropertyName("actionTaken")
    var actionTaken: String = "",

    // Timestamps
    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Any? = null,

    @get:PropertyName("updatedAt")
    @set:PropertyName("updatedAt")
    var updatedAt: Any? = null
)
