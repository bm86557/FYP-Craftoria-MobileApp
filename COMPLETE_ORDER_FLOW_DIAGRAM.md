# 🔄 Complete Order Management Flow

## 📱 Visual Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         BUYER SIDE                                  │
└─────────────────────────────────────────────────────────────────────┘

    👤 Buyer browses products
         ↓
    🛒 Adds to cart
         ↓
    💳 Goes to Checkout Page
         ↓
    📝 Enters delivery address
         ↓
    💰 Selects payment method
         ↓
    ┌────────────────────────────────────┐
    │   Payment Method Selection         │
    ├────────────────────────────────────┤
    │  ○ Cash on Delivery (COD)          │
    │  ○ Wallet Payment                  │
    │  ○ Card Payment (Stripe)           │
    └────────────────────────────────────┘
         ↓
    ┌────────────────────────────────────┐
    │     Payment Processing             │
    └────────────────────────────────────┘
         ↓
    ┌─────────────┬─────────────┬─────────────┐
    │    COD      │   Wallet    │   Stripe    │
    └─────────────┴─────────────┴─────────────┘
         ↓              ↓              ↓
         │              │              │
         │              │         ┌────────────────┐
         │              │         │ Stripe Payment │
         │              │         │ Sheet Opens    │
         │              │         └────────────────┘
         │              │              ↓
         │              │         ┌────────────────┐
         │              │         │ Enter Card     │
         │              │         │ Details        │
         │              │         └────────────────┘
         │              │              ↓
         │         ┌────────────┐      │
         │         │ Check      │      │
         │         │ Balance    │      │
         │         └────────────┘      │
         │              ↓              ↓
         │         ┌────────────┐ ┌────────────┐
         │         │ Sufficient?│ │ Payment    │
         │         └────────────┘ │ Successful?│
         │              ↓         └────────────┘
         │         ┌────┴────┐        ↓
         │         │ Yes│ No │   ┌────┴────┐
         │         └────┬────┘   │ Yes│ No │
         │              │        └────┬────┘
         │              ↓             │
         │         ┌────────────┐    │
         │         │ Deduct     │    │
         │         │ Wallet     │    │
         │         └────────────┘    │
         │              ↓             │
         └──────────────┴─────────────┘
                        ↓
         ┌──────────────────────────────┐
         │   Order Created in Firestore │
         │   Status: PAYMENT_CONFIRMED  │
         └──────────────────────────────┘
                        ↓
         ┌──────────────────────────────┐
         │   Cart Items Cleared         │
         └──────────────────────────────┘
                        ↓
         ┌──────────────────────────────┐
         │   Navigate to Orders Page    │
         └──────────────────────────────┘
                        ↓
         ┌──────────────────────────────┐
         │   Buyer sees order status    │
         │   with timeline              │
         └──────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                         SELLER SIDE                                 │
└─────────────────────────────────────────────────────────────────────┘

         ┌──────────────────────────────┐
         │   🔔 Seller gets notification│
         │   "New Order Received!"      │
         └──────────────────────────────┘
                        ↓
         ┌──────────────────────────────┐
         │   Seller opens Orders Screen │
         └──────────────────────────────┘
                        ↓
         ┌──────────────────────────────┐
         │   Order Status:              │
         │   💳 PAYMENT_CONFIRMED       │
         │   ⏰ Accept within 24 hours  │
         └──────────────────────────────┘
                        ↓
         ┌──────────────────────────────┐
         │   Seller Decision            │
         ├──────────────────────────────┤
         │   [✅ Accept] [❌ Reject]    │
         └──────────────────────────────┘
                        ↓
         ┌──────────┬──────────────────┐
         │  Accept  │      Reject      │
         └──────────┴──────────────────┘
              ↓              ↓
              │         ┌────────────────┐
              │         │ Order Cancelled│
              │         │ Auto-Refund    │
              │         │ Buyer Notified │
              │         └────────────────┘
              │              ↓
              │         [END - Order Cancelled]
              ↓
    ┌──────────────────────────┐
    │ Status: CONFIRMED        │
    │ Seller accepted order    │
    └──────────────────────────┘
              ↓
    ┌──────────────────────────┐
    │ [📦 Start Processing]    │
    └──────────────────────────┘
              ↓
    ┌──────────────────────────┐
    │ Status: PROCESSING       │
    │ Seller preparing order   │
    └──────────────────────────┘
              ↓
    ┌──────────────────────────┐
    │ [✅ Mark Ready to Ship]  │
    └──────────────────────────┘
              ↓
    ┌──────────────────────────┐
    │ Status: READY_TO_SHIP    │
    └──────────────────────────┘
              ↓
    ┌──────────────────────────┐
    │ Enter Tracking Details:  │
    │ • Tracking Number        │
    │ • Courier Service        │
    └──────────────────────────┘
              ↓
    ┌──────────────────────────┐
    │ [🚚 Mark as Shipped]     │
    └──────────────────────────┘
              ↓
    ┌──────────────────────────┐
    │ Status: SHIPPED          │
    │ 📦 Tracking: TCS123456   │
    └──────────────────────────┘
              ↓
    ┌──────────────────────────┐
    │ [🚚 Out for Delivery]    │
    └──────────────────────────┘
              ↓
    ┌──────────────────────────┐
    │ Status: OUT_FOR_DELIVERY │
    └──────────────────────────┘
              ↓
    ┌──────────────────────────┐
    │ [✅ Mark as Delivered]   │
    └──────────────────────────┘
              ↓
    ┌──────────────────────────┐
    │ Status: DELIVERED        │
    │ ⏰ Auto-complete in 7d   │
    └──────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                    BUYER SIDE (After Delivery)                      │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────────────────────┐
    │ 🔔 Buyer gets notification│
    │ "Order Delivered!"       │
    └──────────────────────────┘
              ↓
    ┌──────────────────────────┐
    │ Buyer can:               │
    │ [✅ Confirm Received]    │
    │ [📝 Leave Review]        │
    │ [↩️ Request Return]      │
    └──────────────────────────┘
              ↓
    ┌──────────┬───────────────┐
    │ Confirms │  Waits 7 days │
    └──────────┴───────────────┘
              ↓
    ┌──────────────────────────┐
    │ Status: COMPLETED        │
    │ 🎉 Order Complete!       │
    └──────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                    SELLER SIDE (Completion)                         │
└─────────────────────────────────────────────────────────────────────┘

    ┌──────────────────────────┐
    │ 💰 Seller Wallet Credited│
    │ Amount: Rs. 1000         │
    └──────────────────────────┘
              ↓
    ┌──────────────────────────┐
    │ Transaction History      │
    │ Type: CREDIT             │
    │ Order: #ORD-12345        │
    └──────────────────────────┘
              ↓
    ┌──────────────────────────┐
    │ 🔔 Seller Notification   │
    │ "Payment Released!"      │
    └──────────────────────────┘

```

---

## 🔄 Detailed Step-by-Step Flow

### **PHASE 1: Order Creation (Buyer)**

#### **Step 1: Product Selection**
```
User Action: Browse products → Add to cart
System: Updates cart in Firestore
UI: Cart badge shows item count
```

#### **Step 2: Checkout**
```
User Action: Click "Checkout"
System: 
  - Fetch cart items from Firestore
  - Calculate subtotal, discount, platform fee
  - Show total amount
UI: CheckoutPage displays order summary
```

#### **Step 3: Address Entry**
```
User Action: Enter delivery address
Fields:
  - Full Name
  - Street Address
  - City
  - Phone Number
Validation: All fields required, phone min 10 digits
```

#### **Step 4: Payment Method Selection**
```
User Action: Select payment method
Options:
  1. Cash on Delivery (COD)
  2. Wallet Payment
  3. Card Payment (Stripe)
```

---

### **PHASE 2: Payment Processing**

#### **Option A: Cash on Delivery (COD)**
```
Flow:
1. User clicks "Pay Now"
2. System creates order immediately
   - status: "AWAITING_PAYMENT"
   - paymentMethod: "cash_on_delivery"
3. No wallet deduction
4. Order saved to Firestore
5. Cart cleared
6. Navigate to Orders page

Database:
orders/{orderId}
  ├─ status: "AWAITING_PAYMENT"
  ├─ paymentMethod: "cash_on_delivery"
  ├─ paymentStatus: "PENDING"
  └─ totalAmountPKR: 1000
```

#### **Option B: Wallet Payment**
```
Flow:
1. User clicks "Pay Now"
2. System checks wallet balance
   ├─ Sufficient → Continue
   └─ Insufficient → Show error
3. Deduct amount from buyer wallet
4. Create order
   - status: "PAYMENT_CONFIRMED"
   - paymentMethod: "wallet"
5. Record transaction in wallet_transactions
6. Cart cleared
7. Navigate to Orders page

Database:
users/{buyerId}
  └─ walletBalance: 4000 → 3000 (deducted 1000)

wallet_transactions/{txnId}
  ├─ userId: buyerId
  ├─ amount: 1000
  ├─ type: "DEBIT"
  ├─ orderId: "ORD-123"
  └─ description: "Payment for Order #ORD-123"

orders/{orderId}
  ├─ status: "PAYMENT_CONFIRMED"
  ├─ paymentMethod: "wallet"
  └─ paymentStatus: "PAID"
```

#### **Option C: Stripe Card Payment**
```
Flow:
1. User clicks "Pay Now"
2. System calls backend to create Payment Intent
   Request:
   {
     "amountPKR": 1000,
     "orderId": "ORD-123",
     "buyerId": "user_abc",
     "sellerId": "seller_xyz",
     "items": [...],
     "buyerEmail": "buyer@example.com"
   }

3. Backend creates Stripe Payment Intent
   Response:
   {
     "clientSecret": "pi_xxx_secret_yyy",
     "paymentIntentId": "pi_123456"
   }

4. App shows Stripe Payment Sheet
5. User enters card details
6. Stripe processes payment
   ├─ Success → Continue
   └─ Failed → Show error

7. On success, create order
   - status: "PAYMENT_CONFIRMED"
   - paymentMethod: "stripe"
   - stripePaymentIntentId: "pi_123456"

8. ❌ NO wallet deduction (Stripe already charged)
9. Cart cleared
10. Navigate to Orders page

Database:
orders/{orderId}
  ├─ status: "PAYMENT_CONFIRMED"
  ├─ paymentMethod: "stripe"
  ├─ stripePaymentIntentId: "pi_123456"
  └─ paymentStatus: "PAID"

Stripe Dashboard:
Payment Intent: pi_123456
  ├─ Amount: Rs. 1000
  ├─ Status: succeeded
  └─ Metadata:
      ├─ orderId: ORD-123
      ├─ buyerId: user_abc
      └─ sellerId: seller_xyz
```

---

### **PHASE 3: Seller Order Management**

#### **Step 1: Order Notification**
```
Trigger: Order created with status "PAYMENT_CONFIRMED"
Action:
  - Send push notification to seller
  - Show badge on Orders tab
  - Play notification sound

Notification:
  Title: "New Order Received!"
  Body: "Order #ORD-123 - Rs. 1000"
  Action: Open Orders Screen
```

#### **Step 2: Seller Views Order**
```
UI Display:
┌─────────────────────────────────┐
│ Order #ORD-123                  │
│ 💳 PAYMENT_CONFIRMED            │
│ ⏰ Accept within: 23h 45m       │
├─────────────────────────────────┤
│ Total: Rs. 1000                 │
│ Payment: STRIPE                 │
│ Items: 3                        │
├─────────────────────────────────┤
│ Buyer: John Doe                 │
│ Phone: 0300-1234567             │
│ Address: Street 123, Karachi    │
├─────────────────────────────────┤
│ [✅ Accept Order] [❌ Reject]   │
└─────────────────────────────────┘
```

#### **Step 3A: Seller Accepts Order**
```
User Action: Click "Accept Order"
System:
  1. Update order status to "CONFIRMED"
  2. Set confirmedAt timestamp
  3. Send notification to buyer
  4. Remove confirmation deadline

Database Update:
orders/{orderId}
  ├─ status: "CONFIRMED"
  ├─ confirmedAt: timestamp
  └─ lastUpdatedAt: timestamp

Buyer Notification:
  "Your order #ORD-123 has been confirmed!"
```

#### **Step 3B: Seller Rejects Order**
```
User Action: Click "Reject"
System:
  1. Update order status to "CANCELLED"
  2. Initiate refund based on payment method
     ├─ Stripe → Call refund API
     ├─ Wallet → Credit buyer wallet
     └─ COD → No refund needed
  3. Send notification to buyer
  4. Record cancellation reason

Database Update:
orders/{orderId}
  ├─ status: "CANCELLED"
  ├─ cancelledAt: timestamp
  └─ cancelReason: "Seller rejected"

Refund (if Stripe):
  - Call backend /refund-payment
  - Stripe refunds to buyer's card
  - Takes 5-10 business days

Refund (if Wallet):
users/{buyerId}
  └─ walletBalance: 3000 → 4000 (refunded 1000)

wallet_transactions/{txnId}
  ├─ type: "REFUND"
  ├─ amount: 1000
  └─ description: "Refund for Order #ORD-123"

Buyer Notification:
  "Order #ORD-123 cancelled. Refund initiated."
```

#### **Step 4: Processing Order**
```
User Action: Click "Start Processing"
System:
  - Update status to "PROCESSING"
  - Timestamp processingStartedAt

UI Changes:
  Button changes to "Mark Ready to Ship"
```

#### **Step 5: Ready to Ship**
```
User Action: Click "Mark Ready to Ship"
System:
  - Update status to "READY_TO_SHIP"
  - Show tracking input fields

UI Display:
┌─────────────────────────────────┐
│ Enter Tracking Details:         │
│ [Tracking Number: _______]      │
│ [Courier: TCS/Leopards/etc]     │
│ [🚚 Mark as Shipped]            │
└─────────────────────────────────┘
```

#### **Step 6: Shipped**
```
User Action: Enter tracking + Click "Mark as Shipped"
System:
  1. Update status to "SHIPPED"
  2. Save tracking number & courier
  3. Set shippedAt timestamp
  4. Send notification to buyer with tracking

Database Update:
orders/{orderId}
  ├─ status: "SHIPPED"
  ├─ trackingNumber: "TCS123456"
  ├─ courierService: "TCS"
  ├─ shippedAt: timestamp
  └─ estimatedDeliveryDate: timestamp + 3 days

Buyer Notification:
  "Your order #ORD-123 has been shipped!"
  "Tracking: TCS123456"
```

#### **Step 7: Out for Delivery**
```
User Action: Click "Out for Delivery"
System:
  - Update status to "OUT_FOR_DELIVERY"
  - Send notification to buyer

Buyer Notification:
  "Your order is out for delivery!"
```

#### **Step 8: Delivered**
```
User Action: Click "Mark as Delivered"
System:
  1. Update status to "DELIVERED"
  2. Set deliveredAt timestamp
  3. Set autoCompleteAt (7 days from now)
  4. Send notification to buyer

Database Update:
orders/{orderId}
  ├─ status: "DELIVERED"
  ├─ deliveredAt: timestamp
  └─ autoCompleteAt: timestamp + 7 days

Buyer Notification:
  "Your order #ORD-123 has been delivered!"
  "Please confirm receipt"
```

---

### **PHASE 4: Order Completion**

#### **Option A: Buyer Confirms Delivery**
```
User Action: Buyer clicks "Confirm Received"
System:
  1. Update status to "COMPLETED"
  2. Credit seller wallet
  3. Record transaction
  4. Send notification to seller

Database Update:
orders/{orderId}
  ├─ status: "COMPLETED"
  ├─ completedAt: timestamp
  └─ completedBy: "buyer"

users/{sellerId}
  └─ walletBalance: 0 → 1000 (credited)

wallet_transactions/{txnId}
  ├─ userId: sellerId
  ├─ type: "CREDIT"
  ├─ amount: 1000
  └─ description: "Payment for Order #ORD-123"

Seller Notification:
  "Payment released for Order #ORD-123!"
  "Rs. 1000 credited to your wallet"
```

#### **Option B: Auto-Complete (After 7 Days)**
```
Trigger: 7 days after delivery
System (Background Job):
  1. Find orders with status "DELIVERED"
  2. Check if deliveredAt + 7 days < now
  3. Auto-complete those orders
  4. Credit seller wallet
  5. Send notifications

Same database updates as Option A
completedBy: "auto"
```

---

## ⏱️ Timeline Example

```
Day 1, 10:00 AM - Order Created (PAYMENT_CONFIRMED)
Day 1, 10:05 AM - Seller Accepts (CONFIRMED)
Day 1, 11:00 AM - Seller Starts Processing (PROCESSING)
Day 1, 02:00 PM - Ready to Ship (READY_TO_SHIP)
Day 1, 03:00 PM - Shipped (SHIPPED)
Day 2, 10:00 AM - Out for Delivery (OUT_FOR_DELIVERY)
Day 2, 05:00 PM - Delivered (DELIVERED)
Day 9, 05:00 PM - Auto-Completed (COMPLETED)
                  Seller Wallet Credited
```

---

## 🔄 Alternative Flows

### **Flow 1: Buyer Cancels Order**
```
Condition: Order status is PAYMENT_CONFIRMED or CONFIRMED
User Action: Buyer clicks "Cancel Order"
System:
  1. Update status to "CANCELLED"
  2. Initiate refund
  3. Notify seller

Result: Order cancelled, buyer refunded
```

### **Flow 2: Seller Doesn't Accept (24hr Timeout)**
```
Condition: Order status is PAYMENT_CONFIRMED for 24+ hours
System (Background Job):
  1. Auto-cancel order
  2. Auto-refund buyer
  3. Notify both parties

Result: Order auto-cancelled, buyer refunded
```

### **Flow 3: Payment Failed**
```
Condition: Stripe payment fails
System:
  1. Don't create order
  2. Show error to buyer
  3. Keep items in cart

Result: No order created, buyer can retry
```

---

## 📊 Status Summary

| Status | Who Controls | Duration | Next Action |
|--------|-------------|----------|-------------|
| PAYMENT_CONFIRMED | System (Auto) | 0-24 hours | Seller accepts/rejects |
| CONFIRMED | Seller | Variable | Seller starts processing |
| PROCESSING | Seller | Variable | Seller marks ready |
| READY_TO_SHIP | Seller | Variable | Seller adds tracking |
| SHIPPED | Seller | 1-3 days | Seller marks out for delivery |
| OUT_FOR_DELIVERY | Seller | Hours | Seller marks delivered |
| DELIVERED | Seller | 7 days | Auto-complete or buyer confirms |
| COMPLETED | System/Buyer | Final | Seller gets paid |
| CANCELLED | Seller/Buyer/System | Final | Refund processed |

---

**Is flow ko samajh aa gaya? Koi specific part detail mein chahiye?** 🚀
