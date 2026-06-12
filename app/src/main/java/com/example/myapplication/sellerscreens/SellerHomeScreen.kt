package com.example.myapplication.sellerscreens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.pages.ProductListingPage
import com.example.myapplication.pages.ProfilePage
import com.example.myapplication.pages.SellerHomePage
import com.example.myapplication.sellerscreens.OrderScreen

@Composable
fun SellerHomeScreen(modifier: Modifier = Modifier, navController: NavHostController) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
    val navItemList= listOf(
        NavItem("Home", Icons.Default.Home),
        NavItem("Products", Icons.Default.Add),
        NavItem("Orders", Icons.Default.ShoppingCart),
        NavItem("Profile", Icons.Default.Person)

    )
        var selectedIndex by rememberSaveable{ mutableStateOf(0) }
        Scaffold(
                bottomBar = {
                    NavigationBar {
                        navItemList.forEachIndexed { index, navitem ->
                           NavigationBarItem(
                               selected = index == selectedIndex,
                               onClick = {selectedIndex= index},
                               icon = {Icon(imageVector = navitem.icon, contentDescription = navitem.label)},
                               label = {Text(text = navitem.label)}
                           )

                        }
                    }
                }
        ) {
          SellerContentScreen(modifier = modifier.padding(it), selectedIndex, navController)
        }

    }
}
@Composable
fun SellerContentScreen(modifier: Modifier = Modifier, selectedIndex: Int, navController: NavHostController) {
    Column {
        when (selectedIndex) {
            0 -> SellerHomePage(modifier)
            1 -> ProductListingPage(modifier)
            2 -> OrderScreen(modifier = modifier, navController = navController)
            3 -> ProfilePage(modifier)
        }
    }
}



data class NavItem(
    val label : String,
    val icon : ImageVector
)