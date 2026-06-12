package com.example.myapplication.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.components.CartItemView
import com.example.myapplication.globalNavigation
import com.example.myapplication.model.UserModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

@Composable
fun CartPage(modifier: Modifier = Modifier) {
    val usermodel = remember { mutableStateOf(UserModel()) }

    DisposableEffect(Unit) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid == null) {
            onDispose { }
        } else {
            val listener = Firebase.firestore.collection("users")
                .document(currentUid)
                .addSnapshotListener { it, _ ->
                    if (it != null) {
                        val result = it.toObject(UserModel::class.java)
                        if (result != null) {
                            usermodel.value = result
                        }
                    }
                }
            onDispose {
                listener.remove()
            }
        }
    }
    
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Your Cart", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(16.dp))
    
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(usermodel.value.cartItems.toList(), key = { it.first }) { (productId, qty) ->
                CartItemView(productId = productId, qty = qty)
            }
        }
        
        Button(
            onClick = {
                val productId = usermodel.value.cartItems.keys.firstOrNull()
                if (productId != null) {
                    globalNavigation.navigateSafely("negotiation/$productId")
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = usermodel.value.cartItems.isNotEmpty()
        ) {
            Text(text = "Negotiate Price", fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = {
                globalNavigation.navigateSafely("checkout")
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = usermodel.value.cartItems.isNotEmpty()
        ) {
            Text(text = "Proceed To Checkout", fontSize = 16.sp)
        }
    }
}