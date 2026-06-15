package com.piecejob.provider.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HelpCenter
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSupportScreen(
    viewModel: ProviderSupportViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToTicket: (String) -> Unit
) {
    val tickets by viewModel.tickets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("GENERAL") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Create Support Ticket") },
            text = {
                Column {
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.submitTicket(type, subject, description)
                    showAddDialog = false
                    subject = ""
                    description = ""
                }) { Text("Submit") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New Ticket")
            }
        }
    ) { padding ->
        if (isLoading && tickets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (tickets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No support history.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(tickets) { ticket ->
                    TicketRow(ticket) { onNavigateToTicket(ticket.id) }
                }
            }
        }
    }
}

@Composable
fun TicketRow(ticket: TicketDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.HelpCenter, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = ticket.subject, fontWeight = FontWeight.Bold)
                Text(text = "Ref: ${ticket.id.takeLast(6)} • ${ticket.createdAt.take(10)}", fontSize = 11.sp, color = Color.Gray)
            }
            TicketStatusBadge(ticket.status)
        }
    }
}

@Composable
fun TicketStatusBadge(status: String) {
    Surface(
        color = when(status) {
            "OPEN" -> Color(0xFFE3F2FD)
            "RESOLVED" -> Color(0xFFE8F5E9)
            "CLOSED" -> Color(0xFFF5F5F5)
            else -> Color(0xFFFFF3E0)
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = when(status) {
                "OPEN" -> Color(0xFF1976D2)
                "RESOLVED" -> Color(0xFF2E7D32)
                "CLOSED" -> Color(0xFF616161)
                else -> Color(0xFFE65100)
            }
        )
    }
}
