# 🔍 DEBUGGING STEPS - Amount 0 Issue

## Issue
Orders mein **PKR 0** show ho raha hai, matlab `totalAmountPKR` field 0 save ho raha hai Firestore mein.

## Possible Causes

### 1. Product Price Empty/Null
- Product ka `actualPrice` field empty ya null ho sakta hai
- Ya price string mein non-numeric characters hain

### 2. Quantity 0
- Cart mein quantity 0 save ho sakti hai
- Ya quantity fetch nahi ho rahi sahi se

### 3. Calculation Issue
- `actualSubtotal` calculate karte waqt 0 aa raha hai
- Items list empty hai

## Step-by-Step Debugging

### Step 1: Check Logcat
Order place karte waqt ye logs check karo:

```
CheckOutPage: === CHECKOUT CALCULATION ===
CheckOutPage: Subtotal (UI): ???
CheckOutPage: Actual Subtotal (for order): ???
```

**If Actual Subtotal = 0**:
- Product prices check karo Firestore mein
- Cart quantities check karo

### Step 2: Check Item Details
```
CheckOutViewModel: --- Processing Seller: XXX ---
CheckOutViewModel: Items count: ???
CheckOutViewModel: Item 0:
CheckOutViewModel:   - productId: ???
CheckOutViewModel:   - productName: ???
CheckOutViewModel:   - price: ???
CheckOutViewModel:   - quantity: ???
CheckOutViewModel:   Item calculation: price=??? * qty=??? = ???
```

**If price = "0" or null**:
- Firestore mein product ka `actualPrice` field check karo
- Field name sahi hai? (`actualPrice` not `price`)

**If quantity = 0**:
- Cart mein quantity check karo
- `usermodel.value.cartItems[productId]` check karo

### Step 3: Check Seller Total
```
CheckOutViewModel: Seller Total: ???
CheckOutViewModel: Commission (5%): ???
CheckOutViewModel: Seller Amount: ???
```

**If Seller Total = 0.0**:
- Items list empty hai ya
- Sab items ka price * quantity = 0

### Step 4: Check Firestore Order Document
Firebase Console mein jao aur order document dekho:

```json
{
  "orderId": "...",
  "totalAmountPKR": ???,  // Ye 0 hai?
  "platformCommission": ???,
  "sellerAmount": ???,
  "items": [
    {
      "productId": "...",
      "productName": "...",
      "price": "???",  // Ye empty hai?
      "quantity": ???   // Ye 0 hai?
    }
  ]
}
```

## Quick Fixes to Try

### Fix 1: Check Product Model
File: `app/src/main/java/com/example/myapplication/model/ProductModel.kt`

Verify field name:
```kotlin
@get:PropertyName("actualPrice")
@set:PropertyName("actualPrice")
var actualPrice: String = ""
```

### Fix 2: Check Firestore Product Document
Firebase Console → `data/stock/products/{productId}`

Verify:
```json
{
  "id": "...",
  "title": "...",
  "actualPrice": "1000",  // ✅ Should be string with number
  "sellerId": "...",
  ...
}
```

**NOT**:
```json
{
  "price": "1000",  // ❌ Wrong field name
  "actualPrice": "",  // ❌ Empty
  "actualPrice": 1000,  // ❌ Number instead of string
}
```

### Fix 3: Check Cart Data
Firebase Console → `users/{userId}`

Verify:
```json
{
  "cartItems": {
    "productId1": 2,  // ✅ Should be number > 0
    "productId2": 1
  }
}
```

**NOT**:
```json
{
  "cartItems": {
    "productId1": 0,  // ❌ Zero quantity
    "productId1": "2"  // ❌ String instead of number
  }
}
```

## Manual Test

### Test in Kotlin Playground
```kotlin
fun main() {
    val items = listOf(
        mapOf(
            "price" to "1000",
            "quantity" to 1
        )
    )
    
    val total = items.sumOf { item ->
        val price = (item["price"] as? String)?.toDoubleOrNull() ?: 0.0
        val quantity = (item["quantity"] as? Int) ?: 0
        println("price: $price, quantity: $quantity")
        price * quantity
    }
    
    println("Total: $total")  // Should be 1000.0
}
```

## Expected Logs (Correct Flow)

```
CheckOutPage: === CHECKOUT CALCULATION ===
CheckOutPage: Subtotal (UI): 1000.0
CheckOutPage: Actual Subtotal (for order): 1000.0
CheckOutPage: Buyer Balance: 5000.0

CheckOutViewModel: === PLACING MULTI-SELLER ORDER ===
CheckOutViewModel: Total PKR: 1000.0
CheckOutViewModel: Seller Groups: 1

CheckOutViewModel: --- Processing Seller: seller123 ---
CheckOutViewModel: Items count: 1
CheckOutViewModel: Item 0:
CheckOutViewModel:   - productId: prod123
CheckOutViewModel:   - productName: Test Product
CheckOutViewModel:   - price: 1000
CheckOutViewModel:   - quantity: 1
CheckOutViewModel:   Item calculation: price=1000 (1000.0) * qty=1 = 1000.0

CheckOutViewModel: --- Seller: seller123 ---
CheckOutViewModel: Seller Total: 1000.0
CheckOutViewModel: Commission (5%): 50.0
CheckOutViewModel: Seller Amount: 950.0

CheckOutViewModel: Order created: order123
```

## If Still 0

### Last Resort: Add Hardcoded Test
Temporarily add this in CheckOutViewModel before order creation:

```kotlin
// TEMPORARY TEST
android.util.Log.e("TEST", "sellerTotal = $sellerTotal")
if (sellerTotal == 0.0) {
    android.util.Log.e("TEST", "WARNING: sellerTotal is 0!")
    android.util.Log.e("TEST", "Items: $items")
    android.util.Log.e("TEST", "totalPKR param: $totalPKR")
    
    // Force a test value
    // val sellerTotal = totalPKR.toDouble()
}
```

## Action Items

1. ✅ Run app aur order place karo
2. ✅ Logcat mein sab logs check karo
3. ✅ Firebase Console mein product document check karo
4. ✅ Firebase Console mein order document check karo
5. ✅ Logs yahan paste karo for analysis

## Common Issues Found

### Issue: actualPrice is empty string
**Solution**: Firestore mein products ke `actualPrice` field mein values add karo

### Issue: actualPrice is number not string
**Solution**: Firestore mein `actualPrice` ko string mein convert karo: `"1000"` not `1000`

### Issue: Cart quantity is 0
**Solution**: Cart mein items add karte waqt quantity > 0 ensure karo

### Issue: sellerId missing
**Solution**: Products mein `sellerId` field add karo
