package com.piecejob.provider.ui.messages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.TicketDto

@Composable
fun ProviderMessagesScreen(
    viewModel: ProviderMessagesViewModel = hiltViewModel(),
    onNavigateToChat: (String, String) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Active Chats", "Completed Jobs", "Support", "Disputes")
    
    val conversations by viewModel.conversations.collectAsState()
    val tickets by viewModel.tickets.collectAsState()
    val disputes by viewModel.disputes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { (jobId, otherUserId) ->
            onNavigateToChat(jobId, otherUserId)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading && conversations.isEmpty() && tickets.isEmpty() && disputes.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                when (selectedTabIndex) {
                    0 -> ConversationList(conversations.filter { it.status in listOf("ACCEPTED", "ARRIVED", "STARTED", "IN_PROGRESS") }) {
                        viewModel.openChat(it.jobId, it.otherUser._id)
                    }
                    1 -> ConversationList(conversations.filter { it.status == "COMPLETED" }) {
                        viewModel.openChat(it.jobId, it.otherUser._id)
                    }
                    2 -> TicketList(tickets)
                    3 -> DisputeList(disputes)
                    else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "${tabs[selectedTabIndex]} Placeholder")
                    }
                }
            }
        }
    }
}

@Composable
fun DisputeList(disputes: List<com.piecejob.core.data.remote.dto.DisputeDto>) {
    if (disputes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No disputes found")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(disputes) { dispute ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Job: #${dispute.jobId.takeLast(6).uppercase()}", fontWeight = FontWeight.Bold)
                            Text(text = dispute.status, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                        Text(text = dispute.reason, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = dispute.description, fontSize = 12.sp, color = Color.Gray, maxLines = 2)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = dispute.createdAt.take(10), fontSize = 10.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationList(conversations: List<com.piecejob.core.data.remote.dto.ConversationDto>, onClick: (com.piecejob.core.data.remote.dto.ConversationDto) -> Unit) {
    if (conversations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No conversations found")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(conversations) { conversation ->
                ConversationItem(conversation, onClick)
            }
        }
    }
}

@Composable
fun ConversationItem(conversation: com.piecejob.core.data.remote.dto.ConversationDto, onClick: (com.piecejob.core.data.remote.dto.ConversationDto) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick(conversation) }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                if (conversation.otherUser.profilePicture != null) {
                    coil.compose.AsyncImage(
                        model = conversation.otherUser.profilePicture,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Person, contentDescription = null)
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "${conversation.otherUser.firstName} ${conversation.otherUser.lastName}", fontWeight = FontWeight.Bold)
                    Text(text = conversation.lastMessageTime.take(10), fontSize = 10.sp, color = Color.Gray)
                }
                Text(text = "Job: ${conversation.serviceName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(text = conversation.lastMessage, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
            }
            
            if (conversation.unreadCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier.size(20.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
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
fun TicketList(tickets: List<TicketDto>) {
    if (tickets.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No support tickets found")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tickets) { ticket ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(text = ticket.subject, fontWeight = FontWeight.Bold)
                            Text(text = "Status: ${ticket.status}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(text = ticket.createdAt.take(10), fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
