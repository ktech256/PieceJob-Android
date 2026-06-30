package com.piecejob.customer.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.dto.UserCardDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(
    onBack: () -> Unit,
    viewModel: CustomerAccountViewModel = hiltViewModel()
) {
    val cards by viewModel.paymentMethods.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Methods", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Card")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (cards.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Text("No cards saved yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cards) { card ->
                        CardItem(
                            card = card,
                            onDelete = { card._id?.let { viewModel.deletePaymentMethod(it) } }
                        )
                    }
                }
            }
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showAddDialog) {
        AddCardDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { brand, last4, month, year, token, isDefault ->
                viewModel.addPaymentMethod(brand, last4, month, year, token, isDefault)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CardItem(card: UserCardDto, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${card.brand} **** ${card.last4}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "Expires ${card.expMonth}/${card.expYear}", fontSize = 14.sp, color = Color.Gray)
                if (card.isDefault) {
                    Text(text = "Default", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun AddCardDialog(onDismiss: () -> Unit, onConfirm: (String, String, Int, Int, String, Boolean) -> Unit) {
    var last4 by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("Visa") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Card (Mock)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("In production, this would be a secure gateway component.")
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") })
                OutlinedTextField(value = last4, onValueChange = { last4 = it }, label = { Text("Last 4 Digits") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(brand, last4, 12, 2026, "mock_token", true) }) {
                Text("ADD")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
