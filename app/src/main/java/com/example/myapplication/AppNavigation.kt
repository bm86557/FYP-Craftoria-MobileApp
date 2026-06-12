package com.example.myapplication


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.Auth.AuthScreen
import com.example.myapplication.Auth.LoginScreen
import com.example.myapplication.Auth.SignUpScreen
import com.example.myapplication.buyer.HomeScreen
import com.example.myapplication.pages.AddProductPage
import com.example.myapplication.pages.AddStoreProductPage
import com.example.myapplication.pages.AllStoresPage
import com.example.myapplication.pages.BuyerOrdersPage
import com.example.myapplication.pages.BuyerRefundHistoryPage
import com.example.myapplication.pages.BuyerStoreDetailPage
import com.example.myapplication.pages.CategoryProductPage
import com.example.myapplication.pages.ChatScreen
import com.example.myapplication.pages.ChatsListPage
import com.example.myapplication.pages.CheckOutPage
import com.example.myapplication.pages.CoSellerStorePage
import com.example.myapplication.pages.CreateCoSellerStorePage
import com.example.myapplication.pages.LearningResourcesPage
import com.example.myapplication.pages.MyReportsPage
import com.example.myapplication.pages.NegotiatonPage
import com.example.myapplication.pages.OrderDetailPage
import com.example.myapplication.pages.PaymentHistoryPage
import com.example.myapplication.pages.ProductDetailPge
import com.example.myapplication.pages.SearchResultsPage
import com.example.myapplication.pages.SellerNegotiationRequestScreen
import com.example.myapplication.pages.SellerProfileDetailPage
import com.example.myapplication.pages.StoreDetailPage
import com.example.myapplication.pages.StoreSettingsPage
import com.example.myapplication.pages.WalletScreen
import com.example.myapplication.sellerscreens.SellerAnalyticsPage
import com.example.myapplication.sellerscreens.SellerHomeScreen
import com.example.myapplication.sellerscreens.SellerRefundHistoryPage
import com.example.myapplication.sellerscreens.SellerRefundRequestsPage
import com.example.myapplication.verification.SellerVerificationScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await


@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    globalNavigation.navController = navController
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    var userRole by remember { mutableStateOf<String?>(null) }
    var isRoleLoaded by remember { mutableStateOf(false) }
    var hasNetworkError by remember { mutableStateOf(false) }
    var retryCount by remember { mutableStateOf(0) }
    var shouldSignOut by remember { mutableStateOf(false) }
   
    LaunchedEffect(firebaseUser, retryCount) {
        firebaseUser?.uid?.let { uid ->
            hasNetworkError = false
            try {
                val doc = Firebase.firestore.collection("users")
                    .document(uid)
                    .get()
                    .await()

                if (doc.exists()) {
                    userRole = doc.getString("role") ?: "buyer"
                    isRoleLoaded = true
                } else {
                    // User exists in Auth but not in Firestore, so sign them out safely.
                    shouldSignOut = true
                    FirebaseAuth.getInstance().signOut()
                    userRole = null
                    isRoleLoaded = true
                }
            } catch (e: Exception) {
                // Network/Firestore failure should not block app forever.
                hasNetworkError = true
                userRole = "buyer"
                isRoleLoaded = true
            }
        } ?: run {
            isRoleLoaded = true
        }
    }
    
    // Handle sign out
    LaunchedEffect(shouldSignOut) {
        if (shouldSignOut) {
            // Reset flag
            shouldSignOut = false
        }
    }
    
    if (firebaseUser != null && !isRoleLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (hasNetworkError) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Network issue detected. Loading app with safe defaults.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        hasNetworkError = false
                        isRoleLoaded = false
                        retryCount++
                    }) {
                        Text("Retry")
                    }
                }
            } else {
                CircularProgressIndicator()
            }
        }
    } else {
        val startDestination = when {
            firebaseUser == null -> AppRoutes.AUTH
            userRole == "seller" -> AppRoutes.SELLER_HOME
            else -> AppRoutes.HOME
        }
        NavHost(navController = navController, startDestination = startDestination) {
            composable(AppRoutes.AUTH){
                AuthScreen(modifier,navController)
            }
            composable(AppRoutes.LOGIN){
                LoginScreen(modifier,navController)
            }
            composable(AppRoutes.SIGN_UP){
                SignUpScreen(modifier,navController)
            }
            composable(AppRoutes.HOME){
                HomeScreen(modifier,navController)
            }
            composable("category-product/{categoryId}") {
                val categoryId = it.arguments?.getString("categoryId")
                if (categoryId != null) {
                    CategoryProductPage(modifier, categoryId)
                }
            }
            composable("product-details/{productId}") {
                val productId = it.arguments?.getString("productId")
                if (productId != null) {
                    ProductDetailPge(modifier, productId, navController)
                }
            }
            composable(AppRoutes.CHECKOUT){
                CheckOutPage(modifier,navController)
            }
            composable("checkout/{selectedProductId}") {
                val selectedProductId = it.arguments?.getString("selectedProductId").orEmpty()
                CheckOutPage(
                    modifier,
                    navController,
                    selectedProductId = selectedProductId
                )
            }
            composable("negotiation/{productId}") {
                val productId = it.arguments?.getString("productId")
                if (productId != null) {
                    NegotiatonPage(modifier, productId)
                }
            }
            composable("checkoutDeal/{negotiatedPrice}/{productId}") {
                val price = it.arguments?.getString("negotiatedPrice")?.toFloatOrNull() ?: 0f
                val productId = it.arguments?.getString("productId").orEmpty()
                CheckOutPage(
                    modifier,
                    navController,
                    negotiatedPrice = price,
                    negotiatedProductId = productId,
                )
            }
            composable("checkout/{negotiatedPrice}"){
                val price = it.arguments?.getString("negotiatedPrice")?.toFloatOrNull() ?: 0f
                CheckOutPage(modifier,navController, negotiatedPrice = price)
            }
            composable(AppRoutes.SELLER_HOME) {
                SellerHomeScreen(modifier,navController)
            }
            composable("addProductPage"){
                AddProductPage(modifier,navController)
            }

            composable("createCoSellerStore") {
                val context = LocalContext.current
                CreateCoSellerStorePage(
                    context = context,
                    navController = navController
                )
            }

            composable("cosellerstorepage") {
                val currentSellerId = Firebase.auth.currentUser?.uid ?: ""
                CoSellerStorePage(
                    modifier = modifier,
                    navController = navController,
                    currentSellerId = currentSellerId
                )
            }
            
            composable("storeDetail/{storeId}") {
                val storeId = it.arguments?.getString("storeId")
                if (storeId != null) {
                    StoreDetailPage(
                        storeId = storeId,
                        navController = navController
                    )
                }
            }
            
            composable("storeSettings/{storeId}") {
                val storeId = it.arguments?.getString("storeId")
                if (storeId != null) {
                    StoreSettingsPage(
                        storeId = storeId,
                        navController = navController
                    )
                }
            }
            
            composable("sellerProfile/{sellerId}?storeId={storeId}") {
                val sellerId = it.arguments?.getString("sellerId")
                val storeId = it.arguments?.getString("storeId")
                if (sellerId != null) {
                    SellerProfileDetailPage(
                        sellerId = sellerId,
                        storeId = storeId,
                        navController = navController
                    )
                }
            }
            
            composable("addStoreProduct/{storeId}") {
                val storeId = it.arguments?.getString("storeId")
                if (storeId != null) {
                    AddStoreProductPage(
                        storeId = storeId,
                        navController = navController
                    )
                }
            }
            
            composable("allStores") {
                AllStoresPage(navController = navController)
            }
            
            composable("search") {
                SearchResultsPage(navController = navController)
            }
            
            composable("payment_history") {
                PaymentHistoryPage(navController = navController)
            }
            
            composable("buyerStoreDetail/{storeId}") {
                val storeId = it.arguments?.getString("storeId")
                if (storeId != null) {
                    BuyerStoreDetailPage(
                        storeId = storeId,
                        navController = navController
                    )
                }
            }
            composable("sellerNegotiationRequest") {
                SellerNegotiationRequestScreen(modifier,navController)
            }
           
            // Buyer Orders
            composable(AppRoutes.BUYER_ORDERS) {
                BuyerOrdersPage(navController = navController)
            }
            
            // Buyer Refund History
            composable("buyer_refund_history") {
                BuyerRefundHistoryPage(navController = navController)
            }
            
            // Seller Refund History
            composable("seller_refund_history") {
                SellerRefundHistoryPage(navController = navController)
            }
            
            // Seller Refund Requests (Pending Approvals)
            composable("seller_refund_requests") {
                SellerRefundRequestsPage(navController = navController)
            }
            
            // Seller Analytics
            composable("seller_analytics") {
                SellerAnalyticsPage(navController = navController)
            }
            
            composable("order_detail/{orderId}") { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                OrderDetailPage(
                    orderId = orderId,
                    navController = navController
                )
            }
            composable(AppRoutes.WALLET) {
                WalletScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            // Chat Routes
            composable("chats_list") {
                ChatsListPage(navController = navController)
            }
            composable("seller_verification") {
                SellerVerificationScreen(navController = navController)
            }
            
            composable("chat_screen/{chatId}/{receiverId}") { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                val receiverId = backStackEntry.arguments?.getString("receiverId") ?: ""
                ChatScreen(
                    chatId = chatId,
                    receiverId = receiverId,
                    onBack = { navController.popBackStack() }
                )
            }
            
            // Learning Resources
            composable("learning_resources") {
                LearningResourcesPage()
            }
            
            // My Reports & Complaints
            composable("my_reports") {
                MyReportsPage(navController = navController)
            }


    }


   
    
  
}


}
object AppRoutes {
    const val AUTH = "auth"
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
    const val HOME = "home"
    const val CHECKOUT = "checkout"
    const val SELLER_HOME = "sellerhome"
    const val BUYER_ORDERS = "buyer_orders"
    const val SELLER_ORDERS = "seller_orders"
    const val WALLET = "wallet"
}

object  globalNavigation{
    lateinit var navController: NavHostController
    
    fun navigateSafely(route: String) {
        try {
            if (::navController.isInitialized) {
                navController.navigate(route)
            }
        } catch (e: Exception) {
            // Log error but don't crash
        }
    }
    
    fun popBackStackSafely(): Boolean {
        return try {
            if (::navController.isInitialized) {
                navController.popBackStack()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun navigateToTopLevel(route: String) {
        try {
            if (::navController.isInitialized) {
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                    launchSingleTop = true
                }
            }
        } catch (e: Exception) {
            // Keep app stable even if navigation state is not ready
        }
    }
}

