package com.piecejob.customer.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rewards", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFFFFA000), Color(0xFFFFD54F))),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(text = "Reward Balance", color = Color.Black.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(text = "250 Points", color = Color.Black, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
                Icon(
                    Icons.Default.Stars,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).align(Alignment.CenterEnd).offset(x = 10.dp),
                    tint = Color.Black.copy(alpha = 0.1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Available Rewards", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            RewardOption("5% Off Next Job", "100 Points")
            RewardOption("Free SOS Monitoring (1mo)", "500 Points")
            RewardOption("$10 Wallet Credit", "1000 Points")
        }
    }
}

@Composable
fun RewardOption(label: String, cost: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Button(onClick = { /* Redeem */ }, shape = RoundedCornerShape(8.dp)) {
                Text(cost, fontSize = 10.sp)
            }
        }
    }
}
