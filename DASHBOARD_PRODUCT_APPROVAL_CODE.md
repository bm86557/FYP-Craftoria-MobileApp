# 🎨 Next.js Dashboard - Product Approval Screen

## Complete Implementation Code

---

## 📁 File Structure:

```
dashboard/
├── pages/
│   └── products/
│       └── approval.jsx          # Main approval page
├── components/
│   ├── ProductApprovalCard.jsx   # Product card component
│   └── RejectDialog.jsx          # Rejection dialog
└── lib/
    └── firebase.js                # Firebase config
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

export default function ProductApprovalPage() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('pending'); // 'pending' | 'approved' | 'rejected' | 'all'
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
        reviewedBy: 'admin', // Replace with actual admin ID
        reviewedAt: Timestamp.now()
      });
      
      alert('Product approved successfully!');
      fetchProducts(); // Refresh list
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
        reviewedBy: 'admin', // Replace with actual admin ID
        reviewedAt: Timestamp.now()
      });
      
      alert('Product rejected successfully!');
      setShowRejectDialog(false);
      setSelectedProduct(null);
      fetchProducts(); // Refresh list
    } catch (error) {
      console.error('Error rejecting product:', error);
      alert('Failed to reject product: ' + error.message);
    } finally {
      setProcessing(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      {/* Header */}
      <div className="max-w-7xl mx-auto mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          Product Approval
        </h1>
        <p className="text-gray-600">
          Review and approve/reject products uploaded by sellers
        </p>
      </div>

      {/* Filters */}
      <div className="max-w-7xl mx-auto mb-6">
        <div className="bg-white rounded-lg shadow p-4">
          <div className="flex items-center gap-4">
            <span className="text-sm font-medium text-gray-700">Filter:</span>
            <div className="flex gap-2">
              {['pending', 'approved', 'rejected', 'all'].map((status) => (
                <button
                  key={status}
                  onClick={() => setFilter(status)}
                  className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                    filter === status
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  {status.charAt(0).toUpperCase() + status.slice(1)}
                  {status !== 'all' && (
                    <span className="ml-2 px-2 py-0.5 rounded-full text-xs bg-white/20">
                      {products.filter(p => p.status === status).length}
                    </span>
                  )}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Stats */}
      <div className="max-w-7xl mx-auto mb-6">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-white rounded-lg shadow p-6">
            <div className="text-sm font-medium text-gray-600">Total Products</div>
            <div className="text-2xl font-bold text-gray-900 mt-2">
              {products.length}
            </div>
          </div>
          <div className="bg-yellow-50 rounded-lg shadow p-6">
            <div className="text-sm font-medium text-yellow-800">Pending</div>
            <div className="text-2xl font-bold text-yellow-900 mt-2">
              {products.filter(p => p.status === 'pending').length}
            </div>
          </div>
          <div className="bg-green-50 rounded-lg shadow p-6">
            <div className="text-sm font-medium text-green-800">Approved</div>
            <div className="text-2xl font-bold text-green-900 mt-2">
              {products.filter(p => p.status === 'approved').length}
            </div>
          </div>
          <div className="bg-red-50 rounded-lg shadow p-6">
            <div className="text-sm font-medium text-red-800">Rejected</div>
            <div className="text-2xl font-bold text-red-900 mt-2">
              {products.filter(p => p.status === 'rejected').length}
            </div>
          </div>
        </div>
      </div>

      {/* Products List */}
      <div className="max-w-7xl mx-auto">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          </div>
        ) : products.length === 0 ? (
          <div className="bg-white rounded-lg shadow p-12 text-center">
            <div className="text-gray-400 text-lg">
              No {filter !== 'all' ? filter : ''} products found
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
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

## 2️⃣ **Product Card Component: `components/ProductApprovalCard.jsx`**

```jsx
import { useState } from 'react';
import Image from 'next/image';

export default function ProductApprovalCard({ product, onApprove, onReject, processing }) {
  const [showFullDescription, setShowFullDescription] = useState(false);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);

  const images = Array.isArray(product.images) 
    ? product.images 
    : typeof product.images === 'string' && product.images 
      ? [product.images] 
      : [];

  const getStatusColor = (status) => {
    switch (status) {
      case 'pending':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'approved':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'rejected':
        return 'bg-red-100 text-red-800 border-red-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

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

  return (
    <div className="bg-white rounded-lg shadow-lg overflow-hidden hover:shadow-xl transition-shadow">
      {/* Image Gallery */}
      <div className="relative h-64 bg-gray-100">
        {images.length > 0 ? (
          <>
            <img
              src={images[currentImageIndex]}
              alt={product.title}
              className="w-full h-full object-cover"
            />
            {images.length > 1 && (
              <div className="absolute bottom-2 left-0 right-0 flex justify-center gap-2">
                {images.map((_, index) => (
                  <button
                    key={index}
                    onClick={() => setCurrentImageIndex(index)}
                    className={`w-2 h-2 rounded-full transition-all ${
                      index === currentImageIndex
                        ? 'bg-white w-6'
                        : 'bg-white/50 hover:bg-white/75'
                    }`}
                  />
                ))}
              </div>
            )}
            {images.length > 1 && (
              <>
                <button
                  onClick={() => setCurrentImageIndex((prev) => (prev - 1 + images.length) % images.length)}
                  className="absolute left-2 top-1/2 -translate-y-1/2 bg-black/50 hover:bg-black/70 text-white p-2 rounded-full"
                >
                  ←
                </button>
                <button
                  onClick={() => setCurrentImageIndex((prev) => (prev + 1) % images.length)}
                  className="absolute right-2 top-1/2 -translate-y-1/2 bg-black/50 hover:bg-black/70 text-white p-2 rounded-full"
                >
                  →
                </button>
              </>
            )}
          </>
        ) : (
          <div className="w-full h-full flex items-center justify-center text-gray-400">
            No Image
          </div>
        )}
        
        {/* Status Badge */}
        <div className="absolute top-2 right-2">
          <span className={`px-3 py-1 rounded-full text-xs font-semibold border ${getStatusColor(product.status)}`}>
            {product.status?.toUpperCase() || 'UNKNOWN'}
          </span>
        </div>
      </div>

      {/* Product Info */}
      <div className="p-4">
        {/* Title */}
        <h3 className="text-lg font-bold text-gray-900 mb-2 line-clamp-2">
          {product.title}
        </h3>

        {/* Price */}
        <div className="flex items-center gap-2 mb-3">
          <span className="text-2xl font-bold text-blue-600">
            PKR {product.price}
          </span>
          {product.actualPrice && product.actualPrice !== product.price && (
            <span className="text-sm text-gray-500 line-through">
              PKR {product.actualPrice}
            </span>
          )}
        </div>

        {/* Category */}
        <div className="mb-3">
          <span className="inline-block px-2 py-1 bg-gray-100 text-gray-700 text-xs rounded">
            {product.category}
          </span>
        </div>

        {/* Description */}
        <div className="mb-3">
          <p className={`text-sm text-gray-600 ${!showFullDescription && 'line-clamp-3'}`}>
            {product.description}
          </p>
          {product.description && product.description.length > 150 && (
            <button
              onClick={() => setShowFullDescription(!showFullDescription)}
              className="text-blue-600 text-xs hover:underline mt-1"
            >
              {showFullDescription ? 'Show less' : 'Show more'}
            </button>
          )}
        </div>

        {/* Seller Info */}
        <div className="border-t pt-3 mb-3">
          <div className="text-xs text-gray-500 mb-1">Seller</div>
          <div className="text-sm font-medium text-gray-900">{product.sellerName || 'Unknown'}</div>
          <div className="text-xs text-gray-500">ID: {product.sellerId?.substring(0, 8)}...</div>
        </div>

        {/* Co-Store Info (if applicable) */}
        {product.isCoStoreProduct && (
          <div className="border-t pt-3 mb-3">
            <div className="text-xs text-gray-500 mb-1">Co-Seller Store</div>
            <div className="text-sm font-medium text-gray-900">{product.coStoreName}</div>
          </div>
        )}

        {/* Min Deal Price */}
        {product.minDealPrice > 0 && (
          <div className="mb-3">
            <div className="text-xs text-gray-500">Minimum Deal Price</div>
            <div className="text-sm font-medium text-gray-900">PKR {product.minDealPrice}</div>
          </div>
        )}

        {/* Created At */}
        <div className="text-xs text-gray-500 mb-3">
          Uploaded: {formatDate(product.createdAt)}
        </div>

        {/* Rejection Reason (if rejected) */}
        {product.status === 'rejected' && product.rejectionReason && (
          <div className="bg-red-50 border border-red-200 rounded p-3 mb-3">
            <div className="text-xs font-semibold text-red-800 mb-1">Rejection Reason:</div>
            <div className="text-sm text-red-700">{product.rejectionReason}</div>
          </div>
        )}

        {/* Action Buttons */}
        {product.status === 'pending' && (
          <div className="flex gap-2">
            <button
              onClick={() => onApprove(product.id)}
              disabled={processing}
              className="flex-1 bg-green-600 hover:bg-green-700 disabled:bg-gray-400 text-white font-medium py-2 px-4 rounded-lg transition-colors"
            >
              ✓ Approve
            </button>
            <button
              onClick={() => onReject(product)}
              disabled={processing}
              className="flex-1 bg-red-600 hover:bg-red-700 disabled:bg-gray-400 text-white font-medium py-2 px-4 rounded-lg transition-colors"
            >
              ✗ Reject
            </button>
          </div>
        )}

        {product.status === 'approved' && (
          <div className="bg-green-50 border border-green-200 rounded p-3 text-center">
            <div className="text-sm font-medium text-green-800">✓ Approved</div>
          </div>
        )}
      </div>
    </div>
  );
}
```

---

## 3️⃣ **Reject Dialog Component: `components/RejectDialog.jsx`**

```jsx
import { useState } from 'react';

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
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-md w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="p-6 border-b">
          <h2 className="text-xl font-bold text-gray-900">Reject Product</h2>
          <p className="text-sm text-gray-600 mt-1">
            Please provide a reason for rejecting this product
          </p>
        </div>

        {/* Product Info */}
        <div className="p-6 border-b bg-gray-50">
          <div className="flex gap-4">
            {product.images && product.images[0] && (
              <img
                src={Array.isArray(product.images) ? product.images[0] : product.images}
                alt={product.title}
                className="w-20 h-20 object-cover rounded"
              />
            )}
            <div className="flex-1">
              <div className="font-medium text-gray-900">{product.title}</div>
              <div className="text-sm text-gray-600">PKR {product.price}</div>
              <div className="text-xs text-gray-500 mt-1">
                Seller: {product.sellerName}
              </div>
            </div>
          </div>
        </div>

        {/* Reason Selection */}
        <div className="p-6">
          <label className="block text-sm font-medium text-gray-700 mb-3">
            Select Reason *
          </label>
          <div className="space-y-2">
            {predefinedReasons.map((r) => (
              <label
                key={r}
                className="flex items-center p-3 border rounded-lg cursor-pointer hover:bg-gray-50 transition-colors"
              >
                <input
                  type="radio"
                  name="reason"
                  value={r}
                  checked={reason === r}
                  onChange={(e) => setReason(e.target.value)}
                  className="mr-3"
                />
                <span className="text-sm text-gray-700">{r}</span>
              </label>
            ))}
          </div>

          {/* Custom Reason Input */}
          {reason === 'Other (specify below)' && (
            <div className="mt-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Custom Reason *
              </label>
              <textarea
                value={customReason}
                onChange={(e) => setCustomReason(e.target.value)}
                placeholder="Enter custom rejection reason..."
                rows={4}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
              />
            </div>
          )}
        </div>

        {/* Actions */}
        <div className="p-6 border-t bg-gray-50 flex gap-3">
          <button
            onClick={onCancel}
            disabled={processing}
            className="flex-1 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 font-medium hover:bg-gray-100 disabled:opacity-50 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleConfirm}
            disabled={processing || !reason}
            className="flex-1 px-4 py-2 bg-red-600 hover:bg-red-700 disabled:bg-gray-400 text-white font-medium rounded-lg transition-colors"
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
NEXT_PUBLIC_FIREBASE_API_KEY=your_api_key
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=your_project.firebaseapp.com
NEXT_PUBLIC_FIREBASE_PROJECT_ID=your_project_id
NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=your_project.appspot.com
NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=your_sender_id
NEXT_PUBLIC_FIREBASE_APP_ID=your_app_id
```

---

## 6️⃣ **Package Dependencies:**

```json
{
  "dependencies": {
    "next": "^14.0.0",
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "firebase": "^10.7.0"
  },
  "devDependencies": {
    "tailwindcss": "^3.3.0",
    "autoprefixer": "^10.4.16",
    "postcss": "^8.4.32"
  }
}
```

---

## 🎨 Features Implemented:

### ✅ **Product Listing:**
- View all products with filters (pending/approved/rejected/all)
- Grid layout with responsive design
- Image gallery with navigation
- Product details display

### ✅ **Approval Actions:**
- Approve button (green)
- Reject button with reason dialog (red)
- Confirmation dialogs
- Loading states

### ✅ **Rejection Reasons:**
- Predefined reasons dropdown
- Custom reason textarea
- Reason saved to Firestore

### ✅ **Statistics:**
- Total products count
- Pending count
- Approved count
- Rejected count

### ✅ **Product Information:**
- Title, description, price
- Images (multiple with gallery)
- Category
- Seller information
- Co-store information (if applicable)
- Upload date
- Rejection reason (if rejected)

---

## 🚀 How to Use:

1. **Install dependencies:**
   ```bash
   npm install
   ```

2. **Setup environment variables:**
   - Create `.env.local` file
   - Add Firebase config

3. **Run development server:**
   ```bash
   npm run dev
   ```

4. **Access dashboard:**
   ```
   http://localhost:3000/products/approval
   ```

---

## 📝 Testing:

1. Upload product from Android app
2. Check dashboard - product should appear in "Pending"
3. Click "Approve" - product moves to "Approved"
4. Click "Reject" - dialog opens
5. Select reason - product moves to "Rejected"
6. Check Android app - only approved products visible

---

## ✅ Complete Implementation Ready!

**Dashboard code complete hai. Copy paste karke use kar sakte hain!** 🎉
