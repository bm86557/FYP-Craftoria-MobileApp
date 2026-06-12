# 🔄 Refund & Cancellation System

**Complete Implementation Guide**  
**Date:** April 30, 2026  
**Status:** ✅ Fully Implemented

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Features](#features)
3. [Architecture](#architecture)
4. [Refund Flow](#refund-flow)
5. [UI Components](#ui-components)
6. [Backend Integration](#backend-integration)
7. [Testing Guide](#testing-guide)

---

## 🎯 Overview

Complete order cancellation and refund system with:
- ✅ Buyer can cancel orders
- ✅ Seller can cancel orders
- ✅ Automatic Stripe refunds
- ✅ Automatic wallet refunds
- ✅ Refund tracking
- ✅ Cancellation reasons
- ✅ Real-time status updates

---

## ✨ Features

### **1. Buyer Cancellation** ✅
- Cancel orders before shipping
- Automatic refund processing
- Optional cancellation reason
- Real-time status updates

### **2. Seller Cancellation** ✅
- Cancel orders with reason (required)
- Automatic buyer refund
- Refund tracking
- Status updates

### **3. Stripe Refund Integration** ✅
- Automatic refund via backend
- Full refund support
- Partial refund support (future)
- 3-5 business days processing

### **4. Wallet Refund** ✅
- Instant wallet credit
- Transaction history
- Balance updates
- Refund notifications

### **5. COD Handling** ✅
- No refund needed
- Simple cancellation
- Status update only

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    CANCELLATION FLOW                                 │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────┐
│ User Action  │ (Buyer or Seller clicks "Cancel Order")
└──────┬───────┘
       │
       ↓
┌──────────────────────────────────────────────────────────────────────┐
│ OrdersViewModel.cancelOrder()                                        │
│                                                                       │
│ 1. Validate order status (can be cancelled?)                         │
│ 2. Check payment method                                              │
│ 3. Process refund if needed                                          │
│ 4. Update order status to CANCELLED                                  │
└──────┬────────────────────────────────────────────────────────────────┘
       │
       ├─────────────────┬─────────────────┬─────────────────┐
       │                 │                 │                 │
       ↓                 ↓                 ↓                 ↓
┌─────────────┐   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│   STRIPE    │   │   WALLET    │   │     COD     │   │  FIRESTORE  │
│   REFUND    │   │   REFUND    │   │  NO REFUND  │   │   UPDATE    │
└─────────────┘   └─────────────┘   └─────────────┘   └─────────────┘
       │                 │                 │                 │
       │                 │                 │                 │
       ↓                 ↓                 ↓                 ↓
┌──────────────────────────────────────────────────────────────────────┐
│                    REFUND COMPLETED                                   │
│                                                                       │
│ • Order status: CANCELLED                                             │
│ • Payment status: REFUNDED                                            │
│ • Refund amount recorded                                              │
│ • Cancellation reason saved                                           │
│ • Timestamps updated                                                  │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 💰 Refund Flow by Payment Method

### **1. Stripe Card Payment**

```
┌─────────────────────────────────────────────────────────────────────┐
│                    STRIPE REFUND FLOW                                │
└─────────────────────────────────────────────────────────────────────┘

User clicks "Cancel Order"
       ↓
OrdersViewModel.cancelOrder()
       ↓
Check: paymentMethod == "stripe"
       ↓
processStripeRefund()
       ├─ POST /refund-payment
       ├─ {
       │    paymentIntentId: "pi_xxx",
       │    amount: 1400,
       │    reason: "requested_by_customer",
       │    orderId: "ORD-123"
       │  }
       ↓
Stripe Backend
       ├─ Validate Payment Intent
       ├─ Create Refund
       ├─ Return success
       ↓
Update Firestore
       ├─ status: "CANCELLED"
       ├─ paymentStatus: "REFUNDED"
       ├─ refundedAmount: 1400
       ├─ cancellationReason: "..."
       ↓
✅ Refund Complete
   • Buyer receives refund in 3-5 business days
   • Refund appears in Stripe Dashboard
   • Order marked as CANCELLED
```

### **2. Wallet Payment**

```
┌─────────────────────────────────────────────────────────────────────┐
│                    WALLET REFUND FLOW                                │
└─────────────────────────────────────────────────────────────────────┘

User clicks "Cancel Order"
       ↓
OrdersViewModel.cancelOrder()
       ↓
Check: paymentMethod == "wallet"
       ↓
refundBuyerWallet()
       ├─ Get buyer's current balance
       ├─ Add refund amount
       ├─ Update wallet balance
       ├─ Create transaction record
       ↓
Update Firestore
       ├─ status: "CANCELLED"
       ├─ paymentStatus: "REFUNDED"
       ├─ refundedAmount: 1400
       ↓
✅ Refund Complete
   • Buyer's wallet credited instantly
   • Transaction recorded
   • Order marked as CANCELLED
```

### **3. Cash on Delivery**

```
┌─────────────────────────────────────────────────────────────────────┐
│                    COD CANCELLATION FLOW                             │
└─────────────────────────────────────────────────────────────────────┘

User clicks "Cancel Order"
       ↓
OrdersViewModel.cancelOrder()
       ↓
Check: paymentMethod == "cash_on_delivery"
       ↓
No refund needed (payment not made yet)
       ↓
Update Firestore
       ├─ status: "CANCELLED"
       ├─ cancellationReason: "..."
       ↓
✅ Cancellation Complete
   • No refund needed
   • Order marked as CANCELLED
```

---

## 🎨 UI Components

### **1. Buyer Order Card with Cancel Button**

```kotlin
@Composable
fun BuyerOrderCard(
    order: Order,
    viewModel: OrdersViewModel,
    isCancelling: Boolean
) {
    // Order details display
    
    // ✅ Cancel button (only for cancellable orders)
    if (order.status in listOf("PAYMENT_CONFIRMED", "PENDING", "CONFIRMED")) {
        OutlinedButton(
            onClick = { showCancelDialog = true },
            enabled = !isCancelling,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            if (isCancelling) {
                CircularProgressIndicator(...)
            }
            Text("Cancel Order")
        }
    }
    
    // ✅ Show refund info if cancelled
    if (order.status == "CANCELLED" && order.refundedAmount > 0) {
        Text("Refunded: Rs. ${order.refundedAmount}")
        Text("Reason: ${order.cancellationReason}")
    }
}
```

### **2. Cancel Confirmation Dialog**

```kotlin
AlertDialog(
    title = { Text("Cancel Order?") },
    text = {
        Column {
            Text("Are you sure you want to cancel this order?")
            Text("Order #${order.orderId}")
            Text("Amount: Rs. ${order.totalAmountPKR}")
            
            // Show refund info based on payment method
            when (order.paymentMethod) {
                "wallet" -> Text("💰 Refund will be credited to your wallet")
                "stripe" -> Text("💳 Refund will be processed to your card (3-5 days)")
                "cash_on_delivery" -> Text("💵 No refund needed")
            }
            
            // Reason input
            OutlinedTextField(
                value = cancelReason,
                onValueChange = { cancelReason = it },
                label = { Text("Reason (optional)") }
            )
        }
    },
    confirmButton = {
        Button(
            onClick = {
                viewModel.cancelOrder(
                    orderId = order.orderId,
                    reason = cancelReason,
                    onSuccess = { /* Show success */ },
                    onError = { /* Show error */ }
                )
            }
        ) {
            Text("Yes, Cancel Order")
        }
    },
    dismissButton = {
        TextButton(onClick = { /* Close dialog */ }) {
            Text("Keep Order")
        }
    }
)
```

### **3. Seller Order Card with Cancel Button**

```kotlin
@Composable
fun SellerOrderCard(
    order: Order,
    onConfirm: () -> Unit,
    onComplete: () -> Unit
) {
    // Order details display
    
    Row {
        when (order.status) {
            "PAYMENT_CONFIRMED", "PENDING" -> {
                Button(onClick = onConfirm) { Text("Confirm ✅") }
                OutlinedButton(
                    onClick = { showCancelDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancel")
                }
            }
            "CONFIRMED" -> {
                Button(onClick = onComplete) { Text("Complete 🎉") }
                OutlinedButton(onClick = { showCancelDialog = true }) {
                    Text("Cancel")
                }
            }
        }
    }
    
    // Show refund info if cancelled
    if (order.status == "CANCELLED" && order.refundedAmount > 0) {
        Text("Refunded to buyer: Rs. ${order.refundedAmount}")
        Text("Reason: ${order.cancellationReason}")
    }
}
```

---

## 🖥️ Backend Integration

### **Stripe Backend Endpoint**

**Already Implemented:** ✅

```javascript
// POST /refund-payment
app.post('/refund-payment', async (req, res) => {
  try {
    const { 
      paymentIntentId,
      amount,
      reason,
      orderId 
    } = req.body;
    
    // Validate input
    if (!paymentIntentId) {
      return res.status(400).json({ 
        error: 'Payment Intent ID is required' 
      });
    }
    
    // Create refund
    const refundData = {
      payment_intent: paymentIntentId,
      reason: reason || 'requested_by_customer',
      metadata: {
        orderId: orderId || '',
        refundedAt: new Date().toISOString()
      }
    };
    
    // If amount specified, do partial refund
    if (amount && amount > 0) {
      const amountUSDCents = Math.round((amount / PKR_TO_USD) * 100);
      refundData.amount = amountUSDCents;
    }
    
    const refund = await stripe.refunds.create(refundData);
    
    res.json({
      success: true,
      refundId: refund.id,
      status: refund.status,
      amountUSD: (refund.amount / 100).toFixed(2),
      currency: refund.currency
    });
    
  } catch (error) {
    res.status(400).json({ error: error.message });
  }
});
```

### **Android Integration**

```kotlin
// OrdersViewModel.kt
private suspend fun processStripeRefund(
    paymentIntentId: String,
    amountPKR: Double,
    orderId: String,
    reason: String
): Boolean {
    return try {
        val client = OkHttpClient()
        val json = JSONObject().apply {
            put("paymentIntentId", paymentIntentId)
            put("amount", amountPKR.toInt())
            put("reason", reason)
            put("orderId", orderId)
        }.toString()
        
        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL/refund-payment")
            .post(body)
            .build()
        
        val response = client.newCall(request).execute()
        val responseString = response.body?.string() ?: ""
        
        if (response.isSuccessful) {
            val responseJson = JSONObject(responseString)
            responseJson.optBoolean("success", false)
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }
}
```

---

## 📊 Database Structure

### **Order Document (Updated)**

```firestore
orders/{orderId}
├─ orderId: string
├─ buyerId: string
├─ sellerId: string
├─ items: array
├─ address: map
├─ totalAmountPKR: number
├─ platformCommission: number
├─ sellerAmount: number
├─ paymentMethod: string
├─ stripePaymentIntentId: string
├─ status: string (CANCELLED when refunded)
├─ paymentStatus: string (REFUNDED when refunded)
│
├─ ✅ NEW: Refund fields
├─ refundedAmount: number
├─ refundReason: string
├─ cancellationReason: string
├─ refundedAt: timestamp
├─ cancelledAt: timestamp
│
└─ lastUpdatedAt: timestamp
```

### **Wallet Transaction (Refund)**

```firestore
wallet_transactions/{txnId}
├─ userId: string (buyer ID)
├─ amount: number (refund amount)
├─ type: "REFUND"
├─ orderId: string
├─ description: "Refund for Order #ORD-123"
└─ timestamp: timestamp
```

---

## 🧪 Testing Guide

### **Test Scenario 1: Buyer Cancels Wallet Order**

```
1. Create order with wallet payment (Rs. 1000)
2. Buyer wallet deducted: Rs. 1000
3. Order status: PAYMENT_CONFIRMED
4. Buyer clicks "Cancel Order"
5. Enter reason: "Changed my mind"
6. Confirm cancellation
7. Verify:
   ✅ Order status: CANCELLED
   ✅ Payment status: REFUNDED
   ✅ Buyer wallet credited: Rs. 1000
   ✅ Transaction recorded
   ✅ Cancellation reason saved
```

### **Test Scenario 2: Buyer Cancels Stripe Order**

```
1. Create order with Stripe payment (Rs. 1400)
2. Card charged: $5.04 USD
3. Order status: PAYMENT_CONFIRMED
4. Buyer clicks "Cancel Order"
5. Enter reason: "Wrong address"
6. Confirm cancellation
7. Verify:
   ✅ Order status: CANCELLED
   ✅ Payment status: REFUNDED
   ✅ Stripe refund created
   ✅ Refund visible in Stripe Dashboard
   ✅ Buyer receives refund in 3-5 days
   ✅ Cancellation reason saved
```

### **Test Scenario 3: Seller Cancels Order**

```
1. Order status: CONFIRMED
2. Payment method: Wallet (Rs. 800)
3. Seller clicks "Cancel"
4. Enter reason: "Out of stock"
5. Confirm cancellation
6. Verify:
   ✅ Order status: CANCELLED
   ✅ Payment status: REFUNDED
   ✅ Buyer wallet credited: Rs. 800
   ✅ Seller sees refund info
   ✅ Cancellation reason saved
```

### **Test Scenario 4: Cancel COD Order**

```
1. Create order with COD
2. Order status: PENDING
3. Buyer clicks "Cancel Order"
4. Enter reason: "Not needed"
5. Confirm cancellation
6. Verify:
   ✅ Order status: CANCELLED
   ✅ No refund processed (COD)
   ✅ Cancellation reason saved
   ✅ No wallet transaction
```

### **Test Scenario 5: Cannot Cancel Completed Order**

```
1. Order status: COMPLETED
2. Buyer clicks "Cancel Order"
3. Verify:
   ✅ Cancel button not visible
   OR
   ✅ Error: "Cannot cancel completed order"
```

---

## 🔒 Cancellation Rules

### **Orders That CAN Be Cancelled:**

✅ **PAYMENT_CONFIRMED** - Payment received, not yet confirmed by seller  
✅ **PENDING** - Awaiting payment (COD)  
✅ **CONFIRMED** - Confirmed by seller, not yet shipped  

### **Orders That CANNOT Be Cancelled:**

❌ **PROCESSING** - Being prepared (contact seller)  
❌ **SHIPPED** - Already shipped (contact support)  
❌ **DELIVERED** - Already delivered (return process)  
❌ **COMPLETED** - Already completed (no refund)  
❌ **CANCELLED** - Already cancelled  

---

## 💡 Key Features

### **1. Automatic Refund Processing**

```kotlin
fun cancelOrder(orderId: String, reason: String, ...) {
    // Automatically detects payment method
    when (order.paymentMethod) {
        "stripe" -> processStripeRefund(...)
        "wallet" -> refundBuyerWallet(...)
        "cash_on_delivery" -> // No refund needed
    }
}
```

### **2. Real-time Status Updates**

```kotlin
// Firestore listener automatically updates UI
db.collection("orders")
    .whereEqualTo("buyerId", userId)
    .addSnapshotListener { snap, error ->
        // UI updates automatically when order cancelled
    }
```

### **3. Refund Tracking**

```kotlin
// Order document stores complete refund info
order.refundedAmount = 1400.0
order.refundReason = "requested_by_customer"
order.cancellationReason = "Changed my mind"
order.refundedAt = Timestamp.now()
```

### **4. Error Handling**

```kotlin
viewModel.cancelOrder(
    orderId = orderId,
    reason = reason,
    onSuccess = {
        // Show success message
        showSuccessMessage = true
    },
    onError = { error ->
        // Show error message
        errorMessage = error
    }
)
```

---

## 📈 Benefits

### **For Buyers:**

✅ Easy cancellation process  
✅ Automatic refunds  
✅ Instant wallet refunds  
✅ Clear refund status  
✅ Cancellation history  

### **For Sellers:**

✅ Can cancel problematic orders  
✅ Automatic buyer refund  
✅ Refund tracking  
✅ Cancellation reasons recorded  
✅ No manual refund processing  

### **For Platform:**

✅ Automated refund system  
✅ Stripe integration  
✅ Wallet integration  
✅ Complete audit trail  
✅ Reduced support tickets  

---

## 🚀 Summary

### **What's Implemented:**

✅ **Buyer Cancellation UI** - Cancel button, dialog, reason input  
✅ **Seller Cancellation UI** - Cancel button, dialog, reason input  
✅ **Stripe Refund Integration** - Automatic refund via backend  
✅ **Wallet Refund** - Instant wallet credit  
✅ **COD Handling** - Simple cancellation without refund  
✅ **Refund Tracking** - Complete refund information  
✅ **Status Updates** - Real-time order status changes  
✅ **Error Handling** - Proper error messages  
✅ **Loading States** - Loading indicators during cancellation  

### **Refund Methods:**

✅ **Stripe Card** - 3-5 business days  
✅ **Wallet** - Instant  
✅ **COD** - No refund needed  

### **Cancellation Rules:**

✅ Can cancel: PAYMENT_CONFIRMED, PENDING, CONFIRMED  
❌ Cannot cancel: PROCESSING, SHIPPED, DELIVERED, COMPLETED, CANCELLED  

---

**🎉 Complete refund and cancellation system ready for use!**

**Last Updated:** April 30, 2026  
**Version:** 2.0.0  
**Status:** Production Ready
