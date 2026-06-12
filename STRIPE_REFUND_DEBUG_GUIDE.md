# Stripe Refund Debugging Guide

## Problem
Stripe refund fail ho raha tha jab order cancel karte the.

## Changes Made

### 1. Enhanced Error Logging in `OrdersViewModel.kt`

#### Added Detailed Logs:
- ✅ Payment Intent ID validation (empty check, format check)
- ✅ Payment Intent ID length and format logging
- ✅ Network error categorization (Timeout, Connection Refused, Unknown Host)
- ✅ Backend response parsing with detailed error messages
- ✅ Request/Response JSON logging
- ✅ Increased timeout from 30s to 45s

#### Added Backend Connection Test:
```kotlin
fun testBackendConnection(onResult: (Boolean, String) -> Unit)
```
Yeh function backend server ko test karne ke liye hai.

### 2. Improved Payment Intent ID Validation

**Before:**
```kotlin
if (order.stripePaymentIntentId.isNotEmpty()) {
    // process refund
}
```

**After:**
```kotlin
val paymentIntentId = order.stripePaymentIntentId.trim()

if (paymentIntentId.isEmpty()) {
    // Detailed error with support message
    return
}

if (!paymentIntentId.startsWith("pi_")) {
    // Warning log for incorrect format
}
```

### 3. Better Error Messages

**User-Friendly Errors:**
- Backend not reachable → "Backend server is not responding"
- Timeout → "Network Timeout! Backend might be down"
- Invalid Payment Intent → "Payment information missing. Contact support"
- Refund failed → Shows detailed checklist (Backend running? Internet? Valid Payment Intent?)

## How to Test

### Step 1: Check Backend Server
```bash
# Backend URL in app:
https://sandbox-backend-production.up.railway.app

# Test manually:
curl https://sandbox-backend-production.up.railway.app
```

**Expected Response:**
```json
{
  "status": "Server is running",
  "version": "2.0.0",
  "endpoints": {
    "createPayment": "POST /create-payment-intent",
    "refund": "POST /refund-payment",
    ...
  }
}
```

### Step 2: Install Updated APK
```bash
# APK location:
app/build/outputs/apk/debug/app-debug.apk

# Install:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Step 3: Test Refund Flow

1. **Create a Stripe Order:**
   - Add product to cart
   - Checkout with Stripe payment
   - Complete payment

2. **Cancel Order:**
   - Go to Orders page
   - Open order detail
   - Click "Cancel Order & Request Refund"
   - Enter reason
   - Confirm

3. **Check Logs:**
```bash
# Filter OrdersViewModel logs:
adb logcat -s OrdersViewModel

# Look for these logs:
# ✅ Success indicators:
#    - "✅ Payment Intent ID found: pi_xxxxx"
#    - "🔄 Processing Stripe refund..."
#    - "✅ Refund successful!"
#    - "Refund ID: re_xxxxx"

# ❌ Error indicators:
#    - "❌ Payment Intent ID is EMPTY!"
#    - "❌ Network Timeout!"
#    - "❌ Cannot reach backend server!"
#    - "❌ Refund API failed"
```

## Common Issues & Solutions

### Issue 1: Payment Intent ID Empty
**Log:**
```
❌ Payment Intent ID is EMPTY!
⚠️ Cannot process Stripe refund without Payment Intent ID
```

**Cause:** Order mein Payment Intent ID save nahi hui thi payment ke time.

**Solution:**
- Check `CheckOutViewModel.kt` - payment success ke baad Payment Intent ID save ho rahi hai?
- Firestore mein order document check karo - `stripePaymentIntentId` field hai?

### Issue 2: Backend Not Reachable
**Log:**
```
❌ Cannot reach backend server!
Host: sandbox-backend-production.up.railway.app
```

**Cause:** Backend server down hai ya internet connection issue hai.

**Solution:**
1. Browser mein backend URL open karo: https://sandbox-backend-production.up.railway.app
2. Agar open nahi ho raha, backend deploy karo
3. Internet connection check karo

### Issue 3: Connection Timeout
**Log:**
```
❌ Network Timeout!
Backend might be down or unreachable
```

**Cause:** Backend slow response de raha hai (>45 seconds).

**Solution:**
- Backend logs check karo
- Railway dashboard mein backend status dekho
- Backend restart karo if needed

### Issue 4: Refund API Failed (400/500)
**Log:**
```
❌ Refund API failed
Status Code: 400
Backend Error: Invalid payment intent
```

**Cause:** Payment Intent ID invalid hai ya already refunded hai.

**Solution:**
- Stripe Dashboard mein payment intent check karo
- Payment status dekho (succeeded, refunded, etc.)
- Agar already refunded hai, Firestore mein order status update karo

## Backend Refund Endpoint

**URL:** `POST /refund-payment`

**Request:**
```json
{
  "paymentIntentId": "pi_xxxxxxxxxxxxx",
  "amount": 5000,
  "reason": "requested_by_customer",
  "orderId": "order123"
}
```

**Response (Success):**
```json
{
  "success": true,
  "refundId": "re_xxxxxxxxxxxxx",
  "status": "succeeded",
  "amountUSD": "17.99",
  "currency": "usd"
}
```

**Response (Error):**
```json
{
  "error": "Payment Intent not found"
}
```

## Testing Checklist

- [ ] Backend server running hai?
- [ ] Backend URL accessible hai browser se?
- [ ] App mein internet permission hai?
- [ ] Order mein Payment Intent ID saved hai?
- [ ] Payment Intent ID format correct hai (pi_xxxxx)?
- [ ] Stripe Dashboard mein payment succeeded hai?
- [ ] Order status PENDING/CONFIRMED hai (not COMPLETED/CANCELLED)?
- [ ] Logcat logs clear dikh rahe hain?

## Next Steps

1. **Install updated APK**
2. **Create test Stripe order**
3. **Try to cancel order**
4. **Check logcat for detailed logs**
5. **Share logs if issue persists**

## Log Commands

```bash
# Real-time logs:
adb logcat -s OrdersViewModel

# Save logs to file:
adb logcat -s OrdersViewModel > refund_logs.txt

# Clear old logs first:
adb logcat -c
```

## Contact Support Message

Agar refund fail ho, user ko yeh message dikhega:
```
Stripe refund failed. Please check:
1. Backend server is running
2. Internet connection
3. Payment Intent ID is valid

Order ID: ABC12345
```

User is Order ID ke saath support se contact kar sakta hai.
