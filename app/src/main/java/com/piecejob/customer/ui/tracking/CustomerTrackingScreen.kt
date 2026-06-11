package com.piecejob.customer.ui.tracking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piecejob.core.ui.components.LiveTrackingMap

// Customer Theme Colors
val EarthyRed = Color(0xFF410200)
val DeepMauve = Color(0xFF4A2C2A)
val Cream = Color(0xFFEFDECD)

@Composable
fun CustomerTrackingScreen(
    providerName: String,
    providerRating: Float,
    providerLocation: Pair<Double, Double>?,
    customerLocation: Pair<Double, Double>,
    eta: String,
    onCancelJob: () -> Unit,
    onChatOpen: () -> Unit,
    onSosTrigger: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Map Integrated
        LiveTrackingMap(
            providerLocation = providerLocation,
            customerLocation = customerLocation,
            modifier = Modifier.fillMaxSize()
        )

        // Top Status Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            colors = CardDefaults.cardColors(containerColor = EarthyRed),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Provider is en route", color = Color.White, fontSize = 14.sp)
                    Text(text = "Estimated Arrival: $eta", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Cream,
                    strokeWidth = 2.dp
                )
            }
        }

        // Bottom Provider Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile Photo Placeholder
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(DeepMauve),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = providerName.take(1), color = Color.White, fontSize = 24.sp)
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = providerName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "⭐ $providerRating • Professional Level", color = Color.Gray, fontSize = 14.sp)
                    }
                    
                    Button(onClick = onChatOpen, colors = ButtonDefaults.buttonColors(containerColor = EarthyRed)) {
                        Text("Chat")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 9.1: Stealth SOS Trigger
                // Disguised as a "Safety Center" to remain stealthy
                OutlinedButton(
                    onClick = onSosTrigger,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EarthyRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EarthyRed)
                ) {
                    Text("Safety Center & Support")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onCancelJob,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Cancel Request", color = Color.DarkGray)
                }
            }
        }
    }
}
