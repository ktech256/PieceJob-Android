package com.piecejob.customer.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PieceJobPlusScreen(
    onBack: () -> Unit,
    viewModel: CustomerAccountViewModel = hiltViewModel()
) {
    val subscription by viewModel.subscription.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PieceJob Plus", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            val isPlus = subscription?.plan == "PLUS" && subscription?.status == "ACTIVE"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF6200EE), Color(0xFFBB86FC))),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        text = if (isPlus) "You are a Plus Member" else "Upgrade to Plus",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (isPlus) "Valid until ${subscription?.expiryDate?.take(10)}" else "Unlock premium benefits",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
                Icon(
                    Icons.Default.Stars,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).align(Alignment.CenterEnd).offset(x = 20.dp),
                    tint = Color.White.copy(alpha = 0.2f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(text = "Benefits", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            BenefitItem("Priority Matching", "Get connected to the best providers faster.")
            BenefitItem("Zero Booking Fees", "Pay no booking fees on any job.")
            BenefitItem("Premium Support", "24/7 dedicated support line.")
            BenefitItem("Exclusive Discounts", "Up to 15% off on selected services.")

            Spacer(modifier = Modifier.weight(1f))

            if (!isPlus) {
                Button(
                    onClick = { viewModel.upgradeSubscription("PLUS") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading
                ) {
                    Text("UPGRADE NOW - $9.99/mo", fontWeight = FontWeight.Black)
                }
            } else {
                OutlinedButton(
                    onClick = { viewModel.cancelSubscription() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("CANCEL SUBSCRIPTION", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun BenefitItem(title: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 12.dp)) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = desc, fontSize = 14.sp, color = Color.Gray)
        }
    }
}
