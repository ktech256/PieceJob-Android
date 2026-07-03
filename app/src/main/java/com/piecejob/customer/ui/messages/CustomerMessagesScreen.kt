package com.piecejob.customer.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.piecejob.core.data.remote.dto.ConversationDto

@Composable
fun CustomerMessagesScreen(
    viewModel: CustomerMessagesViewModel = hiltViewModel(),
    onNavigateToChat: (String, String) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Active Chats", "Job History")
    
    val conversations by viewModel.conversations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { (jobId, otherUserId) ->
            onNavigateToChat(jobId, otherUserId)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F5F7))) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading && conversations.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val filteredConversations = when (selectedTabIndex) {
                    0 -> conversations.filter { it.status in listOf("ACCEPTED", "ARRIVED", "STARTED", "IN_PROGRESS") }
                    1 -> conversations.filter { it.status == "COMPLETED" }
                    else -> emptyList()
                }

                if (filteredConversations.isEmpty()) {
                    EmptyState(tabs[selectedTabIndex])
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredConversations) { conversation ->
                            ConversationCard(conversation) {
                                viewModel.openChat(conversation.jobId, conversation.otherUser._id)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationCard(conversation: ConversationDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                if (conversation.otherUser.profilePicture != null) {
                    AsyncImage(
                        model = conversation.otherUser.profilePicture,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "${conversation.otherUser.firstName} ${conversation.otherUser.lastName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = conversation.lastMessageTime.take(10),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
                Text(
                    text = conversation.serviceName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = conversation.lastMessage,
                    fontSize = 13.sp,
                    color = if (conversation.unreadCount > 0) Color.Black else Color.Gray,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
            
            if (conversation.unreadCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier.size(20.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = conversation.unreadCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(tabName: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "No $tabName", color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(text = "Conversations related to $tabName will appear here.", color = Color.LightGray, fontSize = 12.sp)
        }
    }
}
