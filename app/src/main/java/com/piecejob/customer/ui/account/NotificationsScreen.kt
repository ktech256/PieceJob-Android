package com.piecejob.customer.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.dto.NotificationDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit
) {
    // Reusing logic for now, in a real app we'd have a specific ViewModel
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Mocking list for now as backend model for Notification exists
            val notifications = emptyList<NotificationDto>()

            if (notifications.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Text("You're all caught up!", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(notifications) { notification ->
                        NotificationRow(notification)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationRow(notification: NotificationDto) {
    ListItem(
        headlineContent = { Text(notification.title, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(notification.body) },
        overlineContent = { Text(notification.createdAt.take(10), fontSize = 10.sp) },
        leadingContent = {
            Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = if (notification.status == "UNREAD") Color.Blue else Color.Transparent) {}
        }
    )
    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
}
