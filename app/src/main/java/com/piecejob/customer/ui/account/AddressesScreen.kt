package com.piecejob.customer.ui.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.dto.AddressDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressesScreen(
    onBack: () -> Unit,
    viewModel: CustomerAccountViewModel = hiltViewModel()
) {
    val addresses by viewModel.addresses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<AddressDto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Addresses", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Address")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (addresses.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Text("No addresses saved yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(addresses) { address ->
                        AddressItem(
                            address = address,
                            onEdit = { editingAddress = address },
                            onDelete = { address._id?.let { viewModel.deleteAddress(it) } }
                        )
                    }
                }
            }
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showAddDialog) {
        AddressDialog(
            title = "Add Address",
            onDismiss = { showAddDialog = false },
            onConfirm = { label, addr, lat, lng, isDefault ->
                viewModel.addAddress(label, addr, listOf(lng, lat), isDefault)
                showAddDialog = false
            }
        )
    }

    if (editingAddress != null) {
        AddressDialog(
            title = "Edit Address",
            initialLabel = editingAddress!!.label,
            initialAddress = editingAddress!!.address,
            initialIsDefault = editingAddress!!.isDefault,
            onDismiss = { editingAddress = null },
            onConfirm = { label, addr, lat, lng, isDefault ->
                viewModel.updateAddress(editingAddress!!._id!!, label, addr, listOf(lng, lat), isDefault)
                editingAddress = null
            }
        )
    }
}

@Composable
fun AddressItem(address: AddressDto, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when(address.label.uppercase()) {
                    "HOME" -> Icons.Default.Home
                    "WORK" -> Icons.Default.Work
                    else -> Icons.Default.LocationOn
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = address.label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = address.address, fontSize = 14.sp, color = Color.Gray)
                if (address.isDefault) {
                    Text(text = "Default", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun AddressDialog(
    title: String,
    initialLabel: String = "Home",
    initialAddress: String = "",
    initialIsDefault: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Double, Boolean) -> Unit
) {
    var label by remember { mutableStateOf(initialLabel) }
    var addressText by remember { mutableStateOf(initialAddress) }
    var isDefault by remember { mutableStateOf(initialIsDefault) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label (e.g. Home, Work)") })
                OutlinedTextField(value = addressText, onValueChange = { addressText = it }, label = { Text("Full Address") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                    Text("Set as Default")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(label, addressText, 0.0, 0.0, isDefault) }) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
