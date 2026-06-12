package com.example.myapplication.pages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.globalNavigation
import com.example.myapplication.model.NegotiationRequest
import com.example.myapplication.model.ProductModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import java.util.UUID

@Composable
fun NegotiatonPage(
    modifier: Modifier = Modifier,
    productId : String,

) {
   val product = remember { mutableStateOf(ProductModel()) }
    val messages = remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    val userInput = remember { mutableStateOf("") }
    val isDealDone = remember { mutableStateOf(false) }
    val  finalPrice = remember { mutableStateOf(0) }
    val pendingOffer = remember { mutableStateOf<Int?>(null) }
    val requestSent = remember { mutableStateOf(false) }
    val requestAccepted = remember { mutableStateOf(false) }
    val requestRejected = remember { mutableStateOf(false) }

   LaunchedEffect(productId) {
       // Reset all states when productId changes
       messages.value = emptyList()
       requestSent.value = false
       requestAccepted.value = false
       requestRejected.value = false
       isDealDone.value = false
       pendingOffer.value = null
       finalPrice.value = 0
       
       val buyerId = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
       
       // First check if there's an existing pending/accepted request for this product
       Firebase.firestore.collection("negotiation_requests")
           .whereEqualTo("productId", productId)
           .whereEqualTo("buyerId", buyerId)
           .get()
           .addOnSuccessListener { snapshot ->
               val existingRequest = snapshot.documents.firstOrNull()
               
               if (existingRequest != null) {
                   val status = existingRequest.getString("status")
                   val offeredPrice = existingRequest.getLong("offeredPrice")?.toInt() ?: 0
                   
                   when (status) {
                       "pending" -> {
                           // Restore pending state
                           requestSent.value = true
                           pendingOffer.value = offeredPrice
                           messages.value = messages.value + Pair("bot", "⏳ Your offer of Rs$offeredPrice is pending. Waiting for seller response...")
                       }
                       "accepted" -> {
                           // Restore accepted state
                           requestAccepted.value = true
                           finalPrice.value = offeredPrice
                           messages.value = messages.value + Pair("bot", "✅ Seller Accepted Your Offer of Rs$offeredPrice! Checkout Now")
                       }
                       "rejected" -> {
                           // Show rejected message
                           requestRejected.value = true
                           messages.value = messages.value + Pair("bot", "❌ Your previous offer of Rs$offeredPrice was rejected. Try a better price!")
                       }
                   }
               }
           }
       
       // Load product details
       Firebase.firestore.collection("data").document("stock")
           .collection("products").document(productId).get()
           .addOnCompleteListener {
               if (it.isSuccessful){
                  val  result = it.result.toObject(ProductModel::class.java)
                   if (result!= null){
                       product.value = result
                       // Only show initial message if no existing request
                       if (messages.value.isEmpty()) {
                           messages.value = messages.value + Pair("bot", "Hello Dear Customer, Product: ${product.value?.title} in Price Rs${product.value?.actualPrice}. What you offer?")
                       }
                   }
               }
           }

       // Listen for real-time updates on negotiation status
       Firebase.firestore.collection("negotiation_requests")
           .whereEqualTo("productId", productId)
           .whereEqualTo("buyerId", buyerId)
           .addSnapshotListener { snapshot, _ ->
               snapshot?.documents?.firstOrNull()?.let { doc ->
                   val status = doc.getString("status")
                   when (status) {
                       "accepted" -> {
                           if (!requestAccepted.value) {
                               requestAccepted.value = true
                               requestRejected.value = false
                               requestSent.value = false
                               finalPrice.value = doc.getLong("offeredPrice")?.toInt() ?: 0
                               // Only add message if not already in messages
                               if (!messages.value.any { it.second.contains("Seller Accepted") }) {
                                   messages.value = messages.value + Pair("bot", "✅ Seller Accepted Your Offer! Checkout Now")
                               }
                           }
                       }
                       "rejected" -> {
                           if (!requestRejected.value) {
                               requestRejected.value = true
                               requestAccepted.value = false
                               requestSent.value = false
                               // Only add message if not already in messages
                               if (!messages.value.any { it.second.contains("Seller Rejected") }) {
                                   messages.value = messages.value + Pair("bot", "❌ Seller Rejected Your Offer. Try a better price!")
                               }
                           }
                       }
                   }
               }
           }
   }
    fun botReply(usermessage: String): String{
        val offered = usermessage.filter { it.isDigit() }.toIntOrNull()?: return  "Price Should be in Number"
        val minDealPrice= product.value?.minDealPrice ?: return "Error!"
        val actualPrice = product.value?.actualPrice?.toInt() ?:return "Error!"
        return when {
            offered >= actualPrice ->{
                isDealDone.value= true
                finalPrice.value = actualPrice
                "Deal in Rs$actualPrice,CheckOut Now"}
            offered >= minDealPrice -> {
                 pendingOffer.value = offered
                " Rs$offered is good offer! Send Request to Seller?"}
            else -> "Deal in Rs$offered is not Acceptable,Please Give Better Offer"
        }
    }
    fun sendNegotiationRequest(offeredPrice: Int,onSent:()-> Unit){
        val buyerId  = FirebaseAuth.getInstance().currentUser?.uid ?:return
        val sellerId = product.value?.sellerId ?: return
        val requestId = UUID.randomUUID().toString()

        val request = NegotiationRequest(
            requestId = requestId,
            productId = productId,
            buyerId = buyerId,
            sellerId = sellerId,
            offeredPrice = offeredPrice,
            status = "pending",
            timestamp = System.currentTimeMillis()
        )
        Firebase.firestore.collection("negotiation_requests")
            .document(requestId).set(request)
            .addOnSuccessListener {
                onSent()
            }
    }
    Column (modifier = Modifier.fillMaxSize().padding(16.dp).imePadding().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        
    ){
    Text( text =  "Negotiate: ${product.value.title}", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(8.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Original Price: Rs${product.value.actualPrice}",fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(14.dp))
        Text(text= "Enter Your Offer Amount: ", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        //Chat
        LazyColumn (modifier = Modifier.weight(1f)){
            items(messages.value){(sender,message) ->
                Row (modifier = Modifier.fillMaxWidth().padding(4.dp),
                    horizontalArrangement = if (sender == "user") Arrangement.End else Arrangement.Start
                ){
                    Card (colors = CardDefaults.cardColors(
                        containerColor = if (sender =="user") Color.Blue else Color.Gray), elevation = CardDefaults.cardElevation(8.dp)){
                        Text(
                            text = message,
                            color = Color.White,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        //Send Request Button
        if (pendingOffer.value !=null&& !requestSent.value){
            Button(onClick = {
                sendNegotiationRequest(
                    offeredPrice = pendingOffer.value!!, onSent = {
                        requestSent.value = true
                        messages.value = messages.value+ Pair("bot","⏳ Request sent to seller! Please wait for response...")
                    }
                )
            }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                Text("Send Request to Seller", color = Color.White)
            }
        }
        //Waiting Message
        if (requestSent.value && !requestAccepted.value && !requestRejected.value){
            Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Color(0xFFFF9800)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "⏳ Waiting for Seller Response...",
                        color = Color(0xFF856404),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "You can go back and check later",
                        color = Color(0xFF856404),
                        fontSize = 12.sp
                    )
                }
            }
        }
        // Input
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!isDealDone.value && !requestSent.value && !requestRejected.value) {
                TextField(
                    value = userInput.value,
                    onValueChange = { userInput.value = it },
                    placeholder = { Text("Enter Price To Offer...") },
                    modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                 val userMsg  = userInput.value.trim()
                    if (userMsg.isNotEmpty()){
                        messages.value = messages.value + Pair("user",userMsg)
                        val reply = botReply(userMsg)
                        messages.value = messages.value +Pair("bot",reply)

                        userInput.value = ""
                    }

                }) {
                    Text("Send")
                }
            }

        }
        Spacer(modifier = Modifier.height(16.dp))

        // Try Again Button for Rejected Requests
        if (requestRejected.value && !requestAccepted.value) {
            Button(
                onClick = {
                    // Delete the old rejected request
                    val buyerId = FirebaseAuth.getInstance().currentUser?.uid
                    if (buyerId != null) {
                        Firebase.firestore.collection("negotiation_requests")
                            .whereEqualTo("productId", productId)
                            .whereEqualTo("buyerId", buyerId)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                snapshot.documents.forEach { it.reference.delete() }
                            }
                    }
                    // Reset states
                    requestRejected.value = false
                    requestSent.value = false
                    pendingOffer.value = null
                    messages.value = messages.value + Pair("bot", "Let's try again! What's your new offer?")
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
            ) {
                Text(text = "Try Again with New Offer", fontSize = 16.sp, color = Color.White)
            }
        }

        if (requestAccepted.value && !requestRejected.value || isDealDone.value){
            Button(onClick = {
                   globalNavigation.navigateSafely("checkoutDeal/${finalPrice.value}/$productId")

            }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text(text = "Proceed To Checkout ", fontSize = 16.sp)
            }
        }


    }


}