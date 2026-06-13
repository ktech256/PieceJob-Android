package com.piecejob.provider.ui.profile

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.dto.ReferralStatsDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderReferralScreen(
    viewModel: ProviderReferralViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Referral Program", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && stats == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                item {
                    Text(text = "Refer Friends & Earn", fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Earn R50.00 for every service provider who registers with your code and completes their first job.", fontSize = 14.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    ReferralCodeCard(stats?.referralCode ?: "------") { code ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Join me on PieceJob! Use my referral code $code to register as a provider: https://piecejob.work/invite")
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Code"))
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ReferralStatItem("Invited", (stats?.totalReferrals ?: 0).toString(), Modifier.weight(1f))
                        ReferralStatItem("Earned", "R${stats?.paidRewards ?: 0.0}", Modifier.weight(1f))
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(text = "REFERRAL HISTORY", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                stats?.history?.let { history ->
                    items(history) { user ->
                        ListItem(
                            headlineContent = { Text("${user.firstName} ${user.lastName}") },
                            supportingContent = { Text("Joined: ${user.createdAt.take(10)}") },
                            trailingContent = { 
                                if (user.isVerified) {
                                    Text("Success", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                } else {
                                    Text("Pending", color = Color(0xFFEF6C00), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
fun ReferralCodeCard(code: String, onShare: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "YOUR UNIQUE CODE", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text(text = code, fontSize = 36.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { onShare(code) }, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SHARE LINK")
            }
        }
    }
}

@Composable
fun ReferralStatItem(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text(text = label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
    }
}
