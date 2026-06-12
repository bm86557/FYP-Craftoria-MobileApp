package com.example.myapplication.model

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout

/**
 * Bug Condition Exploration Test
 * 
 * **Validates: Requirements 1.1, 1.2, 1.3**
 * 
 * **Property 1: Bug Condition** - Flow Collection Doesn't Persist
 * 
 * CRITICAL: This test MUST FAIL on unfixed code - failure confirms the bug exists
 * 
 * This test demonstrates that when loadBuyerOrders() or loadSellerOrders() is called,
 * the order data does NOT persist in the StateFlow due to improper flow collection lifecycle.
 * 
 * Expected behavior (what the test checks for):
 * - Order data should persist in StateFlow after initial load
 * - Flow collection coroutine should remain active
 * - Firestore listener should remain registered
 * 
 * On UNFIXED code, this test will FAIL because:
 * - StateFlow emits order data once, then may emit empty list
 * - Flow collection coroutine completes prematurely
 * - Firestore listener may be removed too early
 */
class OrdersViewModelBugConditionTest : StringSpec({
    
    "Property 1: Bug Condition - Flow collection persists for buyer orders" {
        checkAll(10, Arb.list(Arb.string(), 1..5)) { orderIds ->
            // Setup mocks
            val mockAuth = mockk<FirebaseAuth>()
            val mockUser = mockk<FirebaseUser>()
            val mockDb = mockk<FirebaseFirestore>()
            val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>()
            val mockQuery = mockk<Query>()
            val mockSnapshot = mockk<QuerySnapshot>()
            val mockListenerRegistration = mockk<ListenerRegistration>()
            
            // Mock Firebase Auth
            every { mockAuth.currentUser } returns mockUser
            every { mockUser.uid } returns "test-buyer-id"
            
            // Mock Firestore query chain
            every { mockDb.collection("orders") } returns mockCollection
            every { mockCollection.whereEqualTo("buyerId", any<String>()) } returns mockQuery
            every { mockQuery.orderBy("createdAt", Query.Direction.DESCENDING) } returns mockQuery
            
            // Create mock orders
            val mockOrders = orderIds.mapIndexed { index, orderId ->
                mockk<DocumentSnapshot>().apply {
                    every { toObject(Order::class.java) } returns Order(
                        orderId = orderId,
                        buyerId = "test-buyer-id",
                        sellerId = "seller-$index",
                        totalAmountPKR = 100.0 * (index + 1),
                        status = "PENDING"
                    )
                }
            }
            
            every { mockSnapshot.documents } returns mockOrders
            
            // Capture the listener so we can trigger it
            var capturedListener: EventListener<QuerySnapshot>? = null
            every { 
                mockQuery.addSnapshotListener(capture(slot<EventListener<QuerySnapshot>>()))
            } answers {
                capturedListener = firstArg()
                mockListenerRegistration
            }
            
            every { mockListenerRegistration.remove() } just Runs
            
            // Create ViewModel with mocked dependencies
            val viewModel = OrdersViewModel()
            
            // Use reflection to inject mocked dependencies
            val dbField = OrdersViewModel::class.java.getDeclaredField("db")
            dbField.isAccessible = true
            dbField.set(viewModel, mockDb)
            
            val authField = OrdersViewModel::class.java.getDeclaredField("auth")
            authField.isAccessible = true
            authField.set(viewModel, mockAuth)
            
            runTest {
                // Call loadBuyerOrders
                viewModel.loadBuyerOrders()
                
                // Simulate Firestore callback with order data
                capturedListener?.onEvent(mockSnapshot, null)
                
                // Wait a bit for the flow to emit
                advanceTimeBy(100)
                
                // CRITICAL CHECK 1: Order data should persist in StateFlow
                val firstEmission = viewModel.buyerOrders.value
                firstEmission.shouldNotBeEmpty()
                firstEmission.size shouldBe orderIds.size
                
                // CRITICAL CHECK 2: After some time, data should STILL be there
                // On unfixed code, this will FAIL because the flow collection completes
                // and the StateFlow gets cleared or reset
                advanceTimeBy(500)
                
                val secondEmission = viewModel.buyerOrders.value
                secondEmission.shouldNotBeEmpty()
                secondEmission.size shouldBe orderIds.size
                
                // CRITICAL CHECK 3: Listener should NOT be removed prematurely
                // On unfixed code, this might be called too early
                verify(exactly = 0) { mockListenerRegistration.remove() }
            }
        }
    }
    
    "Property 1: Bug Condition - Flow collection persists for seller orders" {
        checkAll(10, Arb.list(Arb.string(), 1..5)) { orderIds ->
            // Setup mocks
            val mockAuth = mockk<FirebaseAuth>()
            val mockUser = mockk<FirebaseUser>()
            val mockDb = mockk<FirebaseFirestore>()
            val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>()
            val mockQuery = mockk<Query>()
            val mockSnapshot = mockk<QuerySnapshot>()
            val mockListenerRegistration = mockk<ListenerRegistration>()
            
            // Mock Firebase Auth
            every { mockAuth.currentUser } returns mockUser
            every { mockUser.uid } returns "test-seller-id"
            
            // Mock Firestore query chain
            every { mockDb.collection("orders") } returns mockCollection
            every { mockCollection.whereEqualTo("sellerId", any<String>()) } returns mockQuery
            every { mockQuery.orderBy("createdAt", Query.Direction.DESCENDING) } returns mockQuery
            
            // Create mock orders
            val mockOrders = orderIds.mapIndexed { index, orderId ->
                mockk<DocumentSnapshot>().apply {
                    every { toObject(Order::class.java) } returns Order(
                        orderId = orderId,
                        buyerId = "buyer-$index",
                        sellerId = "test-seller-id",
                        totalAmountPKR = 100.0 * (index + 1),
                        status = "PENDING"
                    )
                }
            }
            
            every { mockSnapshot.documents } returns mockOrders
            
            // Capture the listener so we can trigger it
            var capturedListener: EventListener<QuerySnapshot>? = null
            every { 
                mockQuery.addSnapshotListener(capture(slot<EventListener<QuerySnapshot>>()))
            } answers {
                capturedListener = firstArg()
                mockListenerRegistration
            }
            
            every { mockListenerRegistration.remove() } just Runs
            
            // Create ViewModel with mocked dependencies
            val viewModel = OrdersViewModel()
            
            // Use reflection to inject mocked dependencies
            val dbField = OrdersViewModel::class.java.getDeclaredField("db")
            dbField.isAccessible = true
            dbField.set(viewModel, mockDb)
            
            val authField = OrdersViewModel::class.java.getDeclaredField("auth")
            authField.isAccessible = true
            authField.set(viewModel, mockAuth)
            
            runTest {
                // Call loadSellerOrders
                viewModel.loadSellerOrders()
                
                // Simulate Firestore callback with order data
                capturedListener?.onEvent(mockSnapshot, null)
                
                // Wait a bit for the flow to emit
                advanceTimeBy(100)
                
                // CRITICAL CHECK 1: Order data should persist in StateFlow
                val firstEmission = viewModel.sellerOrders.value
                firstEmission.shouldNotBeEmpty()
                firstEmission.size shouldBe orderIds.size
                
                // CRITICAL CHECK 2: After some time, data should STILL be there
                // On unfixed code, this will FAIL because the flow collection completes
                advanceTimeBy(500)
                
                val secondEmission = viewModel.sellerOrders.value
                secondEmission.shouldNotBeEmpty()
                secondEmission.size shouldBe orderIds.size
                
                // CRITICAL CHECK 3: Listener should NOT be removed prematurely
                verify(exactly = 0) { mockListenerRegistration.remove() }
            }
        }
    }
})
