// ✅ CORRECTED Dashboard Code for Seller Verification

const handleApprove = async (request) => {
  if (!confirm(`Approve verification for ${request.sellerName}?`)) return;
  
  setProcessing(true);
  try {
    // ✅ FIX 1: Update verification request with correct status
    await updateDoc(doc(db, 'verificationRequests', request.id), {
      status: 'APPROVED',  // ✅ Changed from 'VERIFIED' to 'APPROVED'
      reviewedAt: serverTimestamp(),
      reviewedBy: 'admin',
    });

    // ✅ FIX 2: Update user document with ALL required fields
    await updateDoc(doc(db, 'users', request.sellerId), {
      verificationStatus: 'VERIFIED',        // ✅ Correct - app checks this
      verifiedAt: Date.now(),                // ✅ Correct - timestamp in milliseconds
      isVerifiedSeller: true,                // ✅ Good - extra field for queries
      verificationRejectionReason: '',       // ✅ ADDED - clear any previous rejection
    });

    alert('✅ Seller verified successfully!');
    setSelectedRequest(null);
  } catch (error) {
    console.error('Error approving:', error);
    alert('❌ Failed to approve: ' + error.message);
  } finally {
    setProcessing(false);
  }
};

const handleReject = async (request) => {
  if (!rejectionReason.trim()) {
    alert('Please provide a rejection reason');
    return;
  }
  
  if (!confirm(`Reject verification for ${request.sellerName}?`)) return;
  
  setProcessing(true);
  try {
    // ✅ Update verification request
    await updateDoc(doc(db, 'verificationRequests', request.id), {
      status: 'REJECTED',  // ✅ This is correct
      reviewedAt: serverTimestamp(),
      reviewedBy: 'admin',
      rejectionReason: rejectionReason,
    });

    // ✅ Update user document
    await updateDoc(doc(db, 'users', request.sellerId), {
      verificationStatus: 'REJECTED',        // ✅ Correct
      verificationRejectionReason: rejectionReason,  // ✅ Correct
      isVerifiedSeller: false,               // ✅ ADDED - set to false
      verifiedAt: null,                      // ✅ ADDED - clear verified timestamp
    });

    alert('✅ Verification rejected');
    setSelectedRequest(null);
    setRejectionReason('');
  } catch (error) {
    console.error('Error rejecting:', error);
    alert('❌ Failed to reject: ' + error.message);
  } finally {
    setProcessing(false);
  }
};

// ✅ SUMMARY OF CHANGES:
// 
// 1. verificationRequests.status: 'VERIFIED' → 'APPROVED'
// 2. Added verificationRejectionReason: '' when approving (clear previous rejection)
// 3. Added isVerifiedSeller: false when rejecting
// 4. Added verifiedAt: null when rejecting (clear timestamp)
//
// These changes ensure the app's VerificationViewModel receives the correct data.
