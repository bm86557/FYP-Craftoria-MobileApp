package com.example.myapplication.Auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.navigation.NavHostController
import com.example.myapplication.AppRoutes
import com.example.myapplication.R


@Composable
fun AuthScreen(modifier: Modifier = Modifier, navController: NavHostController) {
    Box(modifier = Modifier.fillMaxSize().fillMaxWidth()) {
        Image(
            painter = painterResource(R.drawable.bgimgggg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(52.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Craftoria",
                    modifier = Modifier.padding(16.dp),
                    color = Color.Black,
                    fontSize = 40.sp,
                    fontWeight = Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Empower Women Artisans",
                    modifier = Modifier.padding(10.dp),
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = Bold
                )
                Spacer(modifier = Modifier.height(25.dp))
                Button(
                    onClick = { navController.navigate("login") },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text(text = "Sign in", fontSize = 22.sp) }
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = { navController.navigate(AppRoutes.SIGN_UP) },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(text = "Sign Up", fontSize = 22.sp, color = Color.Black)
                }
            }
        }
    }



@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AuthScreenPreview(modifier: Modifier = Modifier) {
    AuthScreen(modifier,navController = NavHostController(LocalContext.current))
}