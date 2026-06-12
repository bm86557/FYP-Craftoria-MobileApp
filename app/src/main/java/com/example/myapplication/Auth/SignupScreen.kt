package com.example.myapplication.Auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.AppUtil
import com.example.myapplication.model.AuthViewModel
import com.example.myapplication.R


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SignUpScreenPreview(){
    SignUpScreen(navController = NavHostController(LocalContext.current))
}

@Composable
fun SignUpScreen(modifier: Modifier = Modifier, navController: NavHostController,authViewModel: AuthViewModel=viewModel()) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    val BUYER = "buyer"
    val SELLER = "seller"
    var role by remember { mutableStateOf(BUYER) }

    Box(
        modifier = Modifier.fillMaxSize()
            , contentAlignment = Alignment.Center
    ){
        Image(
            painter = painterResource(R.drawable.bgimgggg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)

        ){
            Column(
                modifier = modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Text(
                    text = "Craftoria",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Black,
                    fontSize = 40.sp,
                    fontWeight = Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Create an Account!",
                    modifier = Modifier.padding(10.dp),
                    color = Color.Black,
                    fontSize = 28.sp,
                    fontWeight = Bold
                )
                Spacer(modifier = Modifier.height(18.dp))
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text(text = "Email Address", color = Color.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name= it },
                    label = { Text(text = "Full Name", color = Color.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text(text = "Password", color = Color.Black) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color.Black
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "Select Role", color = Color.Black)
                Spacer(modifier = Modifier.height(14.dp))
                Row {
                    Text(text = "Buyer", color = Color.Black)
                    RadioButton(
                        selected = role == BUYER,
                        onClick = {role = BUYER })
                    Text(text = "Seller", color = Color.Black)
                    Spacer(modifier = Modifier.width(14.dp))
                    RadioButton(
                        selected = role == SELLER,
                        onClick = { role= SELLER })

                }
                Spacer(modifier = Modifier.height(44.dp))

                Button(onClick = {
                    isLoading= true
                    authViewModel.Signup(email,name,password,role){success,errorMessage->
                        if (success){
                            isLoading = false
                           if (role == SELLER){
                               navController.navigate("sellerhome"){
                                   popUpTo("auth"){inclusive= true}
                               }
                           }
                            else {
                               navController.navigate("home") {
                                   popUpTo("auth") { inclusive = true }
                               }
                           }
                        }
                        else{
                            isLoading=false
                            AppUtil.showToast(context,errorMessage?:"Something went Wrong")
                        }

                    }
                },
                    enabled = !isLoading,
                    modifier=Modifier.fillMaxWidth().height(60.dp)
                ) { Text(text = if(isLoading)"Creating Account" else "Sign Up",fontSize = 22.sp)}
            }
        }
    }
}