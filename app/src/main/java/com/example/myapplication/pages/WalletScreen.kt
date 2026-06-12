package com.example.myapplication.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.model.WalletViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun WalletScreen(
    viewModel: WalletViewModel = viewModel(),
    onBack: () -> Unit
) {
    val balance by viewModel.balance.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadWallet()
        viewModel.loadTransactions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ── Balance Card ──────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if (userRole == "seller") "💼 Seller Wallet" else "👛 Buyer Wallet",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Rs. ${String.format("%.2f", balance)}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Set Default Balance Button (Sirf Buyer ke liye) ────────────────
        if (userRole == "buyer") {
            Button(
                onClick = {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .update("walletBalance", 4000.0)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Add Test Balance (Rs. 4000)")
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Transactions List ─────────────────────────
        Text("Recent Transactions", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (transactions.isEmpty() && !isLoading) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "No transactions yet",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn {
                items(
                    items = transactions,
                    key = { txn -> txn["id"] as? String ?: txn.hashCode() }
                ) { txn ->
                    val type = txn["type"] as? String ?: ""
                    val amount = (txn["amount"] as? Number)?.toDouble() ?: 0.0
                    val desc = txn["description"] as? String ?: "Transaction"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = type,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when (type) {
                                        "CREDIT" -> Color(0xFF2E7D32)
                                        "REFUND" -> Color(0xFF1565C0)
                                        "DEBIT"  -> Color(0xFFC62828)
                                        else     -> Color.Gray
                                    }
                                )
                            }
                            Text(
                                text = "${if (type == "DEBIT") "-" else "+"}Rs. ${
                                    String.format("%.2f", amount)
                                }",
                                color = if (type == "DEBIT")
                                    Color(0xFFC62828) else Color(0xFF2E7D32),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}