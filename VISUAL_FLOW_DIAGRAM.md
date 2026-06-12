# 📊 Visual Flow Diagrams

**Multi-Seller E-Commerce System**  
**Complete Visual Guide**

---

## 🎯 System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          BUYER (Android App)                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. Browse Products                                                  │
│  2. Add to Cart (from multiple sellers)                             │
│  3. Go to Checkout                                                   │
│  4. Select Payment Method                                            │
│  5. Complete Payment                                                 │
│                                                                      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│                      CHECKOUT SYSTEM (ViewModel)                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  • Group products by seller                                          │
│  • Calculate commission (5%)                                         │
│  • Process payment                                                   │
│  • Create separate orders per seller                                 │
│                                                                      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│                    PAYMENT PROCESSING                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │     COD      │  │    WALLET    │  │    STRIPE    │             │
│  │              │  │              │  │              │             │
│  │ No payment   │  │ Deduct from  │  │ Card payment │             │
│  │ now          │  │ wallet       │  │ via backend  │             │
│  └──────────────┘  └──────────────┘  └──────────────┘             │
│                                                                      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│                    FIREBASE FIRESTORE                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  orders/                                                             │
│  ├── order_1 (Seller A)                                             │
│  │   ├── totalAmountPKR: 800                                        │
│  │   ├── platformCommission: 40                                     │
│  │   ├── sellerAmount: 760                                          │
│  │   └── status: PAYMENT_CONFIRMED                                  │
│  │                                                                   │
│  └── order_2 (Seller B)                                             │
│      ├── totalAmountPKR: 600                                        │
│      ├── platformCommission: 30                                     │
│      ├── sellerAmount: 570                                          │
│      └── status: PAYMENT_CONFIRMED                                  │
│                                                                      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│                    SELLERS (Android App)                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Seller A:                        Seller B:                         │
│  • Sees order (Rs. 800)           • Sees order (Rs. 600)            │
│  • Commission: Rs. 40             • Commission: Rs. 30              │
│  • Will receive: Rs. 760          • Will receive: Rs. 570           │
│  • Clicks "Confirm ✅"            • Clicks "Confirm ✅"             │
│  • Clicks "Complete 🎉"           • Clicks "Complete 🎉"            │
│  • Wallet credited: Rs. 760       • Wallet credited: Rs. 570        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 💰 Payment Flow Comparison

### **Scenario: Rs. 1400 Order (2 Sellers)**

```
┌─────────────────────────────────────────────────────────────────────┐
│                    CASH ON DELIVERY (COD)                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Buyer                                                               │
│    ↓                                                                 │
│  Selects COD                                                         │
│    ↓                                                                 │
│  Orders Created                                                      │
│    ├── Order 1: Rs. 800 (Seller A) - Status: PENDING               │
│    └── Order 2: Rs. 600 (Seller B) - Status: PENDING               │
│    ↓                                                                 │
│  Delivery                                                            │
│    ├── Courier collects Rs. 1400                                    │
│    └── Delivers products                                             │
│    ↓                                                                 │
│  Sellers Complete Orders                                             │
│    ├── Seller A: Receives Rs. 760 (95%)                            │
│    └── Seller B: Receives Rs. 570 (95%)                            │
│    ↓                                                                 │
│  Platform Commission: Rs. 70 (5%)                                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                       WALLET PAYMENT                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Buyer (Balance: Rs. 2000)                                          │
│    ↓                                                                 │
│  Selects "💰 Wallet"                                                │
│    ↓                                                                 │
│  Balance Check: Rs. 2000 ≥ Rs. 1400 ✅                             │
│    ↓                                                                 │
│  Deduct Rs. 1400 (ONE TIME)                                         │
│    ↓                                                                 │
│  New Balance: Rs. 600                                               │
│    ↓                                                                 │
│  Orders Created                                                      │
│    ├── Order 1: Rs. 800 (Seller A) - Status: PAYMENT_CONFIRMED     │
│    └── Order 2: Rs. 600 (Seller B) - Status: PAYMENT_CONFIRMED     │
│    ↓                                                                 │
│  Sellers Complete Orders                                             │
│    ├── Seller A: Wallet +Rs. 760                                   │
│    └── Seller B: Wallet +Rs. 570                                   │
│    ↓                                                                 │
│  Platform Commission: Rs. 70 (5%)                                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                      STRIPE CARD PAYMENT                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Buyer                                                               │
│    ↓                                                                 │
│  Selects "💳 Card Payment"                                          │
│    ↓                                                                 │
│  Backend: Create Payment Intent                                      │
│    ├── Amount: Rs. 1400 → $5.04 USD                                │
│    ├── Metadata: Seller details                                     │
│    └── Returns: Client Secret                                       │
│    ↓                                                                 │
│  Stripe Payment Sheet Opens                                          │
│    ├── Enter card: 4242 4242 4242 4242                             │
│    ├── Expiry: 12/34                                                │
│    └── CVC: 123                                                     │
│    ↓                                                                 │
│  Stripe Processes Payment                                            │
│    ├── Charges card: $5.04                                          │
│    └── Returns: Success                                             │
│    ↓                                                                 │
│  Orders Created                                                      │
│    ├── Order 1: Rs. 800 (Seller A) - Status: PAYMENT_CONFIRMED     │
│    └── Order 2: Rs. 600 (Seller B) - Status: PAYMENT_CONFIRMED     │
│    ↓                                                                 │
│  NO Wallet Deduction ✅                                             │
│    ↓                                                                 │
│  Sellers Complete Orders                                             │
│    ├── Seller A: Wallet +Rs. 760                                   │
│    └── Seller B: Wallet +Rs. 570                                   │
│    ↓                                                                 │
│  Platform Commission: Rs. 70 (5%)                                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Order Status Lifecycle

```
┌─────────────────────────────────────────────────────────────────────┐
│                      ORDER STATUS FLOW                               │
└─────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────┐
                    │  ORDER CREATED      │
                    └──────────┬──────────┘
                               │
                ┌──────────────┴──────────────┐
                │                             │
                ↓                             ↓
    ┌───────────────────┐         ┌───────────────────┐
    │ PAYMENT_CONFIRMED │         │     PENDING       │
    │  (Wallet/Stripe)  │         │      (COD)        │
    └─────────┬─────────┘         └─────────┬─────────┘
              │                             │
              │  Seller clicks              │  Payment
              │  "Confirm ✅"               │  received
              │                             │
              └──────────────┬──────────────┘
                             │
                             ↓
                  ┌───────────────────┐
                  │    CONFIRMED      │
                  │                   │
                  │ Seller preparing  │
                  │ order             │
                  └─────────┬─────────┘
                            │
                            │  Seller clicks
                            │  "Complete 🎉"
                            │
                            ↓
                  ┌───────────────────┐
                  │    COMPLETED      │
                  │                   │
                  │ ✅ Seller paid    │
                  │ ✅ Order finished │
                  └───────────────────┘


                  Alternative Flow:
                  
                  Any Status
                      ↓
                  Cancel Order
                      ↓
                  ┌───────────────────┐
                  │    CANCELLED      │
                  │                   │
                  │ ✅ Buyer refunded │
                  │ (if not COD)      │
                  └───────────────────┘
```

---

## 💸 Commission Breakdown

```
┌─────────────────────────────────────────────────────────────────────┐
│                    COMMISSION CALCULATION                            │
└─────────────────────────────────────────────────────────────────────┘

Example: Rs. 1400 Order (2 Sellers)

┌─────────────────────────────────────────────────────────────────────┐
│                         BUYER PAYS                                   │
│                                                                      │
│                      Rs. 1400 (Total)                               │
│                                                                      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               │ Split by Seller
                               │
                ┌──────────────┴──────────────┐
                │                             │
                ↓                             ↓
    ┌───────────────────────┐     ┌───────────────────────┐
    │   SELLER A ORDER      │     │   SELLER B ORDER      │
    │                       │     │                       │
    │   Total: Rs. 800      │     │   Total: Rs. 600      │
    │                       │     │                       │
    │   ┌───────────────┐   │     │   ┌───────────────┐   │
    │   │ Commission 5% │   │     │   │ Commission 5% │   │
    │   │   Rs. 40      │   │     │   │   Rs. 30      │   │
    │   └───────────────┘   │     │   └───────────────┘   │
    │                       │     │                       │
    │   ┌───────────────┐   │     │   ┌───────────────┐   │
    │   │ Seller Gets   │   │     │   │ Seller Gets   │   │
    │   │   Rs. 760     │   │     │   │   Rs. 570     │   │
    │   │   (95%)       │   │     │   │   (95%)       │   │
    │   └───────────────┘   │     │   └───────────────┘   │
    │                       │     │                       │
    └───────────────────────┘     └───────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                         FINAL BREAKDOWN                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Buyer Paid:              Rs. 1400                                  │
│  ─────────────────────────────────                                  │
│  Seller A Received:       Rs. 760  (95% of Rs. 800)                │
│  Seller B Received:       Rs. 570  (95% of Rs. 600)                │
│  Platform Commission:     Rs. 70   (5% of Rs. 1400)                │
│  ─────────────────────────────────                                  │
│  Total:                   Rs. 1400 ✅                               │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🔐 Stripe Integration Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    STRIPE PAYMENT FLOW                               │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────┐
│ Android App  │
└──────┬───────┘
       │
       │ 1. User clicks "Pay Now"
       │
       ↓
┌──────────────────────────────────────────────────────────────────────┐
│ CheckOutViewModel                                                     │
│                                                                       │
│ • Group products by seller                                            │
│ • Calculate totals                                                    │
│ • Prepare metadata                                                    │
└──────┬────────────────────────────────────────────────────────────────┘
       │
       │ 2. POST /create-payment-intent
       │    {
       │      amountPKR: 1400,
       │      metadata: {
       │        sellerCount: 2,
       │        seller_0_id: "ali",
       │        seller_0_amount: 800,
       │        seller_1_id: "sara",
       │        seller_1_amount: 600
       │      }
       │    }
       │
       ↓
┌──────────────────────────────────────────────────────────────────────┐
│ Stripe Backend (Node.js)                                             │
│                                                                       │
│ • Convert PKR to USD: Rs. 1400 ÷ 278 = $5.04                        │
│ • Create Payment Intent with metadata                                │
│ • Return client secret                                               │
└──────┬────────────────────────────────────────────────────────────────┘
       │
       │ 3. Returns: { clientSecret: "pi_xxx_secret_xxx" }
       │
       ↓
┌──────────────────────────────────────────────────────────────────────┐
│ MainActivity                                                          │
│                                                                       │
│ • Store pending order data                                            │
│ • Present Stripe Payment Sheet                                        │
└──────┬────────────────────────────────────────────────────────────────┘
       │
       │ 4. Show payment sheet
       │
       ↓
┌──────────────────────────────────────────────────────────────────────┐
│ Stripe Payment Sheet                                                 │
│                                                                       │
│ • User enters card: 4242 4242 4242 4242                             │
│ • User enters expiry: 12/34                                          │
│ • User enters CVC: 123                                               │
│ • User clicks "Pay $5.04"                                            │
└──────┬────────────────────────────────────────────────────────────────┘
       │
       │ 5. Process payment
       │
       ↓
┌──────────────────────────────────────────────────────────────────────┐
│ Stripe API                                                            │
│                                                                       │
│ • Validate card                                                       │
│ • Charge $5.04                                                       │
│ • Store metadata                                                      │
│ • Return success                                                      │
└──────┬────────────────────────────────────────────────────────────────┘
       │
       │ 6. Payment successful
       │
       ↓
┌──────────────────────────────────────────────────────────────────────┐
│ MainActivity (Payment Result)                                         │
│                                                                       │
│ • Call saveStripeOrders()                                             │
│ • Create orders in Firestore                                          │
│ • Clear cart                                                          │
│ • Navigate to orders page                                             │
└──────┬────────────────────────────────────────────────────────────────┘
       │
       │ 7. Create orders
       │
       ↓
┌──────────────────────────────────────────────────────────────────────┐
│ Firebase Firestore                                                    │
│                                                                       │
│ orders/order_1                                                        │
│ ├── sellerId: "ali"                                                  │
│ ├── totalAmountPKR: 800                                              │
│ ├── platformCommission: 40                                           │
│ ├── sellerAmount: 760                                                │
│ ├── paymentMethod: "stripe"                                          │
│ ├── stripePaymentIntentId: "pi_xxx"                                 │
│ └── status: "PAYMENT_CONFIRMED"                                      │
│                                                                       │
│ orders/order_2                                                        │
│ ├── sellerId: "sara"                                                 │
│ ├── totalAmountPKR: 600                                              │
│ ├── platformCommission: 30                                           │
│ ├── sellerAmount: 570                                                │
│ ├── paymentMethod: "stripe"                                          │
│ ├── stripePaymentIntentId: "pi_xxx"                                 │
│ └── status: "PAYMENT_CONFIRMED"                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 🎯 Multi-Seller Order Splitting

```
┌─────────────────────────────────────────────────────────────────────┐
│                    BUYER'S CART                                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Product A - Rs. 500 (Seller: Ali)                                  │
│  Product B - Rs. 300 (Seller: Ali)                                  │
│  Product C - Rs. 400 (Seller: Sara)                                 │
│  Product D - Rs. 200 (Seller: Sara)                                 │
│  Product E - Rs. 350 (Seller: Ahmed)                                │
│                                                                      │
│  Total: Rs. 1750                                                    │
│                                                                      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               │ Group by Seller
                               │
                ┌──────────────┴──────────────┬──────────────┐
                │                             │              │
                ↓                             ↓              ↓
    ┌───────────────────┐         ┌───────────────────┐  ┌───────────────────┐
    │   SELLER: ALI     │         │  SELLER: SARA     │  │  SELLER: AHMED    │
    ├───────────────────┤         ├───────────────────┤  ├───────────────────┤
    │ Product A: Rs.500 │         │ Product C: Rs.400 │  │ Product E: Rs.350 │
    │ Product B: Rs.300 │         │ Product D: Rs.200 │  │                   │
    ├───────────────────┤         ├───────────────────┤  ├───────────────────┤
    │ Total: Rs. 800    │         │ Total: Rs. 600    │  │ Total: Rs. 350    │
    │ Commission: Rs.40 │         │ Commission: Rs.30 │  │ Commission: Rs.18 │
    │ Gets: Rs. 760     │         │ Gets: Rs. 570     │  │ Gets: Rs. 333     │
    └───────────────────┘         └───────────────────┘  └───────────────────┘
                │                             │                      │
                │                             │                      │
                └──────────────┬──────────────┴──────────────────────┘
                               │
                               │ Create Separate Orders
                               │
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│                    FIREBASE FIRESTORE                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  orders/ORD-001 (Ali)                                               │
│  orders/ORD-002 (Sara)                                              │
│  orders/ORD-003 (Ahmed)                                             │
│                                                                      │
│  Each order has:                                                     │
│  • Own seller ID                                                     │
│  • Own products                                                      │
│  • Own commission                                                    │
│  • Own seller amount                                                 │
│  • Reference to total order amount                                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 📱 User Interface Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    BUYER INTERFACE                                   │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Browse     │ →   │   Add to     │ →   │   View       │
│   Products   │     │   Cart       │     │   Cart       │
└──────────────┘     └──────────────┘     └──────────────┘
                                                  │
                                                  ↓
                                          ┌──────────────┐
                                          │   Checkout   │
                                          └──────┬───────┘
                                                 │
                        ┌────────────────────────┼────────────────────────┐
                        │                        │                        │
                        ↓                        ↓                        ↓
                ┌──────────────┐        ┌──────────────┐        ┌──────────────┐
                │     COD      │        │    Wallet    │        │    Stripe    │
                │              │        │              │        │              │
                │ Pay on       │        │ Check        │        │ Enter card   │
                │ delivery     │        │ balance      │        │ details      │
                └──────┬───────┘        └──────┬───────┘        └──────┬───────┘
                       │                       │                       │
                       └───────────────────────┼───────────────────────┘
                                               │
                                               ↓
                                       ┌──────────────┐
                                       │   Orders     │
                                       │   Created    │
                                       └──────┬───────┘
                                              │
                                              ↓
                                       ┌──────────────┐
                                       │   Track      │
                                       │   Orders     │
                                       └──────────────┘


┌─────────────────────────────────────────────────────────────────────┐
│                    SELLER INTERFACE                                  │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   View       │ →   │   Confirm    │ →   │   Complete   │
│   Orders     │     │   Order      │     │   Order      │
└──────────────┘     └──────────────┘     └──────┬───────┘
                                                  │
                                                  ↓
                                          ┌──────────────┐
                                          │   Wallet     │
                                          │   Credited   │
                                          └──────────────┘
```

---

## 🎉 Success Indicators

```
┌─────────────────────────────────────────────────────────────────────┐
│                    SYSTEM HEALTH CHECK                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ✅ Multi-seller order splitting working                            │
│  ✅ Commission calculation correct (5%)                             │
│  ✅ Payment methods all functional                                   │
│  ✅ Order status tracking working                                    │
│  ✅ Seller payments on completion                                    │
│  ✅ Buyer refunds on cancellation                                    │
│  ✅ Wallet transactions recorded                                     │
│  ✅ Stripe integration complete                                      │
│  ✅ No double deductions                                             │
│  ✅ Proper payment method display                                    │
│  ✅ All bugs fixed                                                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

**🎯 All flows implemented and working correctly!**

**Last Updated:** April 30, 2026  
**Status:** Production Ready (Test Mode)
