# 🚀 Stripe Backend Setup Guide

## ✅ ISSUE FIXED: Communication Error

### **Problem:**
Card payment showing "Communication Error" because:
1. ❌ Stripe backend was not running
2. ❌ Wrong IP address in app (192.168.1.6 instead of 192.168.1.9)

### **Solution Applied:**
1. ✅ Started Stripe backend server on port 3000
2. ✅ Updated IP address to 192.168.1.9 in both files:
   - `CheckOutViewModel.kt`
   - `OrdersViewModel.kt`

---

## 🖥️ Backend Status

### **Server Running:**
```
✅ Server Running on Port 3000
📊 Conversion Rate: 1 USD = 278 PKR
🔑 Stripe Mode: TEST
```

### **Server URL:**
```
http://192.168.1.9:3000
```

### **Endpoints Available:**
- `POST /create-payment-intent` - Create Stripe payment
- `POST /refund-payment` - Process refund
- `POST /cancel-payment-intent` - Cancel payment
- `GET /payment-intent/:id` - Get payment details
- `POST /webhook` - Stripe webhook (optional)

---

## 📱 App Configuration

### **Updated Files:**

#### 1. CheckOutViewModel.kt
```kotlin
private val BASE_URL = "http://192.168.1.9:3000"
```

#### 2. OrdersViewModel.kt
```kotlin
private val BASE_URL = "http://192.168.1.9:3000"
```

---

## 🧪 Testing Steps

### **1. Verify Backend is Running:**
Open browser and go to:
```
http://192.168.1.9:3000
```

You should see:
```json
{
  "status": "Server is running",
  "version": "2.0.0",
  "endpoints": { ... }
}
```

### **2. Test from Phone:**
Make sure your phone and computer are on the **SAME WiFi network**:
- Computer IP: 192.168.1.9
- Phone: Connected to same WiFi

### **3. Test Payment:**
1. Open app on phone
2. Add products to cart
3. Go to checkout
4. Select "Card Payment"
5. Use test card: `4242 4242 4242 4242`
6. Expiry: Any future date (e.g., 12/25)
7. CVC: Any 3 digits (e.g., 123)
8. Complete payment

### **Expected Result:**
- ✅ Payment processes successfully
- ✅ Orders created in Firestore
- ✅ No communication error

---

## 🔧 Troubleshooting

### **Issue 1: Still Getting Communication Error**

**Check 1: Backend Running?**
```bash
# In stripe-backend folder
node index.js
```

**Check 2: Correct IP Address?**
```bash
ipconfig | findstr "IPv4"
```
Update `BASE_URL` in both ViewModels if IP changed.

**Check 3: Same WiFi Network?**
- Computer and phone must be on same WiFi
- Check phone WiFi settings

**Check 4: Firewall Blocking?**
```bash
# Allow Node.js through Windows Firewall
# Or temporarily disable firewall for testing
```

---

### **Issue 2: Backend Crashes**

**Check Logs:**
Look at terminal output for errors

**Common Causes:**
- Missing Stripe key in `.env`
- Port 3000 already in use
- Node.js not installed

**Solution:**
```bash
# Kill process on port 3000
netstat -ano | findstr :3000
taskkill /PID <PID> /F

# Restart backend
node index.js
```

---

### **Issue 3: Payment Fails**

**Check Stripe Dashboard:**
- Go to: https://dashboard.stripe.com/test/payments
- Check if payment intent was created
- Look for error messages

**Check App Logs:**
```
adb logcat | findstr "CheckOutViewModel"
```

**Common Issues:**
- Amount too small (minimum Rs. 14)
- Invalid card number
- Network timeout

---

## 🔄 Keeping Backend Running

### **Option 1: Keep Terminal Open**
Just leave the terminal running with `node index.js`

### **Option 2: Use PM2 (Recommended)**
```bash
# Install PM2
npm install -g pm2

# Start backend with PM2
pm2 start index.js --name stripe-backend

# Backend will auto-restart on crash
# And survive terminal close
```

### **Option 3: Windows Service**
Use `node-windows` to run as Windows service

---

## 📊 Current Configuration

| Setting | Value |
|---------|-------|
| **Server IP** | 192.168.1.9 |
| **Server Port** | 3000 |
| **Stripe Mode** | TEST |
| **Currency** | USD |
| **Conversion Rate** | 1 USD = 278 PKR |
| **Min Amount** | Rs. 14 (50 cents) |

---

## 🎯 Next Steps

1. ✅ Backend is running
2. ✅ IP address updated in app
3. ✅ Ready to test payments

### **To Test:**
1. **Rebuild app** in Android Studio
2. **Install on phone**
3. **Test card payment**
4. **Verify order created**

---

## 💡 Important Notes

### **IP Address Changes:**
If your computer's IP address changes (e.g., after restart or WiFi change):
1. Check new IP: `ipconfig | findstr "IPv4"`
2. Update `BASE_URL` in both ViewModels
3. Rebuild and reinstall app

### **Production Deployment:**
For production, replace local IP with:
- Cloud server URL (e.g., Heroku, AWS, DigitalOcean)
- Or use ngrok for testing: `ngrok http 3000`

### **Security:**
- Never commit `.env` file with real Stripe keys
- Use test keys for development
- Switch to live keys only in production

---

## ✅ Summary

**Problem:** Communication error during card payment
**Cause:** Backend not running + wrong IP address
**Solution:** Started backend + updated IP to 192.168.1.9
**Status:** ✅ FIXED - Ready to test!

**Ab payment kaam karega! App rebuild karo aur test karo.** 🎉
