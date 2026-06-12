# 🔧 Stripe Refund Tracking Fix

## Problem Fixed
Multiple orders sharing the same Stripe PaymentIntent could cause refund conflicts and double-refunds.

---

## ✅ Solution Implemented

### 1. **Refund Tracking Collection**
Created new Firestore collection: `stripe_refund_tracking`

**Document Structure:**
```javascript
{
  paymentIntentId: "pi_xxxxx",
  totalPaid: 800.0,           // Total amount paid via Stripe
  totalRefunded: 500.0,       // Amount already refunded
  refundedOrders: [           // List of refunded order IDs
    "order123",
    "order456"
  ],
  createdAt: Timestamp,
  lastRefundedAt: Timestamp
}
```

---

### 2. **Refund Flow with Tracking**

#### **Step 1: Initialize Tracking (CheckOutViewModel.kt)**
When Stripe payment succeeds:
```kotlin
// Create tracking document
db.collection("stripe_refund_tracking").document(paymentIntentId).set(
    mapOf(
        "paymentIntentId" to paymentIntentId,
        "totalPaid" to totalPKR.toDouble(),
        "totalRefunded" to 0.0,
        "refundedOrders" to emptyList<String>()
    )
)
```

#### **Step 2: Check Before Refund (OrdersViewModel.kt)**
Before processing refund:
```kotlin
db.runTransaction { transaction ->
    val trackingSnap = transaction.get(refundTrackingRef)
    
    // Check if order already refunded
    if (refundedOrders.contains(orderId)) {
        throw Exception("Order already refunded")
    }
    
    // Check if enough balance remaining
    val remainingRefundable = totalPaid - alreadyRefunded
    if (amountPKR > remainingRefundable) {
        throw Exception("Insufficient refundable amount")
    }
    
    // Update tracking
    transaction.update(refundTrackingRef, mapOf(
        "totalRefunded" to (alreadyRefunded + amountPKR),
        "refundedOrders" to updatedList
    ))
}
```

#### **Step 3: Process Stripe Refund**
Call Stripe API to process refund

#### **Step 4: Rollback if Failed**
If Stripe API fails, rollback the tracking:
```kotlin
private suspend fun rollbackRefundTracking(
    paymentIntentId: String,
    amountPKR: Double,
    orderId: String
)
```

---

## 🎯 What This Fixes

### ✅ **Prevents Double Refunds**
- Tracks which orders have been refunded
- Rejects refund if order already refunded

### ✅ **Prevents Over-Refunding**
- Tracks total refunded amount
- Rejects refund if exceeds total paid

### ✅ **Handles Multiple Orders**
Example:
```
Total Stripe Payment: Rs. 800
├─ Order 1 (Seller A): Rs. 500
└─ Order 2 (Seller B): Rs. 300

Refund Order 1: Rs. 500 ✅
  → totalRefunded: 500
  → remainingRefundable: 300

Refund Order 2: Rs. 300 ✅
  → totalRefunded: 800
  → remainingRefundable: 0

Try Refund Order 1 Again: ❌
  → Error: "Order already refunded"
```

### ✅ **Atomic Operations**
- Uses Firestore transactions
- Ensures consistency even with concurrent refunds

### ✅ **Rollback on Failure**
- If Stripe API fails, tracking is rolled back
- Prevents inconsistent state

---

## 📊 Refund Status by Payment Method

| Payment Method | Multiple Refunds | Tracking | Status |
|----------------|------------------|----------|--------|
| **Wallet** | ✅ Works | Individual | ✅ Fixed |
| **COD** | ✅ Works | Not needed | ✅ Fixed |
| **Stripe** | ✅ Works | Centralized | ✅ **FIXED** |

---

## 🔍 Debug Helper Function

Added helper to check refund status:
```kotlin
suspend fun getRefundStatus(paymentIntentId: String): Map<String, Any>
```

**Usage:**
```kotlin
val status = ordersViewModel.getRefundStatus("pi_xxxxx")
Log.d("Debug", "Total Paid: ${status["totalPaid"]}")
Log.d("Debug", "Total Refunded: ${status["totalRefunded"]}")
Log.d("Debug", "Remaining: ${status["remainingRefundable"]}")
Log.d("Debug", "Refunded Orders: ${status["refundedOrders"]}")
```

---

## 🧪 Testing Scenarios

### Test 1: Normal Multi-Order Refund
1. Create 2 orders with Stripe (Rs. 500 + Rs. 300)
2. Cancel Order 1 → Refund Rs. 500 ✅
3. Cancel Order 2 → Refund Rs. 300 ✅
4. Both show "REFUNDED" status ✅

### Test 2: Prevent Double Refund
1. Create 1 order with Stripe (Rs. 500)
2. Cancel order → Refund Rs. 500 ✅
3. Try to cancel same order again → ❌ Error: "Order already refunded"

### Test 3: Prevent Over-Refund
1. Create 2 orders with Stripe (Rs. 500 + Rs. 300)
2. Manually increase refund amount in code to Rs. 900
3. Try to refund → ❌ Error: "Insufficient refundable amount"

### Test 4: Concurrent Refunds
1. Create 3 orders with Stripe
2. Cancel all 3 at the same time
3. All should process correctly with proper tracking ✅

---

## 📝 Files Modified

1. **OrdersViewModel.kt**
   - Enhanced `processStripeRefund()` with tracking
   - Added `rollbackRefundTracking()`
   - Added `getRefundStatus()` helper

2. **CheckOutViewModel.kt**
   - Enhanced `saveStripeOrders()` to initialize tracking

---

## 🚀 Deployment Notes

### Firestore Security Rules
Add rules for the new collection:
```javascript
match /stripe_refund_tracking/{paymentIntentId} {
  // Only system can write (via Admin SDK or Cloud Functions)
  allow read: if request.auth != null;
  allow write: if false; // Only backend should write
}
```

### Migration
For existing Stripe orders without tracking:
- Tracking document will be created on first refund attempt
- System will fetch total from orders collection
- No manual migration needed ✅

---

## ✅ Summary

**Before Fix:**
- ❌ Could refund more than total paid
- ❌ Could refund same order multiple times
- ❌ No tracking of refunded amounts

**After Fix:**
- ✅ Tracks all refunds per PaymentIntent
- ✅ Prevents double refunds
- ✅ Prevents over-refunding
- ✅ Atomic operations with rollback
- ✅ Works with multiple concurrent refunds

**Result:** Stripe refunds are now bulletproof! 🎉
