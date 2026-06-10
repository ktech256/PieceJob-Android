package com.piecejob.core.ui.referral

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReferralScreen(
    referralCode: String,
    onInviteClick: (String) -> Unit,
    themeColor: Color // EarthyRed or ForestGreen
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Invite & Earn",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = themeColor
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Share your code with friends and earn rewards for every successful job they complete!",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "YOUR REFERRAL CODE", fontSize = 12.sp, color = themeColor)
                Text(
                    text = referralCode,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = themeColor
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { onInviteClick(referralCode) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Invite Friends", fontWeight = FontWeight.Bold)
        }
    }
}
