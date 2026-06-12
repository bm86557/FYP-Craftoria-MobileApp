# 🧪 Testing Guide: Stripe Refund Tracking

## Prerequisites
- App installed on device/emulator
- Stripe backend running
- Test Stripe account configured
- At least 2 seller accounts and 1 buyer account

---

## Test Case 1: Normal Multi-Order Refund ✅

### Steps:
1. **Login as Buyer**
2. **Add products from 2 different sellers to cart**
   - Seller A: Product worth Rs. 500
   - Seller B: Product worth Rs. 300
3. **Checkout with Stripe payment**
   - Total: Rs. 800
   - Use test card: `4242 4242 4242 4242`
4. **Verify orders created**
   - Check "My Orders" screen
   - Should show 2 separate orders
5. **Cancel Order 1 (Rs. 500)**
   - Tap on Order 1
   - Select "Cancel Order"
   - Enter reason
   - Wait for seller approval (or approve as seller)
6. **Check Logcat for tracking:**
   ```
   OrdersViewModel: First refund for this PaymentIntent
   OrdersViewModel: Total Paid: 800.0
   OrdersViewModel: ✅ Stripe refund successful
   ```
7. **Cancel Order 2 (Rs. 300)**
   - Tap on Order 2
   - Select "Cancel Order"
   - Enter reason
   - Wait for seller approval
8. **Check Logcat for tracking:**
   ```
   OrdersViewModel: Total Paid: 800.0
   OrdersViewModel: Already Refunded: 500.0
   OrdersViewModel: Remaining: 300.0
   OrdersViewModel: ✅ Stripe refund successful
   ```

### Expected Result:
- ✅ Both orders refunded successfully
- ✅ Both show "REFUNDED" status
- ✅ Buyer receives Rs. 800 back to card
- ✅ No errors in logs

---

## Test Case 2: Prevent Double Refund ❌

### Steps:
1. **Create 1 order with Stripe (Rs. 500)**
2. **Cancel the order**
   - Should refund successfully
3. **Try to cancel the SAME order again**
   - Go to order details
   - Try to cancel again

### Expected Result:
- ❌ Error message: "Order already refunded"
- ✅ Logcat shows:
   ```
   OrdersViewModel: ❌ Order already refunded!
   ```
- ✅ No duplicate refund processed
- ✅ Order status remains "REFUNDED"

---

## Test Case 3: Concurrent Refunds ⚡

### Steps:
1. **Create 3 orders with Stripe**
   - Order 1: Rs. 400
   - Order 2: Rs. 300
   - Order 3: Rs. 200
   - Total: Rs. 900
2. **Cancel all 3 orders quickly (within 5 seconds)**
   - Open Order 1 → Cancel
   - Immediately open Order 2 → Cancel
   - Immediately open Order 3 → Cancel
3. **Wait for all refunds to process**

### Expected Result:
- ✅ All 3 orders refunded successfully
- ✅ Total refunded: Rs. 900
- ✅ No race condition errors
- ✅ Tracking document shows:
   ```
   totalPaid: 900
   totalRefunded: 900
   refundedOrders: ["order1", "order2", "order3"]
   ```

---

## Test Case 4: Wallet Payment Refund ✅

### Steps:
1. **Add Rs. 1000 to buyer wallet**
2. **Create order with wallet payment (Rs. 500)**
3. **Cancel the order**

### Expected Result:
- ✅ Refund processed immediately
- ✅ Wallet balance increases by Rs. 500
- ✅ No Stripe tracking created (wallet only)
- ✅ Order shows "REFUNDED" status

---

## Test Case 5: COD Order Cancellation ✅

### Steps:
1. **Create order with Cash on Delivery (Rs. 500)**
2. **Cancel the order before delivery**

### Expected Result:
- ✅ Order cancelled successfully
- ✅ No refund needed (payment not made yet)
- ✅ Order shows "CANCELLED" status
- ✅ Logcat shows: "COD - No refund needed"

---

## Test Case 6: Partial Refund Scenario 💰

### Steps:
1. **Create 2 orders with Stripe**
   - Order 1: Rs. 600
   - Order 2: Rs. 400
   - Total: Rs. 1000
2. **Cancel Order 1 only**
   - Should refund Rs. 600
3. **Check Firestore tracking document**
   - Path: `stripe_refund_tracking/{paymentIntentId}`
   - Should show:
     ```
     totalPaid: 1000
     totalRefunded: 600
     refundedOrders: ["order1"]
     ```
4. **Order 2 remains active**
   - Status: "PAYMENT_CONFIRMED" or "PROCESSING"
   - Can still be delivered

### Expected Result:
- ✅ Order 1 refunded: Rs. 600
- ✅ Order 2 still active
- ✅ Remaining refundable: Rs. 400
- ✅ Buyer can still cancel Order 2 later

---

## Test Case 7: Stripe API Failure Rollback 🔄

### Steps:
1. **Stop the Stripe backend server**
   ```bash
   # Stop the Node.js server
   ```
2. **Create order with Stripe (Rs. 500)**
   - This should still work (payment already processed)
3. **Try to cancel the order**
   - Refund will fail (backend not running)

### Expected Result:
- ❌ Refund fails with error
- ✅ Tracking document rolled back
- ✅ Logcat shows:
   ```
   OrdersViewModel: ❌ Stripe API call failed, rolling back tracking
   OrdersViewModel: ✅ Refund tracking rolled back
   ```
- ✅ Can retry refund after restarting backend

---

## Test Case 8: Check Refund Status (Debug) 🔍

### Steps:
1. **Create order with Stripe**
2. **In code, add debug logging:**
   ```kotlin
   viewModelScope.launch {
       val status = getRefundStatus(paymentIntentId)
       Log.d("RefundDebug", "Status: $status")
   }
   ```
3. **Check logs**

### Expected Result:
```
RefundDebug: Status: {
  exists=true,
  totalPaid=800.0,
  totalRefunded=500.0,
  remainingRefundable=300.0,
  refundedOrders=[order1]
}
```

---

## Firestore Verification 📊

### Check Tracking Document:
1. Open Firebase Console
2. Go to Firestore Database
3. Navigate to `stripe_refund_tracking` collection
4. Find document with PaymentIntent ID
5. Verify fields:
   - `totalPaid` = original payment amount
   - `totalRefunded` = sum of all refunds
   - `refundedOrders` = array of refunded order IDs

### Check Order Documents:
1. Navigate to `orders` collection
2. Find your test orders
3. Verify fields:
   - `refundStatus` = "PROCESSED"
   - `refundedAmount` = individual refund amount
   - `paymentStatus` = "REFUNDED"

---

## Common Issues & Solutions 🔧

### Issue 1: "Order already refunded" on first refund
**Cause:** Order was refunded before
**Solution:** Check `refundedOrders` array in tracking document

### Issue 2: "Insufficient refundable amount"
**Cause:** Trying to refund more than remaining balance
**Solution:** Check `totalRefunded` vs `totalPaid` in tracking document

### Issue 3: Tracking document not created
**Cause:** Old order created before fix
**Solution:** System will auto-create on first refund attempt

### Issue 4: Refund fails silently
**Cause:** Stripe backend not running or wrong URL
**Solution:** Check `BASE_URL` in OrdersViewModel and ensure backend is running

---

## Success Criteria ✅

All tests should pass with:
- ✅ No crashes
- ✅ No duplicate refunds
- ✅ No over-refunding
- ✅ Correct order status updates
- ✅ Accurate tracking in Firestore
- ✅ Proper error messages
- ✅ Rollback on failures

---

## Performance Notes 📈

- Firestore transactions ensure atomic operations
- Concurrent refunds handled correctly
- No race conditions
- Rollback mechanism prevents inconsistent state
- Average refund processing time: 2-5 seconds

---

## Next Steps After Testing 🚀

1. ✅ Test all scenarios above
2. ✅ Verify Firestore data
3. ✅ Check Stripe dashboard for refunds
4. ✅ Test with real payment amounts
5. ✅ Deploy to production

---

## Support 💬

If any test fails:
1. Check Logcat for detailed error messages
2. Verify Firestore tracking document
3. Check Stripe dashboard
4. Ensure backend is running
5. Verify network connectivity
