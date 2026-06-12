# Payment History Debugging Guide

## 🐛 ISSUE
Payment history not showing even though orders exist in database

## 🔧 FIXES APPLIED

### 1. Removed orderBy() Queries
**Problem:** Firestore composite indexes might be missing
**Solution:** Fetch all data without orderBy, sort in code

**Before:**
```kotlin
.orderBy("timestamp", Query.Direction.DESCENDING)
```

**After:**
```kotlin
// No orderBy - fetch all, sort in code
.sortedByDescending { it.timestamp?.seconds ?: 0L }
```

### 2. Show ALL Orders
**Problem:** Filtering by payment method might exclude orders
**Solution:** Show all orders regardless of payment method

**Before:**
```kotlin
if (paymentMethod in listOf("stripe", "cash_on_delivery", "wallet")) {
    allPayments.add(...)
}
```

**After:**
```kotlin
// Add all orders, handle unknown payment methods
allPayments.add(
    PaymentHistoryItem(
        paymentMethod = if (paymentMethod.isEmpty()) "unknown" else paymentMethod,
        ...
    )
)
```

### 3. Enhanced Debug Logging
```kotlin
android.util.Log.d("PaymentHistory", "Starting to fetch for user: $currentUserId")
android.util.Log.d("PaymentHistory", "Orders found: ${ordersSnap.size()}")
android.util.Log.d("PaymentHistory", "Order: ${doc.id}, method: $paymentMethod, amount: $amount, status: $status")
android.util.Log.d("PaymentHistory", "Wallet transactions found: ${walletSnap.size()}")
android.util.Log.d("PaymentHistory", "Total payments loaded: ${paymentHistory.size}")
```

### 4. Handle Unknown Payment Methods
Added support for orders without payment method field:
- Icon: 📦 Receipt
- Label: "📦 Order Payment"
- Color: Gray

---

## 🧪 DEBUGGING STEPS

### Step 1: Check Logcat
```
Filter: PaymentHistory
```

**Expected Logs:**
```
D/PaymentHistory: Starting to fetch for user: [userId]
D/PaymentHistory: Orders found: 5
D/PaymentHistory: Order: abc123, method: stripe, amount: 1000.0, status: completed
D/PaymentHistory: Order: def456, method: cash_on_delivery, amount: 500.0, status: pending
D/PaymentHistory: Wallet transactions found: 2
D/PaymentHistory: Total payments loaded: 7
```

**If you see:**
- `Orders found: 0` → No orders in database OR wrong buyerId
- `Orders found: 5` but `Total payments loaded: 0` → Check payment method filtering
- Error messages → Check Firestore rules or network

### Step 2: Verify User ID
```kotlin
// Add this log to check current user
android.util.Log.d("PaymentHistory", "Current User ID: $currentUserId")
```

Then check Firebase Console:
1. Go to Firestore
2. Open "orders" collection
3. Find your orders
4. Verify `buyerId` field matches the logged user ID

### Step 3: Check Firestore Data Structure

**Orders Collection:**
```
orders/
  ├── order123/
  │   ├── buyerId: "user123"
  │   ├── totalAmount: 1000
  │   ├── paymentMethod: "stripe"  ← Check this field exists
  │   ├── status: "completed"
  │   └── timestamp: [Timestamp]
```

**Common Issues:**
- ❌ `buyerId` field missing or wrong
- ❌ `paymentMethod` field missing
- ❌ `totalAmount` is string instead of number
- ❌ `timestamp` field missing

### Step 4: Test with Simple Query
Add this temporary code to test basic query:

```kotlin
LaunchedEffect(Unit) {
    db.collection("orders")
        .get()
        .addOnSuccessListener { snapshot ->
            android.util.Log.d("PaymentHistory", "Total orders in DB: ${snapshot.size()}")
            snapshot.documents.forEach { doc ->
                android.util.Log.d("PaymentHistory", "Order: ${doc.id}, buyerId: ${doc.getString("buyerId")}")
            }
        }
}
```

This will show ALL orders in database.

### Step 5: Check Firestore Rules
```javascript
match /orders/{orderId} {
  allow read: if request.auth != null && 
    (resource.data.buyerId == request.auth.uid || 
     resource.data.sellerId == request.auth.uid);
}
```

**Test:** Try changing to allow all reads temporarily:
```javascript
match /orders/{orderId} {
  allow read: if request.auth != null;  // Allow all authenticated users
}
```

If this works, the issue is with the rule condition.

---

## 🔍 COMMON ISSUES & SOLUTIONS

### Issue 1: "Orders found: 0"
**Possible Causes:**
1. No orders exist for this user
2. Wrong user ID
3. Firestore rules blocking read
4. Network issue

**Solution:**
```kotlin
// Add this to verify user ID
android.util.Log.d("PaymentHistory", "Fetching for buyerId: $currentUserId")

// Check Firebase Console
// Verify orders exist with matching buyerId
```

### Issue 2: Orders exist but not showing
**Possible Causes:**
1. Payment method filtering too strict
2. Missing required fields
3. Data type mismatch

**Solution:**
```kotlin
// Already fixed - now shows all orders regardless of payment method
// Check logs to see what data is being fetched
```

### Issue 3: "Permission denied" error
**Possible Causes:**
1. Firestore rules too restrictive
2. User not authenticated
3. buyerId doesn't match auth.uid

**Solution:**
```javascript
// Temporarily allow all reads for testing
match /orders/{orderId} {
  allow read: if request.auth != null;
}
```

### Issue 4: App crashes or freezes
**Possible Causes:**
1. Missing coroutines dependency
2. Network timeout
3. Large dataset

**Solution:**
```kotlin
// Already added proper error handling
// Check Gradle sync completed
// Check internet connection
```

---

## 📊 MANUAL TESTING

### Test 1: Create Test Order
```
1. Go to Firebase Console
2. Firestore Database
3. orders collection
4. Add document:
   {
     "buyerId": "[your-user-id]",
     "totalAmount": 1000,
     "paymentMethod": "stripe",
     "status": "completed",
     "timestamp": [current timestamp]
   }
5. Refresh app
6. Check Payment History
```

### Test 2: Check Existing Orders
```
1. Firebase Console → Firestore
2. orders collection
3. Find orders where buyerId = your user ID
4. Note the order IDs
5. Check if they appear in app
6. Check logcat for those order IDs
```

### Test 3: Test Each Payment Method
```
Create 3 test orders:
1. paymentMethod: "stripe"
2. paymentMethod: "cash_on_delivery"
3. paymentMethod: "wallet"

All should appear in Payment History
```

---

## 🎯 EXPECTED LOGCAT OUTPUT

### Successful Load:
```
D/PaymentHistory: Starting to fetch for user: abc123xyz
D/PaymentHistory: Orders found: 3
D/PaymentHistory: Order: order1, method: stripe, amount: 1000.0, status: completed
D/PaymentHistory: Order: order2, method: cash_on_delivery, amount: 500.0, status: pending
D/PaymentHistory: Order: order3, method: wallet, amount: 750.0, status: completed
D/PaymentHistory: Wallet transactions found: 1
D/PaymentHistory: Wallet: trans1, type: DEBIT, orderId: order3, amount: 750.0
D/PaymentHistory: Total payments loaded: 3
```

### No Orders Found:
```
D/PaymentHistory: Starting to fetch for user: abc123xyz
D/PaymentHistory: Orders found: 0
D/PaymentHistory: Wallet transactions found: 0
D/PaymentHistory: Total payments loaded: 0
```

### Error Case:
```
D/PaymentHistory: Starting to fetch for user: abc123xyz
E/PaymentHistory: Error fetching orders: PERMISSION_DENIED: Missing or insufficient permissions
D/PaymentHistory: Total payments loaded: 0
```

---

## 🚀 QUICK FIX CHECKLIST

- [ ] Gradle synced successfully
- [ ] App builds without errors
- [ ] User is logged in
- [ ] Check logcat for "PaymentHistory" logs
- [ ] Verify user ID in logs
- [ ] Check Firebase Console for orders
- [ ] Verify buyerId matches user ID
- [ ] Check Firestore rules allow read
- [ ] Test with simple query (all orders)
- [ ] Create test order manually
- [ ] Check internet connection
- [ ] Try on different device/emulator

---

## 📞 STILL NOT WORKING?

### Share These Details:
1. **Logcat output** (filter: PaymentHistory)
2. **User ID** from logs
3. **Sample order** from Firebase Console
4. **Firestore rules** for orders collection
5. **Error messages** if any

### Temporary Debug Code:
Add this to see ALL orders:

```kotlin
LaunchedEffect(Unit) {
    db.collection("orders")
        .get()
        .addOnSuccessListener { snapshot ->
            android.util.Log.d("DEBUG", "=== ALL ORDERS IN DATABASE ===")
            android.util.Log.d("DEBUG", "Total: ${snapshot.size()}")
            snapshot.documents.forEach { doc ->
                android.util.Log.d("DEBUG", "Order ID: ${doc.id}")
                android.util.Log.d("DEBUG", "  buyerId: ${doc.getString("buyerId")}")
                android.util.Log.d("DEBUG", "  amount: ${doc.getDouble("totalAmount")}")
                android.util.Log.d("DEBUG", "  method: ${doc.getString("paymentMethod")}")
                android.util.Log.d("DEBUG", "  status: ${doc.getString("status")}")
            }
        }
        .addOnFailureListener { e ->
            android.util.Log.e("DEBUG", "Error: ${e.message}", e)
        }
}
```

---

**Status:** Ready for debugging
**Next Step:** Run app and check logcat output
