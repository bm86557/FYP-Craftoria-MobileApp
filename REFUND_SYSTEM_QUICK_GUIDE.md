# 🔄 Refund System - Quick Guide

**Last Updated:** April 30, 2026  
**Status:** ✅ Ready to Use

---

## 🎯 Quick Overview

Complete order cancellation and refund system with automatic Stripe and wallet refunds.

---

## 📱 How to Use

### **For Buyers:**

#### **1. View Your Orders**
```
Navigate to: My Orders
See all your orders with status
```

#### **2. Cancel an Order**
```
1. Find the order you want to cancel
2. Click "Cancel Order" button (red outline)
3. Enter reason (optional)
4. Click "Yes, Cancel Order"
5. Wait for confirmation
```

#### **3. Check Refund Status**
```
After cancellation:
• Order status shows "❌ Cancelled"
• Refund amount displayed
• Cancellation reason shown

Refund Timeline:
💰 Wallet: Instant
💳 Card: 3-5 business days
💵 COD: No refund needed
```

---

### **For Sellers:**

#### **1. View Incoming Orders**
```
Navigate to: Orders
See all orders from buyers
```

#### **2. Cancel an Order**
```
1. Find the order to cancel
2. Click "Cancel" button (red outline)
3. Enter reason (REQUIRED)
4. Click "Yes, Cancel & Refund"
5. Buyer automatically refunded
```

#### **3. Check Cancellation**
```
After cancellation:
• Order status shows "Cancelled ❌"
• Refund info displayed
• Cancellation reason shown
```

---

## 💰 Refund Methods

### **1. Stripe Card Payment** 💳

**Process:**
```
Cancel Order
    ↓
Backend processes refund
    ↓
Stripe refunds card
    ↓
Buyer receives refund in 3-5 business days
```

**What Buyer Sees:**
```
💳 Refund will be processed to your card (3-5 business days)
```

**Verification:**
- Check Stripe Dashboard
- See refund in "Payments" section
- Status: "Refunded"

---

### **2. Wallet Payment** 💰

**Process:**
```
Cancel Order
    ↓
Wallet instantly credited
    ↓
Transaction recorded
    ↓
Buyer can use balance immediately
```

**What Buyer Sees:**
```
💰 Refund will be credited to your wallet
```

**Verification:**
- Check wallet balance
- See transaction in wallet history
- Type: "REFUND"

---

### **3. Cash on Delivery** 💵

**Process:**
```
Cancel Order
    ↓
No refund needed
    ↓
Order marked as cancelled
```

**What Buyer Sees:**
```
💵 No refund needed (COD order)
```

**Verification:**
- Order status: CANCELLED
- No payment was made

---

## 🎨 UI Screenshots (Text Description)

### **Buyer Order Card:**

```
┌─────────────────────────────────────────┐
│ Order #ORD-12AB                         │
│ Total: Rs. 1400                         │
│ Payment: 💰 Wallet                      │
│ Address: Street, City                   │
│                                         │
│ 💳 Payment Confirmed                    │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │      [Cancel Order]                 │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

### **Cancel Dialog:**

```
┌─────────────────────────────────────────┐
│ Cancel Order?                           │
├─────────────────────────────────────────┤
│                                         │
│ Are you sure you want to cancel this    │
│ order?                                  │
│                                         │
│ Order #ORD-12AB                         │
│ Amount: Rs. 1400                        │
│                                         │
│ 💰 Refund will be credited to your     │
│    wallet                               │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ Reason (optional)                   │ │
│ │ Changed my mind                     │ │
│ └─────────────────────────────────────┘ │
│                                         │
│  [Keep Order]  [Yes, Cancel Order]     │
└─────────────────────────────────────────┘
```

### **After Cancellation:**

```
┌─────────────────────────────────────────┐
│ Order #ORD-12AB                         │
│ Total: Rs. 1400                         │
│ Payment: 💰 Wallet                      │
│ Address: Street, City                   │
│                                         │
│ ❌ Cancelled                            │
│ Refunded: Rs. 1400                      │
│ Reason: Changed my mind                 │
│                                         │
│ ✅ Order cancelled successfully!        │
└─────────────────────────────────────────┘
```

---

## 🔒 Cancellation Rules

### **✅ Can Cancel:**

| Status | Description | Refund |
|--------|-------------|--------|
| **PAYMENT_CONFIRMED** | Payment received | ✅ Yes |
| **PENDING** | Awaiting payment (COD) | N/A |
| **CONFIRMED** | Seller confirmed | ✅ Yes |

### **❌ Cannot Cancel:**

| Status | Description | Action |
|--------|-------------|--------|
| **PROCESSING** | Being prepared | Contact seller |
| **SHIPPED** | Already shipped | Contact support |
| **DELIVERED** | Already delivered | Return process |
| **COMPLETED** | Already completed | No refund |
| **CANCELLED** | Already cancelled | - |

---

## 🧪 Quick Test

### **Test 1: Cancel Wallet Order**

```bash
# Steps:
1. Create order with wallet payment (Rs. 1000)
2. Go to "My Orders"
3. Click "Cancel Order"
4. Enter reason: "Test cancellation"
5. Confirm

# Expected Result:
✅ Order status: CANCELLED
✅ Wallet credited: Rs. 1000
✅ Transaction recorded
✅ Reason saved
```

### **Test 2: Cancel Stripe Order**

```bash
# Steps:
1. Create order with Stripe (Rs. 1400)
2. Go to "My Orders"
3. Click "Cancel Order"
4. Enter reason: "Test refund"
5. Confirm

# Expected Result:
✅ Order status: CANCELLED
✅ Stripe refund created
✅ Check Stripe Dashboard
✅ Refund visible
```

### **Test 3: Seller Cancels Order**

```bash
# Steps:
1. Seller views order (CONFIRMED status)
2. Click "Cancel" button
3. Enter reason: "Out of stock"
4. Confirm

# Expected Result:
✅ Order status: CANCELLED
✅ Buyer refunded automatically
✅ Refund info displayed
✅ Reason saved
```

---

## 🐛 Troubleshooting

### **Issue 1: Cancel button not showing**

**Cause:** Order status doesn't allow cancellation

**Solution:**
- Check order status
- Only PAYMENT_CONFIRMED, PENDING, CONFIRMED can be cancelled
- Contact support for other statuses

---

### **Issue 2: Stripe refund failed**

**Cause:** Backend connection issue or invalid Payment Intent

**Solution:**
1. Check backend is running
2. Verify Payment Intent ID exists
3. Check Stripe Dashboard
4. Contact support if issue persists

---

### **Issue 3: Wallet not credited**

**Cause:** Transaction failed or network issue

**Solution:**
1. Check wallet balance
2. Check transaction history
3. Refresh the page
4. Contact support with order ID

---

## 📊 Refund Timeline

```
┌─────────────────────────────────────────────────────────────┐
│                    REFUND TIMELINE                           │
└─────────────────────────────────────────────────────────────┘

💰 WALLET PAYMENT
Cancel → Instant Credit → Use Immediately
         (< 1 second)

💳 STRIPE CARD PAYMENT
Cancel → Backend Process → Stripe Refund → Card Credit
         (< 5 seconds)     (Immediate)     (3-5 days)

💵 CASH ON DELIVERY
Cancel → Status Update → Done
         (< 1 second)
```

---

## 🎯 Key Points

### **For Buyers:**

✅ Cancel anytime before shipping  
✅ Automatic refunds  
✅ Instant wallet refunds  
✅ Card refunds in 3-5 days  
✅ Optional cancellation reason  
✅ Real-time status updates  

### **For Sellers:**

✅ Can cancel problematic orders  
✅ Buyer automatically refunded  
✅ Must provide cancellation reason  
✅ Refund info displayed  
✅ No manual refund processing  

### **Technical:**

✅ Stripe integration working  
✅ Wallet integration working  
✅ COD handling working  
✅ Real-time updates working  
✅ Error handling implemented  
✅ Loading states implemented  

---

## 📞 Support

### **Common Questions:**

**Q: How long does a refund take?**
- Wallet: Instant
- Card: 3-5 business days
- COD: No refund needed

**Q: Can I cancel after shipping?**
- No, contact support for shipped orders

**Q: Will I get full refund?**
- Yes, full order amount refunded

**Q: Can seller cancel my order?**
- Yes, with reason provided
- You'll be automatically refunded

**Q: Where can I see refund status?**
- In order details
- Shows refund amount and reason

---

## ✅ Checklist

### **Before Cancelling:**

- [ ] Check order status (can be cancelled?)
- [ ] Understand refund timeline
- [ ] Have cancellation reason ready (optional for buyer)

### **After Cancelling:**

- [ ] Verify order status changed to CANCELLED
- [ ] Check refund amount displayed
- [ ] For wallet: Check balance updated
- [ ] For card: Wait 3-5 business days
- [ ] Check transaction history

---

## 🚀 Summary

### **What You Can Do:**

✅ Cancel orders before shipping  
✅ Get automatic refunds  
✅ Track refund status  
✅ See cancellation reasons  
✅ Real-time updates  

### **Refund Methods:**

✅ Stripe Card (3-5 days)  
✅ Wallet (Instant)  
✅ COD (No refund)  

### **Who Can Cancel:**

✅ Buyer (with optional reason)  
✅ Seller (with required reason)  

---

**🎉 Complete refund system ready to use!**

**Need Help?** Check the full documentation in `REFUND_CANCELLATION_SYSTEM.md`

**Last Updated:** April 30, 2026  
**Version:** 2.0.0
