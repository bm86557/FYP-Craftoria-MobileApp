package com.example.myapplication.pages

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.components.ProductItemView
import com.example.myapplication.model.ProductModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

@Composable
fun CategoryProductPage(modifier: Modifier = Modifier,categoryId: String) {
    val productList= remember { mutableStateOf<List< ProductModel>>(emptyList()) }
    LaunchedEffect(Unit) {
        Firebase.firestore.collection("data").document("stock")
            .collection("products").whereEqualTo("category",categoryId)
            .whereEqualTo("status","approved")
            .get()
            .addOnCompleteListener (){
                if (it.isSuccessful){
                    val readList= it.result.documents.mapNotNull { doc->
                        doc.toObject(ProductModel::class.java)
                    }
                    productList.value=readList
                }

            }
    }
    LazyColumn (modifier = Modifier.fillMaxSize().padding(16.dp)){
        items(productList.value.chunked(2)){rowitem->
            Row {
                rowitem.forEach {
                    ProductItemView(product = it, modifier = Modifier.weight(1f))
                }
                if (rowitem.size==1){
                    Spacer(modifier = Modifier.weight(1f) )
                }
            }

        }
    }
}