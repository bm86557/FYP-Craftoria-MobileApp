# Verification Check Not Working - Debug Steps

## ✅ Code Status:
- AuthViewModel: Verification check implemented ✅
- AddProductPage: UI disabled for non-verified ✅
- Build: Successful ✅

## ❌ Issue:
Seller bina verification ke product upload kar raha hai

## 🔍 Debugging Steps:

### Step 1: Check Logcat
Android Studio → Logcat → Filter: "AuthViewModel"

**Expected Logs:**
```
AuthViewModel: Verification Status: NOT_SUBMITTED, IsVerified: false
```

**If you see:**
```
AuthViewModel: Verification Status: VERIFIED, IsVerified: true
```
Then seller is actually verified in Firestore.

### Step 2: Check Firestore Document
Firebase Console → Firestore → users → {sellerId}

**Check these fields:**
```json
{
  "verificationStatus": "NOT_SUBMITTED",  // Should NOT be "VERIFIED"
  "verifiedAt": null,
  "isVerifiedSeller": false
}
```

### Step 3: Fresh Install
```bash
# Uninstall old app
adb uninstall com.example.myapplication

# Install new build
./gradlew installDebug
```

### Step 4: Test Again
1. Open app
2. Login as seller (not verified)
3. Go to Add Product page
4. Check:
   - ✅ Warning card should show
   - ✅ Form fields should be disabled
   - ✅ Submit button should be disabled

### Step 5: Check if Using Old Code Path
Maybe product is being added through a different function?

**Search for other product creation:**
```
- CoSellerViewModel.addProduct()
- Direct Firestore writes
- AddStoreProductPage
```

## 🐛 Common Issues:

### Issue 1: App Not Restarted
**Solution:** Force stop app and restart

### Issue 2: Using Different Function
**Solution:** Check if AddStoreProductPage or CoSellerViewModel also adds products

### Issue 3: Firestore Rules Not Applied
**Solution:** 
- Check Firebase Console → Firestore → Rules
- Verify rules are published
- Test with Firestore Rules Playground

### Issue 4: Seller Already Verified
**Solution:**
- Check Firestore document
- If verified, test with a new unverified seller account

## 🧪 Test Cases:

### Test 1: UI Check
```
1. Login as unverified seller
2. Navigate to Add Product
3. Expected: Warning card + disabled form
4. Actual: ?
```

### Test 2: Backend Check
```
1. Try to add product (if UI allows)
2. Check Logcat for error message
3. Expected: "You must be a verified seller to add products"
4. Actual: ?
```

### Test 3: Firestore Rules Check
```
1. Try to add product
2. Check Logcat for Firestore permission error
3. Expected: "PERMISSION_DENIED"
4. Actual: ?
```

## 📝 Checklist:

- [ ] App restarted after new build
- [ ] Logcat shows correct verification status
- [ ] Firestore document has correct verificationStatus
- [ ] Warning card shows on Add Product page
- [ ] Form fields are disabled
- [ ] Submit button is disabled
- [ ] Firestore rules are published
- [ ] Testing with correct (unverified) seller account

## 🚀 Quick Fix Commands:

```bash
# Kill app and reinstall
adb shell am force-stop com.example.myapplication
./gradlew installDebug

# Check Logcat
adb logcat | grep "AuthViewModel"

# Check if app is running
adb shell ps | grep "myapplication"
```

## 📞 If Still Not Working:

1. Share Logcat logs (filter: "AuthViewModel")
2. Share Firestore document screenshot (users/{sellerId})
3. Share screenshot of Add Product page
4. Confirm: Did you restart the app after new build?
