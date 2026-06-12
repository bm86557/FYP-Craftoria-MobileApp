'use client';

import { useState, useEffect } from 'react';
import { db } from '@/lib/firebase';
import { collection, query, where, onSnapshot, doc, updateDoc, serverTimestamp } from 'firebase/firestore';
import Image from 'next/image';
import styles from './verification.module.css';

export default function SellerVerification() {
  const [verificationRequests, setVerificationRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('PENDING');
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const [processing, setProcessing] = useState(false);

  useEffect(() => {
    const q = filter === 'ALL' 
      ? query(collection(db, 'verificationRequests'))
      : query(collection(db, 'verificationRequests'), where('status', '==', filter));

    const unsubscribe = onSnapshot(q, (snapshot) => {
      const requests = [];
      snapshot.forEach((doc) => {
        requests.push({ id: doc.id, ...doc.data() });
      });

      requests.sort((a, b) => {
        const timeA = a.submittedAt?.seconds || 0;
        const timeB = b.submittedAt?.seconds || 0;
        return timeB - timeA;
      });

      setVerificationRequests(requests);
      setLoading(false);
    });

    return () => unsubscribe();
  }, [filter]);

  // Lock body scroll when modal is open
  useEffect(() => {
    if (selectedRequest) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }
    return () => {
      document.body.style.overflow = 'unset';
    };
  }, [selectedRequest]);

  // ✅ FIXED: Approve handler
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
        verificationStatus: 'VERIFIED',        // ✅ App checks this field
        verifiedAt: Date.now(),                // ✅ Timestamp in milliseconds
        isVerifiedSeller: true,                // ✅ Extra field for queries
        verificationRejectionReason: '',       // ✅ Clear any previous rejection
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

  // ✅ FIXED: Reject handler
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
        status: 'REJECTED',
        reviewedAt: serverTimestamp(),
        reviewedBy: 'admin',
        rejectionReason: rejectionReason,
      });

      // ✅ FIX: Update user document with all fields
      await updateDoc(doc(db, 'users', request.sellerId), {
        verificationStatus: 'REJECTED',
        verificationRejectionReason: rejectionReason,
        isVerifiedSeller: false,               // ✅ ADDED: Set to false
        verifiedAt: null,                      // ✅ ADDED: Clear timestamp
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

  const formatDate = (timestamp) => {
    if (!timestamp) return 'N/A';
    const date = timestamp.seconds ? new Date(timestamp.seconds * 1000) : new Date(timestamp);
    return date.toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  if (loading) {
    return (
      <div className={styles.loading}>
        <div className={styles.loadingContent}>
          <div className={styles.spinner}></div>
          <p>Loading verification requests...</p>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <div className={styles.wrapper}>
        {/* Header Section */}
        <div className={styles.header}>
          <div className={styles.headerContent}>
            <div className={styles.iconBox}>🔍</div>
            <div className={styles.headerText}>
              <h1>Seller Verification</h1>
              <p>Review and approve seller verification requests</p>
            </div>
          </div>
        </div>

        {/* Filter Tabs */}
        <div className={styles.filterCard}>
          <div className={styles.filterTabs}>
            {['PENDING', 'VERIFIED', 'REJECTED', 'ALL'].map((status) => {
              const count = verificationRequests.filter(r => 
                status === 'ALL' || r.status === status
              ).length;

              return (
                <button
                  key={status}
                  onClick={() => setFilter(status)}
                  className={`${styles.filterTab} ${filter === status ? styles.filterTabActive : ''}`}
                >
                  <span>{status}</span>
                  <span className={`${styles.badge} ${
                    filter === status ? styles.badgeActive : styles.badgeDefault
                  }`}>
                    {count}
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Empty State or Cards Grid */}
        {verificationRequests.length === 0 ? (
          <div className={styles.emptyState}>
            <div className={styles.emptyIcon}>📭</div>
            <h3>No Requests Found</h3>
            <p>There are no {filter.toLowerCase()} verification requests at the moment.</p>
          </div>
        ) : (
          <div className={styles.cardsGrid}>
            {verificationRequests.map((request) => (
              <div key={request.id} className={styles.card}>
                {/* Selfie Image */}
                <div className={styles.cardImage}>
                  <Image
                    src={request.selfieUrl}
                    alt={`${request.sellerName} selfie`}
                    fill
                    className="object-cover"
                    unoptimized
                  />
                  <div className={`${styles.statusBadge} ${
                    request.status === 'PENDING' ? styles.statusPending :
                    request.status === 'VERIFIED' ? styles.statusVerified :
                    styles.statusRejected
                  }`}>
                    {request.status}
                  </div>
                </div>

                {/* Card Content */}
                <div className={styles.cardContent}>
                  <h3 className={styles.cardTitle}>{request.sellerName}</h3>
                  <p className={styles.cardEmail}>{request.sellerEmail}</p>
                  
                  <div className={styles.cardDate}>
                    <span>📅</span>
                    <span>{formatDate(request.submittedAt)}</span>
                  </div>

                  {request.status === 'REJECTED' && request.rejectionReason && (
                    <div className={styles.rejectionBox}>
                      <p><strong>Reason:</strong> {request.rejectionReason}</p>
                    </div>
                  )}

                  {request.status === 'PENDING' && (
                    <button
                      onClick={() => setSelectedRequest(request)}
                      className={styles.reviewButton}
                      disabled={processing}
                    >
                      👁️ Review Request
                    </button>
                  )}

                  {request.status === 'VERIFIED' && (
                    <div className={styles.verifiedBox}>
                      ✓ Verified
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Review Modal */}
        {selectedRequest && (
          <>
            {/* Backdrop */}
            <div 
              className={styles.modalBackdrop}
              onClick={() => {
                setSelectedRequest(null);
                setRejectionReason('');
              }}
            />

            {/* Modal Content */}
            <div className={styles.modalContainer}>
              <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
                {/* Header */}
                <div className={styles.modalHeader}>
                  <h2>Review Verification</h2>
                  <button
                    onClick={() => {
                      setSelectedRequest(null);
                      setRejectionReason('');
                    }}
                    className={styles.closeButton}
                    disabled={processing}
                    aria-label="Close"
                  >
                    ×
                  </button>
                </div>

                {/* Content */}
                <div className={styles.modalContent}>
                  {/* Selfie Preview */}
                  <div className={styles.selfiePreview}>
                    <div className={styles.selfieWrapper}>
                      <Image
                        src={selectedRequest.selfieUrl}
                        alt="Seller verification selfie"
                        fill
                        className="object-contain"
                        unoptimized
                        priority
                      />
                    </div>
                  </div>

                  {/* Seller Info */}
                  <div className={styles.sellerInfo}>
                    <div className={styles.infoGrid}>
                      <div className={styles.infoItem}>
                        <p className={styles.infoLabel}>👤 Seller Name</p>
                        <p className={styles.infoValue}>{selectedRequest.sellerName}</p>
                      </div>

                      <div className={styles.infoItem}>
                        <p className={styles.infoLabel}>📧 Email</p>
                        <p className={styles.infoValue}>{selectedRequest.sellerEmail}</p>
                      </div>

                      <div className={styles.infoItem} style={{ gridColumn: '1 / -1' }}>
                        <p className={styles.infoLabel}>🆔 Seller ID</p>
                        <p className={styles.infoValueMono}>{selectedRequest.sellerId}</p>
                      </div>

                      <div className={styles.infoItem}>
                        <p className={styles.infoLabel}>📅 Submitted</p>
                        <p className={styles.infoValue}>{formatDate(selectedRequest.submittedAt)}</p>
                      </div>

                      <div className={styles.infoItem}>
                        <p className={styles.infoLabel}>📊 Status</p>
                        <span className={`${styles.statusBadgeInline} ${
                          selectedRequest.status === 'PENDING' ? styles.statusPending :
                          selectedRequest.status === 'VERIFIED' ? styles.statusVerified :
                          styles.statusRejected
                        }`}>
                          {selectedRequest.status}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Rejection Reason */}
                  <div className={styles.rejectionSection}>
                    <label>
                      ❌ Rejection Reason <span>(Required if rejecting)</span>
                    </label>
                    <textarea
                      value={rejectionReason}
                      onChange={(e) => setRejectionReason(e.target.value)}
                      placeholder="e.g., Photo is blurry, face not clearly visible, wearing sunglasses, etc."
                      className={styles.rejectionTextarea}
                      rows="3"
                    />
                  </div>
                </div>

                {/* Footer */}
                <div className={styles.modalFooter}>
                  <div className={styles.actionButtons}>
                    <button
                      onClick={() => handleApprove(selectedRequest)}
                      disabled={processing}
                      className={styles.approveButton}
                    >
                      {processing ? '⏳ Processing...' : '✓ Approve'}
                    </button>

                    <button
                      onClick={() => handleReject(selectedRequest)}
                      disabled={processing || !rejectionReason.trim()}
                      className={styles.rejectButton}
                    >
                      {processing ? '⏳ Processing...' : '✗ Reject'}
                    </button>

                    <button
                      onClick={() => {
                        setSelectedRequest(null);
                        setRejectionReason('');
                      }}
                      disabled={processing}
                      className={styles.cancelButton}
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
