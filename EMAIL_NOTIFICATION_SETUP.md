# 📧 Email Notification System - Complete Setup Guide

## ✅ **BACKEND SETUP COMPLETE!**

---

## 🎯 **STEP 1: SendGrid Account Setup**

### **1.1 Create Account:**
1. Go to: https://signup.sendgrid.com/
2. Sign up with your email
3. Verify your email
4. Complete profile:
   - Role: **Developer**
   - Company: **Craftoria**

### **1.2 Create API Key:**
1. Login to SendGrid Dashboard
2. Go to: **Settings → API Keys**
3. Click **"Create API Key"**
4. Settings:
   - Name: `Craftoria-Backend`
   - Permissions: **Full Access**
5. Click **"Create & View"**
6. **COPY THE KEY** (shows only once!)
   ```
   SG.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   ```
7. Save it somewhere safe!

### **1.3 Verify Sender Email:**
1. Go to: **Settings → Sender Authentication**
2. Click **"Verify a Single Sender"**
3. Fill the form:
   ```
   From Name: Craftoria
   From Email: your-email@gmail.com (your actual email)
   Reply To: your-email@gmail.com
   Company: Craftoria
   Address: Any address (e.g., Lahore, Pakistan)
   City: Lahore
   Country: Pakistan
   ```
4. Click **"Create"**
5. **Check your email inbox**
6. Click verification link
7. ✅ Done!

---

## 🎯 **STEP 2: Configure Backend**

### **2.1 Update .env File:**

Open: `stripe-backend/.env`

Replace these values:
```env
SENDGRID_API_KEY=SG.your_actual_api_key_here
SENDGRID_FROM_EMAIL=your-verified-email@gmail.com
SENDGRID_FROM_NAME=Craftoria
```

**Example:**
```env
SENDGRID_API_KEY=SG.abc123xyz789...
SENDGRID_FROM_EMAIL=myemail@gmail.com
SENDGRID_FROM_NAME=Craftoria
```

### **2.2 Install SendGrid Package:**

Open terminal in `stripe-backend` folder:

```bash
cd C:\Users\HP\AndroidStudioProjects\MyApplication2\stripe-backend
npm install @sendgrid/mail
```

### **2.3 Restart Backend:**

```bash
# Stop current server (Ctrl+C)
# Start again
node index.js
```

**You should see:**
```
✅ Server Running on Port 3000
📊 Conversion Rate: 1 USD = 278 PKR
🔑 Stripe Mode: TEST
📧 Email Service: SendGrid ✅
```

---

## 🎯 **STEP 3: Test Email (Optional)**

### **Test with Postman or Browser:**

**URL:** `http://192.168.1.9:3000/send-order-confirmation`

**Method:** POST

**Body (JSON):**
```json
{
  "buyerEmail": "your-test-email@gmail.com",
  "buyerName": "Test User",
  "orderId": "test123456789",
  "orderAmount": 500,
  "itemCount": 2,
  "sellerName": "Test Seller"
}
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Email sent successfully"
}
```

**Check your email inbox!** 📧

---

## 🎯 **STEP 4: App Integration (Next)**

I'll provide the Android code in the next message to integrate with your app.

---

## 📊 **Available Email Endpoints:**

| Endpoint | Purpose | When to Use |
|----------|---------|-------------|
| `/send-order-confirmation` | Order confirmed | Seller confirms order |
| `/send-order-shipped` | Order shipped | Seller ships order |
| `/send-refund-processed` | Refund done | Refund processed |

---

## 🔧 **Troubleshooting:**

### **Error: "API key not configured"**
- Check `.env` file has correct `SENDGRID_API_KEY`
- Restart backend after updating `.env`

### **Error: "From email not verified"**
- Go to SendGrid → Settings → Sender Authentication
- Verify your email address
- Use exact same email in `.env`

### **Error: "Permission denied"**
- API key permissions should be **Full Access**
- Create new API key if needed

### **Email not received:**
- Check spam folder
- Verify sender email in SendGrid
- Check SendGrid dashboard → Activity Feed

---

## 💰 **SendGrid Free Tier:**

- ✅ **100 emails/day** = FREE forever
- ✅ **40,000 emails** in first month = FREE
- ✅ Perfect for testing and small apps

**For your app:**
- 100 orders/day = FREE ✅
- 1000 orders/day = Need paid plan ($19.95/month)

---

## ✅ **Checklist:**

- [ ] SendGrid account created
- [ ] API key generated and copied
- [ ] Sender email verified
- [ ] `.env` file updated with API key
- [ ] `.env` file updated with sender email
- [ ] `npm install @sendgrid/mail` completed
- [ ] Backend restarted
- [ ] Test email sent successfully

---

## 📝 **Next Steps:**

Once backend is working, I'll provide:
1. ✅ Android code to call email API
2. ✅ Integration with OrdersViewModel
3. ✅ Automatic emails on order status changes

**Backend is ready! Tell me when you've completed the SendGrid setup.** 🚀
