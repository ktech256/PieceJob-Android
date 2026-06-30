package com.piecejob.customer.ui.account

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
import com.piecejob.core.ui.navigation.Screen

import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CustomerAccountScreen(
    onLogout: () -> Unit,
    onNavigate: (Screen) -> Unit,
    viewModel: CustomerAccountViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            AccountHeader(
                name = "${user?.firstName ?: ""} ${user?.lastName ?: ""}".trim().ifEmpty { "Customer" },
                email = user?.email ?: "",
                photo = user?.profilePhoto,
                country = user?.countryCode ?: "ZA"
            )
        }

        val sections = listOf(
            AccountSection("PERSONAL", listOf(
                AccountMenuItem("Personal Details", Icons.Default.Person, Screen.CustomerPersonalDetails),
                AccountMenuItem("Addresses", Icons.Default.Home, Screen.CustomerAddresses),
                AccountMenuItem("Saved Locations", Icons.Default.Map, Screen.CustomerSavedLocations)
            )),
            AccountSection("FINANCIAL", listOf(
                AccountMenuItem("Payment Methods", Icons.Default.Payment, Screen.CustomerPaymentMethods),
                AccountMenuItem("Wallet Hub", Icons.Default.AccountBalanceWallet, Screen.CustomerWalletHub),
                AccountMenuItem("Invoices", Icons.Default.Receipt, Screen.CustomerInvoices),
                AccountMenuItem("Statements", Icons.Default.Assessment, Screen.CustomerStatements)
            )),
            AccountSection("ENGAGEMENT", listOf(
                AccountMenuItem("Notifications", Icons.Default.Notifications, Screen.CustomerNotifications),
                AccountMenuItem("Referrals", Icons.Default.CardGiftcard, Screen.CustomerReferrals),
                AccountMenuItem("Rewards", Icons.Default.Stars, Screen.CustomerRewards),
                AccountMenuItem("PieceJob Plus", Icons.Default.AddCircle, Screen.CustomerPlus)
            )),
            AccountSection("SAFETY", listOf(
                AccountMenuItem("SOS Settings", Icons.Default.Security, Screen.CustomerSosSettings),
                AccountMenuItem("Emergency Contacts", Icons.Default.Phone, Screen.CustomerEmergencyContacts)
            )),
            AccountSection("PREFERENCES", listOf(
                AccountMenuItem("Language", Icons.Default.Language, Screen.CustomerLanguage),
                AccountMenuItem("Country", Icons.Default.Public, Screen.CustomerCountry),
                AccountMenuItem("Privacy", Icons.Default.PrivacyTip, Screen.CustomerPrivacy),
                AccountMenuItem("Security", Icons.Default.Lock, Screen.CustomerSecurity)
            )),
            AccountSection("SYSTEM", listOf(
                AccountMenuItem("Support", Icons.Default.SupportAgent, Screen.CustomerSupport),
                AccountMenuItem("About", Icons.Default.Info, Screen.CustomerAbout)
            ))
        )

        sections.forEach { section ->
            item {
                Text(
                    text = section.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                )
            }
            items(section.items.size) { index ->
                val item = section.items[index]
                ListItem(
                    headlineContent = { Text(item.title, fontWeight = FontWeight.SemiBold) },
                    leadingContent = { Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray) },
                    modifier = Modifier.clickable { onNavigate(item.screen) }
                )
                if (index < section.items.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
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
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun AccountHeader(name: String, email: String, photo: String?, country: String) {
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
            if (photo != null) {
                coil.compose.AsyncImage(
                    model = photo,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = name, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(text = email, fontSize = 14.sp, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Text(
                    text = when(country) {
                        "ZA" -> "South Africa 🇿🇦"
                        "NG" -> "Nigeria 🇳🇬"
                        "KE" -> "Kenya 🇰🇪"
                        else -> country
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

data class AccountSection(val title: String, val items: List<AccountMenuItem>)
data class AccountMenuItem(val title: String, val icon: ImageVector, val screen: Screen)
