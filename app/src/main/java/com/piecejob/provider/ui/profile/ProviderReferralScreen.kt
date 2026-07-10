package com.piecejob.provider.ui.profile

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
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
            val referralCode = stats?.referralCode ?: "------"
            val referralLink = "https://piecejob.app/r/$referralCode"
            val shareMessage = "Start earning as a PieceJob Provider! Use my code $referralCode to register and get rewards after your first job.\n\nRegister here: $referralLink"

            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                item {
                    Text(text = "Refer & Earn Rewards", fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Get rewards for every new user (Customer or Provider) who joins and completes their first job using your code.", fontSize = 14.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    ReferralCodeCard(referralCode, referralLink) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareMessage)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Invitation"))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // QUICK SOCIAL SHARING
                    Text(text = "Quick Share", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SocialShareButton("WhatsApp", Icons.Default.Chat, Color(0xFF25D366)) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareMessage)
                                setPackage("com.whatsapp")
                            }
                            try { context.startActivity(intent) } catch (e: Exception) { context.startActivity(Intent.createChooser(intent, "Share via")) }
                        }
                        SocialShareButton("More", Icons.Default.Share, Color(0xFF121212)) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareMessage)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share via"))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ReferralStatItem("Total Invited", (stats?.totalReferrals ?: 0).toString(), Modifier.weight(1f))
                        ReferralStatItem("Total Earned", "${stats?.currency ?: stats?.history?.firstOrNull()?.currency ?: "R"} ${stats?.paidRewards ?: 0.0}", Modifier.weight(1f))
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(text = "REFERRAL HISTORY", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                stats?.history?.let { history ->
                    if (history.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                Text("No history yet.", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    } else {
                        items(history) { item ->
                            ReferralHistoryItem(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReferralCodeCard(code: String, link: String, onShare: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "YOUR UNIQUE CODE", fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text(text = code, fontSize = 36.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Text(text = link, fontSize = 10.sp, color = Color.Gray.copy(alpha = 0.6f))
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onShare, 
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SHARE")
                }
                
                OutlinedButton(
                    onClick = {
                        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Referral Link", link)
                        clipboardManager.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Link copied!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("COPY")
                }
            }
        }
    }
}

@Composable
fun SocialShareButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun ReferralHistoryItem(item: com.piecejob.core.data.remote.dto.ReferralHistoryDto) {
    val statusColor = when (item.status) {
        "REWARDED", "QUALIFIED" -> Color(0xFF2E7D32)
        "PENDING" -> Color(0xFFEF6C00)
        "EXPIRED", "REJECTED", "DISABLED" -> Color(0xFFD32F2F)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = item.referredUser, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Joined ${item.createdAt.take(10)}", fontSize = 11.sp, color = Color.Gray)
                }
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            if (item.jobId != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Qualifying Job", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(text = "Job #${item.jobId.takeLast(6)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Reward", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${item.currency ?: item.workspace ?: "R"} ${String.format("%.2f", item.rewardAmount)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = statusColor
                        )
                    }
                }
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
