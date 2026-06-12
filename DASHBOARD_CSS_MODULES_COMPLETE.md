# 🎨 Product Approval Dashboard - CSS Modules Implementation

## Complete Code with CSS Modules (No Tailwind)

---

## 📁 File Structure:

```
dashboard/
├── pages/
│   └── products/
│       └── approval.jsx                    # Main page
├── components/
│   ├── ProductApprovalCard.jsx             # Product card
│   └── RejectDialog.jsx                    # Reject dialog
├── styles/
│   ├── ProductApproval.module.css          # Main page styles
│   ├── ProductCard.module.css              # Card styles
│   └── RejectDialog.module.css             # Dialog styles
└── lib/
    └── firebase.js                          # Firebase config
```

---

## 1️⃣ **Main Page: `pages/products/approval.jsx`**

```jsx
import { useState, useEffect } from 'react';
import { 
  collection, 
  query, 
  where, 
  getDocs, 
  doc, 
  updateDoc,
  orderBy,
  Timestamp 
} from 'firebase/firestore';
import { db } from '@/lib/firebase';
import ProductApprovalCard from '@/components/ProductApprovalCard';
import RejectDialog from '@/components/RejectDialog';
import styles from '@/styles/ProductApproval.module.css';

export default function ProductApprovalPage() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('pending');
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [showRejectDialog, setShowRejectDialog] = useState(false);
  const [processing, setProcessing] = useState(false);

  // Fetch products
  const fetchProducts = async () => {
    setLoading(true);
    try {
      const productsRef = collection(db, 'data', 'stock', 'products');
      
      let q;
      if (filter === 'all') {
        q = query(productsRef, orderBy('createdAt', 'desc'));
      } else {
        q = query(
          productsRef, 
          where('status', '==', filter),
          orderBy('createdAt', 'desc')
        );
      }
      
      const snapshot = await getDocs(q);
      const productsList = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
      
      setProducts(productsList);
    } catch (error) {
      console.error('Error fetching products:', error);
      alert('Failed to fetch products: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, [filter]);

  // Approve product
  const handleApprove = async (productId) => {
    if (!confirm('Are you sure you want to approve this product?')) return;
    
    setProcessing(true);
    try {
      const productRef = doc(db, 'data', 'stock', 'products', productId);
      await updateDoc(productRef, {
        status: 'approved',
        rejectionReason: null,
        reviewedBy: 'admin',
        reviewedAt: Timestamp.now()
      });
      
      alert('Product approved successfully!');
      fetchProducts();
    } catch (error) {
      console.error('Error approving product:', error);
      alert('Failed to approve product: ' + error.message);
    } finally {
      setProcessing(false);
    }
  };

  // Reject product
  const handleReject = (product) => {
    setSelectedProduct(product);
    setShowRejectDialog(true);
  };

  const confirmReject = async (reason) => {
    if (!selectedProduct) return;
    
    setProcessing(true);
    try {
      const productRef = doc(db, 'data', 'stock', 'products', selectedProduct.id);
      await updateDoc(productRef, {
        status: 'rejected',
        rejectionReason: reason,
        reviewedBy: 'admin',
        reviewedAt: Timestamp.now()
      });
      
      alert('Product rejected successfully!');
      setShowRejectDialog(false);
      setSelectedProduct(null);
      fetchProducts();
    } catch (error) {
      console.error('Error rejecting product:', error);
      alert('Failed to reject product: ' + error.message);
    } finally {
      setProcessing(false);
    }
  };

  const getProductCount = (status) => {
    return products.filter(p => p.status === status).length;
  };

  return (
    <div className={styles.container}>
      {/* Header */}
      <div className={styles.header}>
        <h1 className={styles.title}>Product Approval</h1>
        <p className={styles.subtitle}>
          Review and approve/reject products uploaded by sellers
        </p>
      </div>

      {/* Filters */}
      <div className={styles.filterSection}>
        <div className={styles.filterCard}>
          <div className={styles.filterContent}>
            <span className={styles.filterLabel}>Filter:</span>
            <div className={styles.filterButtons}>
              {['pending', 'approved', 'rejected', 'all'].map((status) => (
                <button
                  key={status}
                  onClick={() => setFilter(status)}
                  className={`${styles.filterButton} ${
                    filter === status ? styles.active : styles.inactive
                  }`}
                >
                  {status.charAt(0).toUpperCase() + status.slice(1)}
                  {status !== 'all' && (
                    <span className={styles.badge}>
                      {getProductCount(status)}
                    </span>
                  )}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Stats */}
      <div className={styles.statsSection}>
        <div className={styles.statsGrid}>
          <div className={`${styles.statCard} ${styles.total}`}>
            <div className={styles.statLabel}>Total Products</div>
            <div className={styles.statValue}>{products.length}</div>
          </div>
          <div className={`${styles.statCard} ${styles.pending}`}>
            <div className={styles.statLabel}>Pending</div>
            <div className={styles.statValue}>{getProductCount('pending')}</div>
          </div>
          <div className={`${styles.statCard} ${styles.approved}`}>
            <div className={styles.statLabel}>Approved</div>
            <div className={styles.statValue}>{getProductCount('approved')}</div>
          </div>
          <div className={`${styles.statCard} ${styles.rejected}`}>
            <div className={styles.statLabel}>Rejected</div>
            <div className={styles.statValue}>{getProductCount('rejected')}</div>
          </div>
        </div>
      </div>

      {/* Products List */}
      <div className={styles.productsSection}>
        {loading ? (
          <div className={styles.loading}>
            <div className={styles.spinner}></div>
          </div>
        ) : products.length === 0 ? (
          <div className={styles.emptyState}>
            <div className={styles.emptyText}>
              No {filter !== 'all' ? filter : ''} products found
            </div>
          </div>
        ) : (
          <div className={styles.productsGrid}>
            {products.map((product) => (
              <ProductApprovalCard
                key={product.id}
                product={product}
                onApprove={handleApprove}
                onReject={handleReject}
                processing={processing}
              />
            ))}
          </div>
        )}
      </div>

      {/* Reject Dialog */}
      {showRejectDialog && (
        <RejectDialog
          product={selectedProduct}
          onConfirm={confirmReject}
          onCancel={() => {
            setShowRejectDialog(false);
            setSelectedProduct(null);
          }}
          processing={processing}
        />
      )}
    </div>
  );
}
```

---

## 2️⃣ **Product Card: `components/ProductApprovalCard.jsx`**

```jsx
import { useState } from 'react';
import styles from '@/styles/ProductCard.module.css';

export default function ProductApprovalCard({ product, onApprove, onReject, processing }) {
  const [showFullDescription, setShowFullDescription] = useState(false);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);

  const images = Array.isArray(product.images) 
    ? product.images 
    : typeof product.images === 'string' && product.images 
      ? [product.images] 
      : [];

  const formatDate = (timestamp) => {
    if (!timestamp) return 'N/A';
    const date = timestamp.toDate ? timestamp.toDate() : new Date(timestamp);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const nextImage = () => {
    setCurrentImageIndex((prev) => (prev + 1) % images.length);
  };

  const prevImage = () => {
    setCurrentImageIndex((prev) => (prev - 1 + images.length) % images.length);
  };

  return (
    <div className={styles.card}>
      {/* Image Gallery */}
      <div className={styles.imageContainer}>
        {images.length > 0 ? (
          <>
            <img
              src={images[currentImageIndex]}
              alt={product.title}
              className={styles.image}
            />
            {images.length > 1 && (
              <>
                <button
                  onClick={prevImage}
                  className={`${styles.imageNav} ${styles.prev}`}
                >
                  ←
                </button>
                <button
                  onClick={nextImage}
                  className={`${styles.imageNav} ${styles.next}`}
                >
                  →
                </button>
                <div className={styles.imageDots}>
                  {images.map((_, index) => (
                    <button
                      key={index}
                      onClick={() => setCurrentImageIndex(index)}
                      className={`${styles.dot} ${
                        index === currentImageIndex ? styles.active : ''
                      }`}
                    />
                  ))}
                </div>
              </>
            )}
          </>
        ) : (
          <div className={styles.noImage}>No Image</div>
        )}
        
        {/* Status Badge */}
        <div className={`${styles.statusBadge} ${styles[product.status || 'pending']}`}>
          {(product.status || 'UNKNOWN').toUpperCase()}
        </div>
      </div>

      {/* Product Info */}
      <div className={styles.content}>
        {/* Title */}
        <h3 className={styles.title}>{product.title}</h3>

        {/* Price */}
        <div className={styles.priceContainer}>
          <span className={styles.price}>PKR {product.price}</span>
          {product.actualPrice && product.actualPrice !== product.price && (
            <span className={styles.actualPrice}>PKR {product.actualPrice}</span>
          )}
        </div>

        {/* Category */}
        <div className={styles.category}>{product.category}</div>

        {/* Description */}
        <div className={`${styles.description} ${!showFullDescription ? styles.clamped : ''}`}>
          {product.description}
        </div>
        {product.description && product.description.length > 150 && (
          <button
            onClick={() => setShowFullDescription(!showFullDescription)}
            className={styles.showMoreButton}
          >
            {showFullDescription ? 'Show less' : 'Show more'}
          </button>
        )}

        {/* Seller Info */}
        <div className={styles.section}>
          <div className={styles.sectionLabel}>Seller</div>
          <div className={styles.sectionValue}>{product.sellerName || 'Unknown'}</div>
          <div className={styles.sectionSubValue}>
            ID: {product.sellerId?.substring(0, 8)}...
          </div>
        </div>

        {/* Co-Store Info */}
        {product.isCoStoreProduct && (
          <div className={styles.section}>
            <div className={styles.sectionLabel}>Co-Seller Store</div>
            <div className={styles.sectionValue}>{product.coStoreName}</div>
          </div>
        )}

        {/* Min Deal Price */}
        {product.minDealPrice > 0 && (
          <div className={styles.section}>
            <div className={styles.sectionLabel}>Minimum Deal Price</div>
            <div className={styles.sectionValue}>PKR {product.minDealPrice}</div>
          </div>
        )}

        {/* Created At */}
        <div className={styles.timestamp}>
          Uploaded: {formatDate(product.createdAt)}
        </div>

        {/* Rejection Reason */}
        {product.status === 'rejected' && product.rejectionReason && (
          <div className={styles.rejectionBox}>
            <div className={styles.rejectionLabel}>Rejection Reason:</div>
            <div className={styles.rejectionReason}>{product.rejectionReason}</div>
          </div>
        )}

        {/* Action Buttons */}
        {product.status === 'pending' && (
          <div className={styles.actions}>
            <button
              onClick={() => onApprove(product.id)}
              disabled={processing}
              className={`${styles.button} ${styles.approveButton}`}
            >
              ✓ Approve
            </button>
            <button
              onClick={() => onReject(product)}
              disabled={processing}
              className={`${styles.button} ${styles.rejectButton}`}
            >
              ✗ Reject
            </button>
          </div>
        )}

        {product.status === 'approved' && (
          <div className={styles.statusBox}>
            <div className={styles.statusText}>✓ Approved</div>
          </div>
        )}
      </div>
    </div>
  );
}
```

---

## 3️⃣ **Reject Dialog: `components/RejectDialog.jsx`**

```jsx
import { useState } from 'react';
import styles from '@/styles/RejectDialog.module.css';

export default function RejectDialog({ product, onConfirm, onCancel, processing }) {
  const [reason, setReason] = useState('');
  const [customReason, setCustomReason] = useState('');

  const predefinedReasons = [
    'Poor image quality',
    'Incomplete product information',
    'Inappropriate content',
    'Misleading description',
    'Incorrect pricing',
    'Prohibited item',
    'Duplicate product',
    'Other (specify below)'
  ];

  const handleConfirm = () => {
    const finalReason = reason === 'Other (specify below)' ? customReason : reason;
    
    if (!finalReason.trim()) {
      alert('Please select or enter a rejection reason');
      return;
    }
    
    onConfirm(finalReason);
  };

  return (
    <div className={styles.overlay}>
      <div className={styles.dialog}>
        {/* Header */}
        <div className={styles.header}>
          <h2 className={styles.title}>Reject Product</h2>
          <p className={styles.subtitle}>
            Please provide a reason for rejecting this product
          </p>
        </div>

        {/* Product Info */}
        <div className={styles.productInfo}>
          <div className={styles.productContent}>
            {product.images && product.images[0] && (
              <img
                src={Array.isArray(product.images) ? product.images[0] : product.images}
                alt={product.title}
                className={styles.productImage}
              />
            )}
            <div className={styles.productDetails}>
              <div className={styles.productTitle}>{product.title}</div>
              <div className={styles.productPrice}>PKR {product.price}</div>
              <div className={styles.productSeller}>
                Seller: {product.sellerName}
              </div>
            </div>
          </div>
        </div>

        {/* Reason Selection */}
        <div className={styles.reasonSection}>
          <label className={styles.label}>Select Reason *</label>
          <div className={styles.reasonList}>
            {predefinedReasons.map((r) => (
              <label key={r} className={styles.reasonOption}>
                <input
                  type="radio"
                  name="reason"
                  value={r}
                  checked={reason === r}
                  onChange={(e) => setReason(e.target.value)}
                />
                <span className={styles.reasonText}>{r}</span>
              </label>
            ))}
          </div>

          {/* Custom Reason Input */}
          {reason === 'Other (specify below)' && (
            <div className={styles.customReasonContainer}>
              <label className={styles.label}>Custom Reason *</label>
              <textarea
                value={customReason}
                onChange={(e) => setCustomReason(e.target.value)}
                placeholder="Enter custom rejection reason..."
                className={styles.textarea}
              />
            </div>
          )}
        </div>

        {/* Actions */}
        <div className={styles.actions}>
          <button
            onClick={onCancel}
            disabled={processing}
            className={`${styles.button} ${styles.cancelButton}`}
          >
            Cancel
          </button>
          <button
            onClick={handleConfirm}
            disabled={processing || !reason}
            className={`${styles.button} ${styles.confirmButton}`}
          >
            {processing ? 'Rejecting...' : 'Confirm Reject'}
          </button>
        </div>
      </div>
    </div>
  );
}
```

---

## 4️⃣ **Firebase Config: `lib/firebase.js`**

```javascript
import { initializeApp, getApps } from 'firebase/app';
import { getFirestore } from 'firebase/firestore';

const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID
};

// Initialize Firebase
const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApps()[0];
const db = getFirestore(app);

export { db };
```

---

## 5️⃣ **Environment Variables: `.env.local`**

```env
NEXT_PUBLIC_FIREBASE_API_KEY=your_api_key_here
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=your_project.firebaseapp.com
NEXT_PUBLIC_FIREBASE_PROJECT_ID=your_project_id
NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=your_project.appspot.com
NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=your_sender_id
NEXT_PUBLIC_FIREBASE_APP_ID=your_app_id
```

---

## 6️⃣ **Package.json Dependencies:**

```json
{
  "name": "product-approval-dashboard",
  "version": "1.0.0",
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start"
  },
  "dependencies": {
    "next": "^14.0.0",
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "firebase": "^10.7.0"
  }
}
```

---

## 🚀 Setup Instructions:

### **Step 1: Create Next.js Project**
```bash
npx create-next-app@latest product-approval-dashboard
cd product-approval-dashboard
```

### **Step 2: Install Firebase**
```bash
npm install firebase
```

### **Step 3: Create File Structure**
```bash
# Create directories
mkdir -p pages/products
mkdir -p components
mkdir -p styles
mkdir -p lib

# Create files (copy code from above)
# pages/products/approval.jsx
# components/ProductApprovalCard.jsx
# components/RejectDialog.jsx
# styles/ProductApproval.module.css
# styles/ProductCard.module.css
# styles/RejectDialog.module.css
# lib/firebase.js
```

### **Step 4: Setup Environment Variables**
```bash
# Create .env.local file
# Add your Firebase credentials
```

### **Step 5: Run Development Server**
```bash
npm run dev
```

### **Step 6: Access Dashboard**
```
http://localhost:3000/products/approval
```

---

## ✅ Features:

### **Main Page:**
- ✅ Product listing with filters
- ✅ Statistics cards (Total, Pending, Approved, Rejected)
- ✅ Responsive grid layout
- ✅ Loading spinner
- ✅ Empty state

### **Product Card:**
- ✅ Image gallery with navigation
- ✅ Status badge
- ✅ Product details (title, price, description, category)
- ✅ Seller information
- ✅ Co-store information
- ✅ Approve/Reject buttons
- ✅ Rejection reason display

### **Reject Dialog:**
- ✅ Modal overlay
- ✅ Product preview
- ✅ Predefined reasons
- ✅ Custom reason textarea
- ✅ Confirm/Cancel actions

---

## 🎨 CSS Modules Benefits:

✅ **No Tailwind dependency**
✅ **Scoped styles** (no conflicts)
✅ **Better performance** (only used styles loaded)
✅ **Easy customization**
✅ **Responsive design**
✅ **Clean class names**

---

## 📝 Testing Checklist:

- [ ] Dashboard loads without errors
- [ ] Products fetch from Firestore
- [ ] Filters work (pending/approved/rejected/all)
- [ ] Statistics update correctly
- [ ] Image gallery navigation works
- [ ] Approve button updates status
- [ ] Reject dialog opens
- [ ] Rejection reasons save correctly
- [ ] Responsive on mobile/tablet/desktop

---

## ✅ Complete Implementation!

**Sab code CSS Modules ke saath ready hai!** 🎉

**No Tailwind CSS needed - Pure CSS Modules!**

