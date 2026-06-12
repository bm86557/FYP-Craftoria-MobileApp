# Chat System Implementation - Complete Guide

## ✅ Files Created

### 1. Model Files
- **ChatModel.kt** - Data models for Chat and Message
- **ChatViewModel.kt** - ViewModel for managing chat operations

### 2. UI Files
- **ChatsListPage.kt** - Shows all user chats with unread badges
- **ChatScreen.kt** - Real-time messaging screen

### 3. Updated Files
- **ProductDetailPage.kt** - Added "Chat with Seller" button
- **ProfilePage.kt** - Added "Messages" card
- **AppNavigation.kt** - Added chat routes

---

## 🗂️ Database Structure

### Firestore Collections

#### Collection: `chats`
```
Document ID: Auto-generated
{
    "chatId": "string",
    "participants": ["buyerId", "sellerId"],
    "productId": "string",
    "productName": "string",
    "productImage": "string",
    "lastMessage": "string",
    "lastMessageTime": Timestamp,
    "lastMessageSenderId": "string",
    "unreadCount": {
        "buyerId": 0,
        "sellerId": 2
    },
    "createdAt": Timestamp
}
```

#### Subcollection: `chats/{chatId}/messages`
```
Document ID: Auto-generated
{
    "messageId": "string",
    "senderId": "string",
    "senderName": "string",
    "text": "string",
    "timestamp": Timestamp,
    "isRead": false
}
```

---

## 🎯 Features Implemented

### 1. **Product Detail Page**
- ✅ "Chat with Seller" button added
- ✅ Only shows if product is not owned by current user
- ✅ Creates new chat or opens existing chat
- ✅ Navigates to ChatScreen

### 2. **Profile Page**
- ✅ "Messages" card added below Wallet
- ✅ Shows description "Chat with buyers/sellers"
- ✅ Navigates to ChatsListPage

### 3. **Chats List Page**
- ✅ Shows all user chats (buyer & seller)
- ✅ Displays product image, name, last message
- ✅ Shows unread count badge
- ✅ Real-time updates via Firestore listener
- ✅ Time formatting (Just now, 5m ago, etc.)
- ✅ Empty state with friendly message

### 4. **Chat Screen**
- ✅ Real-time messaging
- ✅ Message bubbles (different colors for sender/receiver)
- ✅ Auto-scroll to latest message
- ✅ Shows sender name and timestamp
- ✅ Marks chat as read when opened
- ✅ Updates unread count
- ✅ Empty state with "Start conversation" message

---

## 🔄 User Flow

### Buyer Flow:
1. Browse products → Product Detail Page
2. Click "Chat with Seller" button
3. Opens ChatScreen (creates chat if new)
4. Send messages to seller
5. View all chats in Profile → Messages

### Seller Flow:
1. Receive message notification (unread badge)
2. Go to Profile → Messages
3. See all chats with unread counts
4. Click on chat → Opens ChatScreen
5. Reply to buyer

---

## 🎨 UI Components

### ChatsListPage
- Product image thumbnail
- Product name
- Last message preview
- Time stamp
- Unread badge (red)
- Empty state

### ChatScreen
- Top bar with other user name
- Message bubbles (rounded corners)
- Sender name (for received messages)
- Timestamp
- Text input field
- Send button (enabled only when text exists)
- Auto-scroll to bottom

---

## 🔧 Key Functions

### ChatViewModel

#### `loadUserChats()`
- Loads all chats where user is participant
- Real-time listener
- Calculates total unread count

#### `loadMessages(chatId)`
- Loads all messages for specific chat
- Real-time listener
- Ordered by timestamp

#### `getOrCreateChat()`
- Checks if chat exists between buyer-seller for product
- Creates new chat if doesn't exist
- Returns chatId

#### `sendMessage()`
- Adds message to subcollection
- Updates chat document (lastMessage, timestamp)
- Increments unread count for receiver

#### `markChatAsRead()`
- Resets unread count to 0 for current user
- Called when chat is opened

---

## 🚀 Navigation Routes

```kotlin
// Chats List
"chats_list" → ChatsListPage

// Chat Screen
"chat_screen/{chatId}/{receiverId}" → ChatScreen
```

---

## 📱 Testing Checklist

### Buyer Side:
- [ ] Open product detail page
- [ ] Click "Chat with Seller" button
- [ ] Send message
- [ ] Check message appears in chat
- [ ] Go to Profile → Messages
- [ ] Verify chat appears in list

### Seller Side:
- [ ] Go to Profile → Messages
- [ ] See unread badge on chat
- [ ] Open chat
- [ ] Verify unread badge disappears
- [ ] Reply to message
- [ ] Check real-time update

### Both Sides:
- [ ] Send multiple messages
- [ ] Check auto-scroll works
- [ ] Check timestamps are correct
- [ ] Check message bubbles are different colors
- [ ] Test empty states
- [ ] Test with multiple chats

---

## 🐛 Potential Issues & Solutions

### Issue 1: Chat not creating
**Solution:** Check Firebase permissions, ensure user is authenticated

### Issue 2: Messages not showing
**Solution:** Check Firestore listeners are active, verify collection names

### Issue 3: Unread count not updating
**Solution:** Ensure `markChatAsRead()` is called in LaunchedEffect

### Issue 4: Images not loading
**Solution:** Coil is already added, check image URLs are valid

---

## 🔐 Security Rules (Firestore)

Add these rules to Firebase Console:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Chats collection
    match /chats/{chatId} {
      allow read: if request.auth != null && 
                     request.auth.uid in resource.data.participants;
      allow create: if request.auth != null;
      allow update: if request.auth != null && 
                       request.auth.uid in resource.data.participants;
      
      // Messages subcollection
      match /messages/{messageId} {
        allow read: if request.auth != null && 
                       request.auth.uid in get(/databases/$(database)/documents/chats/$(chatId)).data.participants;
        allow create: if request.auth != null && 
                         request.auth.uid in get(/databases/$(database)/documents/chats/$(chatId)).data.participants;
      }
    }
  }
}
```

---

## ✨ Future Enhancements

1. **Image Sharing** - Allow users to send images
2. **Typing Indicator** - Show when other user is typing
3. **Read Receipts** - Show when message is read
4. **Push Notifications** - Notify users of new messages
5. **Message Search** - Search within chat history
6. **Delete Messages** - Allow users to delete messages
7. **Block User** - Block unwanted users
8. **Report Chat** - Report inappropriate content

---

## 📝 Notes

- All chat data is stored in Firestore
- Real-time updates using Firestore listeners
- Unread counts are maintained per user
- Chat is created only when first message is sent (or button clicked)
- Product context is maintained in chat
- Works for both buyer and seller roles

---

## 🎉 Implementation Complete!

The chat system is now fully integrated and ready to use. Test thoroughly before deploying to production.
