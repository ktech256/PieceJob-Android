package com.piecejob.provider.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.ui.navigation.Screen
import com.piecejob.provider.ui.dashboard.ProviderDashboardViewModel

@Composable
fun ProviderProfileScreen(
    viewModel: ProviderDashboardViewModel = hiltViewModel(),
    onLogout: () -> Unit,
    onNavigate: (Screen) -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            ProfileHeader(
                name = "Provider Name",
                tier = stats?.tier ?: "BRONZE",
                rating = stats?.rating ?: 0.0,
                isVerified = stats?.verificationStatus == "APPROVED"
            )
        }

        val menuItems = listOf(
            ProfileMenuItem("Personal Details", Icons.Default.Person, Screen.ProviderPersonalDetails),
            ProfileMenuItem("My Services", Icons.Default.Handyman, Screen.MyServices),
            ProfileMenuItem("Verification Documents", Icons.Default.VerifiedUser, Screen.VerificationDocs),
            ProfileMenuItem("Equipment & Tools", Icons.Default.Construction, Screen.EquipmentTools),
            ProfileMenuItem("Certifications", Icons.Default.HistoryEdu, Screen.Certifications),
            ProfileMenuItem("Experience", Icons.Default.Timeline, Screen.Experience),
            ProfileMenuItem("Wallet Settings", Icons.Default.Wallet, Screen.WalletSettings),
            ProfileMenuItem("Bank Details", Icons.Default.AccountBalance, Screen.BankDetails),
            ProfileMenuItem("Notifications", Icons.Default.Notifications, Screen.Notifications),
            ProfileMenuItem("Security", Icons.Default.Security, Screen.Security),
            ProfileMenuItem("Device Management", Icons.Default.Smartphone, Screen.DeviceManagement),
            ProfileMenuItem("Referral Program", Icons.Default.CardGiftcard, Screen.Referral),
            ProfileMenuItem("Statements", Icons.Default.ReceiptLong, Screen.ProviderStatements),
            ProfileMenuItem("Disputes", Icons.Default.Gavel, Screen.Disputes),
            ProfileMenuItem("Support", Icons.Default.SupportAgent, Screen.Support),
            ProfileMenuItem("Terms & Policies", Icons.Default.Policy, Screen.TermsPolicies)
        )

        items(menuItems.size) { index ->
            val item = menuItems[index]
            ListItem(
                headlineContent = { Text(item.title, fontWeight = FontWeight.SemiBold) },
                leadingContent = { Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray) },
                modifier = Modifier.clickable { onNavigate(item.screen) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("LOGOUT", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
fun ProfileHeader(name: String, tier: String, rating: Double, isVerified: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.White)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = name, fontSize = 20.sp, fontWeight = FontWeight.Black)
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "$tier TIER", color = Color(0xFFFFA000), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "•", color = Color.Gray)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(14.dp))
            Text(text = String.format("%.1f", rating), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            color = if (isVerified) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
            shape = CircleShape
        ) {
            Text(
                text = if (isVerified) "VERIFIED" else "PENDING", 
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = if (isVerified) Color(0xFF2E7D32) else Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

data class ProfileMenuItem(
    val title: String,
    val icon: ImageVector,
    val screen: Screen
)
