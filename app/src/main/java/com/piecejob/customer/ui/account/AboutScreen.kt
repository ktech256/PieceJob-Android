package com.piecejob.customer.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piecejob.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About PieceJob", fontWeight = FontWeight.Bold) },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("PJ", fontSize = 40.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "PieceJob App", fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(text = "Version ${BuildConfig.VERSION_NAME}", color = Color.Gray)

            Spacer(modifier = Modifier.height(48.dp))

            AboutListItem("Terms of Service") { /* Open URL */ }
            AboutListItem("Privacy Policy") { /* Open URL */ }
            AboutListItem("Open Source Licenses") { /* Open Screen */ }
            AboutListItem("Company Information") { /* Open Details */ }
            
            Spacer(modifier = Modifier.height(64.dp))
            Text(text = "© 2024 PieceJob Inc.", fontSize = 12.sp, color = Color.LightGray)
            Text(text = "All rights reserved.", fontSize = 12.sp, color = Color.LightGray)
        }
    }
}

@Composable
fun AboutListItem(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(text = label, modifier = Modifier.weight(1f), color = Color.Black, fontWeight = FontWeight.Medium)
        Icon(androidx.compose.material.icons.Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
}
