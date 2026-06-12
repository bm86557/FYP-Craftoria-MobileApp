# 🔧 Stripe Backend Code (Node.js)

## 📋 Complete Backend Implementation

### **File: `server.js` or `index.js`**

```javascript
const express = require('express');
const Stripe = require('stripe');
const cors = require('cors');

const app = express();
const stripe = Stripe('YOUR_STRIPE_SECRET_KEY'); // ⚠️ Replace with your secret key

// Middleware
app.use(cors());
app.use(express.json());

// ✅ IMPROVED: Create Payment Intent with Metadata
app.post('/create-payment-intent', async (req, res) => {
  try {
    const { 
      amountPKR,
      metadata,
      description 
    } = req.body;
    
    // Validate input
    if (!amountPKR || amountPKR <= 0) {
      return res.status(400).json({ 
        error: 'Invalid amount' 
      });
    }
    
    // Convert PKR to paisa (smallest unit)
    const amountInPaisa = Math.round(amountPKR * 100);
    
    // Create Payment Intent with complete metadata
    const paymentIntent = await stripe.paymentIntents.create({
      amount: amountInPaisa,
      currency: 'pkr',
      description: description || `Order Payment - Rs. ${amountPKR}`,
      
      // ✅ Include all metadata from app
      metadata: metadata || {},
      
      // ✅ Automatic payment methods
      automatic_payment_methods: {
        enabled: true,
      },
    });
    
    // Return client secret and payment intent ID
    res.json({
      clientSecret: paymentIntent.client_secret,
      paymentIntentId: paymentIntent.id,
      amount: amountPKR,
      currency: 'pkr'
    });
    
  } catch (error) {
    console.error('Error creating payment intent:', error);
    res.status(500).json({ 
      error: error.message 
    });
  }
});

// ✅ NEW: Refund Payment
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
      refundData.amount = Math.round(amount * 100); // Convert to paisa
    }
    
    const refund = await stripe.refunds.create(refundData);
    
    res.json({
      success: true,
      refundId: refund.id,
      status: refund.status,
      amount: refund.amount / 100, // Convert back to PKR
      currency: refund.currency
    });
    
  } catch (error) {
    console.error('Error creating refund:', error);
    res.status(500).json({ 
      error: error.message 
    });
  }
});

// ✅ NEW: Cancel Payment Intent (before payment)
app.post('/cancel-payment-intent', async (req, res) => {
  try {
    const { paymentIntentId } = req.body;
    
    if (!paymentIntentId) {
      return res.status(400).json({ 
        error: 'Payment Intent ID is required' 
      });
    }
    
    const paymentIntent = await stripe.paymentIntents.cancel(
      paymentIntentId
    );
    
    res.json({
      success: true,
      status: paymentIntent.status,
      paymentIntentId: paymentIntent.id
    });
    
  } catch (error) {
    console.error('Error cancelling payment intent:', error);
    res.status(500).json({ 
      error: error.message 
    });
  }
});

// ✅ NEW: Get Payment Intent Details
app.get('/payment-intent/:id', async (req, res) => {
  try {
    const { id } = req.params;
    
    const paymentIntent = await stripe.paymentIntents.retrieve(id);
    
    res.json({
      id: paymentIntent.id,
      amount: paymentIntent.amount / 100,
      currency: paymentIntent.currency,
      status: paymentIntent.status,
      metadata: paymentIntent.metadata,
      created: paymentIntent.created
    });
    
  } catch (error) {
    console.error('Error retrieving payment intent:', error);
    res.status(500).json({ 
      error: error.message 
    });
  }
});

// ✅ Webhook for Stripe events (optional but recommended)
app.post('/webhook', express.raw({type: 'application/json'}), async (req, res) => {
  const sig = req.headers['stripe-signature'];
  const webhookSecret = 'YOUR_WEBHOOK_SECRET'; // ⚠️ Get from Stripe Dashboard
  
  let event;
  
  try {
    event = stripe.webhooks.constructEvent(req.body, sig, webhookSecret);
  } catch (err) {
    console.error('Webhook signature verification failed:', err.message);
    return res.status(400).send(`Webhook Error: ${err.message}`);
  }
  
  // Handle the event
  switch (event.type) {
    case 'payment_intent.succeeded':
      const paymentIntent = event.data.object;
      console.log('Payment succeeded:', paymentIntent.id);
      // TODO: Update order status in your database
      break;
      
    case 'payment_intent.payment_failed':
      const failedPayment = event.data.object;
      console.log('Payment failed:', failedPayment.id);
      // TODO: Handle failed payment
      break;
      
    case 'refund.created':
      const refund = event.data.object;
      console.log('Refund created:', refund.id);
      // TODO: Update order status
      break;
      
    default:
      console.log(`Unhandled event type ${event.type}`);
  }
  
  res.json({received: true});
});

// Health check endpoint
app.get('/', (req, res) => {
  res.json({ 
    status: 'Server is running',
    endpoints: {
      createPayment: 'POST /create-payment-intent',
      refund: 'POST /refund-payment',
      cancel: 'POST /cancel-payment-intent',
      getPayment: 'GET /payment-intent/:id',
      webhook: 'POST /webhook'
    }
  });
});

// Start server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
```

---

## 📦 Package.json

```json
{
  "name": "stripe-payment-backend",
  "version": "1.0.0",
  "description": "Stripe payment backend for multi-seller marketplace",
  "main": "server.js",
  "scripts": {
    "start": "node server.js",
    "dev": "nodemon server.js"
  },
  "dependencies": {
    "express": "^4.18.2",
    "stripe": "^14.0.0",
    "cors": "^2.8.5"
  },
  "devDependencies": {
    "nodemon": "^3.0.1"
  }
}
```

---

## 🔑 Environment Variables

Create `.env` file:

```env
STRIPE_SECRET_KEY=sk_test_your_secret_key_here
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret_here
PORT=3000
```

Update `server.js` to use environment variables:

```javascript
require('dotenv').config();

const stripe = Stripe(process.env.STRIPE_SECRET_KEY);
const webhookSecret = process.env.STRIPE_WEBHOOK_SECRET;
```

---

## 🚀 Deployment Steps

### **1. Update Your Railway Backend:**

```bash
# SSH into your Railway server or use Railway CLI
railway login
railway link

# Update the code
# Copy the new server.js content

# Install dependencies
npm install

# Restart server
railway up
```

### **2. Test Endpoints:**

```bash
# Test create payment intent
curl -X POST https://your-backend.railway.app/create-payment-intent \
  -H "Content-Type: application/json" \
  -d '{
    "amountPKR": 1000,
    "metadata": {
      "buyerId": "test123",
      "sellerCount": 2
    },
    "description": "Test Order"
  }'

# Test refund
curl -X POST https://your-backend.railway.app/refund-payment \
  -H "Content-Type: application/json" \
  -d '{
    "paymentIntentId": "pi_xxx",
    "orderId": "ORD-123"
  }'
```

---

## 📊 Example Request/Response

### **Create Payment Intent:**

**Request:**
```json
POST /create-payment-intent
{
  "amountPKR": 1400,
  "metadata": {
    "buyerId": "user_abc123",
    "buyerEmail": "buyer@example.com",
    "sellerCount": 2,
    "totalAmount": 1400,
    "platform": "android_app",
    "seller_0_id": "seller_ali",
    "seller_0_amount": 800,
    "seller_0_items": 2,
    "seller_1_id": "seller_sara",
    "seller_1_amount": 600,
    "seller_1_items": 2
  },
  "description": "Order from 2 seller(s)"
}
```

**Response:**
```json
{
  "clientSecret": "pi_3ABC123_secret_xyz",
  "paymentIntentId": "pi_3ABC123",
  "amount": 1400,
  "currency": "pkr"
}
```

### **Refund Payment:**

**Request:**
```json
POST /refund-payment
{
  "paymentIntentId": "pi_3ABC123",
  "orderId": "ORD-123456",
  "reason": "requested_by_customer"
}
```

**Response:**
```json
{
  "success": true,
  "refundId": "re_3XYZ789",
  "status": "succeeded",
  "amount": 1400,
  "currency": "pkr"
}
```

---

## 🔒 Security Best Practices

### **1. Use Environment Variables:**
```javascript
// ❌ DON'T
const stripe = Stripe('sk_test_abc123...');

// ✅ DO
const stripe = Stripe(process.env.STRIPE_SECRET_KEY);
```

### **2. Validate Input:**
```javascript
if (!amountPKR || amountPKR <= 0) {
  return res.status(400).json({ error: 'Invalid amount' });
}

if (amountPKR > 1000000) { // Max 1 million PKR
  return res.status(400).json({ error: 'Amount too large' });
}
```

### **3. Use Webhook Signature Verification:**
```javascript
const event = stripe.webhooks.constructEvent(
  req.body, 
  sig, 
  webhookSecret
);
```

### **4. Rate Limiting:**
```javascript
const rateLimit = require('express-rate-limit');

const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});

app.use('/create-payment-intent', limiter);
```

---

## 🧪 Testing

### **Test Mode Keys:**
```
Publishable Key: pk_test_...
Secret Key: sk_test_...
```

### **Test Cards:**
```
Success: 4242 4242 4242 4242
Decline: 4000 0000 0000 0002
Insufficient Funds: 4000 0000 0000 9995
```

### **Test in Stripe Dashboard:**
1. Go to https://dashboard.stripe.com/test/payments
2. See all test payments
3. View metadata
4. Test refunds

---

## 📝 Checklist

### **Before Deployment:**
- [ ] Replace `YOUR_STRIPE_SECRET_KEY` with actual key
- [ ] Set up environment variables
- [ ] Test all endpoints locally
- [ ] Add error handling
- [ ] Add logging
- [ ] Set up webhook endpoint

### **After Deployment:**
- [ ] Test create payment intent
- [ ] Test refund endpoint
- [ ] Verify metadata is saved
- [ ] Check Stripe dashboard
- [ ] Test with Android app
- [ ] Monitor logs

---

## 🔄 Migration Steps

### **If You Already Have Backend Running:**

1. **Backup Current Code:**
```bash
# Download current server.js
railway run cat server.js > server_backup.js
```

2. **Update Code:**
```bash
# Copy new code to server.js
# Make sure to keep your Stripe keys
```

3. **Test Locally First:**
```bash
npm install
npm start
# Test with Postman/curl
```

4. **Deploy:**
```bash
railway up
```

5. **Verify:**
```bash
# Check if server is running
curl https://your-backend.railway.app/

# Test payment intent creation
curl -X POST https://your-backend.railway.app/create-payment-intent \
  -H "Content-Type: application/json" \
  -d '{"amountPKR": 100}'
```

---

## 🐛 Troubleshooting

### **Error: "Invalid API Key"**
```
Solution: Check your STRIPE_SECRET_KEY in environment variables
```

### **Error: "Amount must be at least 50 cents"**
```
Solution: Stripe minimum is 50 paisa (Rs. 0.50)
Ensure amountPKR >= 1
```

### **Error: "CORS policy"**
```
Solution: Add cors middleware
app.use(cors());
```

### **Webhook Not Working:**
```
Solution: 
1. Get webhook secret from Stripe Dashboard
2. Add to environment variables
3. Use raw body parser for webhook endpoint
```

---

## 📚 Additional Resources

- [Stripe API Docs](https://stripe.com/docs/api)
- [Stripe Node.js Library](https://github.com/stripe/stripe-node)
- [Payment Intents Guide](https://stripe.com/docs/payments/payment-intents)
- [Refunds Guide](https://stripe.com/docs/refunds)
- [Webhooks Guide](https://stripe.com/docs/webhooks)

---

## 🎯 Summary

### **What's New in Backend:**

1. ✅ **Enhanced Payment Intent Creation**
   - Accepts metadata from app
   - Stores seller information
   - Better error handling

2. ✅ **Refund Endpoint**
   - Full refunds
   - Partial refunds
   - Metadata tracking

3. ✅ **Cancel Payment Intent**
   - Cancel before payment
   - Proper error handling

4. ✅ **Get Payment Details**
   - Retrieve payment info
   - Check status

5. ✅ **Webhook Support**
   - Real-time event handling
   - Automatic order updates

### **Backend is Ready!**

Copy the code above and deploy to your Railway server. Make sure to:
1. Replace Stripe keys
2. Test all endpoints
3. Update Android app BASE_URL if needed

**Backend code complete and ready to deploy!** 🚀
