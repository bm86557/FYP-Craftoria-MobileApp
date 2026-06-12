package com.example.myapplication.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.globalNavigation
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

@Composable
fun HeaderView(modifier: Modifier = Modifier) {
    var name by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        Firebase.firestore.collection("users")
            .document(uid)
            .get().addOnCompleteListener {
                val fullName = it.result?.getString("name").orEmpty().trim()
                name = if (fullName.isNotEmpty()) fullName.split(" ").first() else "User"
            }

    }
    Row (modifier= modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween){
        Column(modifier= modifier.padding(20.dp), verticalArrangement = Arrangement.Center) {
            Text(
                text = "Craftoria",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = Bold,
                )
            )
            Spacer(modifier= modifier.height(10.dp))
            Text(text = "Welcome Back $name !")
        }
        IconButton(
            onClick = { 
                globalNavigation.navigateSafely("search")
            },
            modifier= modifier.padding(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
        }

    }
}