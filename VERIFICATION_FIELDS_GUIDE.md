# Seller Verification - Field Names Guide

## Important: Dashboard Integration

When verifying a seller from the dashboard, make sure to update the following fields in the `users` collection:

### Required Fields to Update

```javascript
// Firestore path: users/{userId}

{
  "verificationStatus": "VERIFIED",  // Must be exactly "VERIFIED" (all caps)
  "verifiedAt": Date.now(),          // Timestamp in milliseconds
  "verificationRejectionReason": ""  // Clear any rejection reason
}
```

### Possible Status Values

- `"NOT_SUBMITTED"` - User hasn't submitted verification yet
- `"PENDING"` - Verification request submitted, waiting for review
- `"VERIFIED"` - Seller is verified ✅
- `"REJECTED"` - Verification rejected

### For Rejection

```javascript
{
  "verificationStatus": "REJECTED",
  "verificationRejectionReason": "Reason for rejection here",
  "verifiedAt": null
}
```

## Firestore Collections

### 1. `verificationRequests` Collection
This stores all verification requests for admin review:

```javascript
{
  "sellerId": "user_uid",
  "sellerName": "Seller Name",
  "sellerEmail": "seller@example.com",
  "selfieUrl": "https://cloudinary.com/...",
  "status": "PENDING",
  "submittedAt": Timestamp,
  "reviewedAt": Timestamp | null,
  "reviewedBy": "admin_uid" | null,
  "rejectionReason": "string" | null
}
```

### 2. `users/{userId}` Document
Update these fields in the user document:

```javascript
{
  // ... other user fields
  "verificationStatus": "VERIFIED",
  "verificationSelfieUrl": "https://cloudinary.com/...",
  "verificationSubmittedAt": 1234567890,
  "verifiedAt": 1234567890,
  "verificationRejectionReason": ""
}
```

## Testing the Fix

1. **Check Logs**: Look for these logs in Android Studio Logcat:
   ```
   VerificationVM: Status updated: VERIFIED
   VerificationVM: verifiedAt: 1234567890
   VerificationVM: All fields: {verificationStatus=VERIFIED, ...}
   ```

2. **Manual Refresh**: User can tap the refresh button (↻) in the top bar

3. **Real-time Updates**: The app uses Firestore snapshot listeners, so changes should appear automatically

## Common Issues

### Issue: Status not updating in app
**Solution**: 
- Verify field names are exactly as shown above (case-sensitive)
- Check that you're updating the correct user document
- Use the refresh button in the app to force a manual check

### Issue: Still showing "PENDING"
**Possible causes**:
- Field name mismatch (e.g., "verified" instead of "VERIFIED")
- Wrong user document being updated
- Firestore security rules blocking the read

## Dashboard Code Example

```javascript
// Example: Approve verification request
async function approveVerification(userId, requestId) {
  const batch = firestore.batch();
  
  // Update user document
  const userRef = firestore.collection('users').doc(userId);
  batch.update(userRef, {
    verificationStatus: 'VERIFIED',
    verifiedAt: Date.now(),
    verificationRejectionReason: ''
  });
  
  // Update verification request
  const requestRef = firestore.collection('verificationRequests').doc(requestId);
  batch.update(requestRef, {
    status: 'APPROVED',
    reviewedAt: firebase.firestore.FieldValue.serverTimestamp(),
    reviewedBy: currentAdminId
  });
  
  await batch.commit();
}
```

## App Changes Made

1. ✅ Added `refreshVerificationStatus()` function in VerificationViewModel
2. ✅ Added refresh button (↻) in the top bar
3. ✅ Added better logging to track status changes
4. ✅ Added visual feedback during refresh
5. ✅ Added tip message in pending state

## Next Steps

1. Verify dashboard is using correct field names
2. Test the refresh button in the app
3. Check Logcat for verification status logs
4. Ensure Firestore security rules allow reading verification fields
