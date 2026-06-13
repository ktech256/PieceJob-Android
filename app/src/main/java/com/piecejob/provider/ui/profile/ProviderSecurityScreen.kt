package com.piecejob.provider.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSecurityScreen(
    viewModel: ProviderSecurityViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val otpSent by viewModel.otpSent.collectAsState()
    val emailCodeSent by viewModel.emailCodeSent.collectAsState()

    var showPassDialog by remember { mutableStateOf(false) }
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }

    var showPhoneDialog by remember { mutableStateOf(false) }
    var newPhone by remember { mutableStateOf("") }
    var phoneOtp by remember { mutableStateOf("") }

    var showEmailDialog by remember { mutableStateOf(false) }
    var newEmail by remember { mutableStateOf("") }
    var emailCode by remember { mutableStateOf("") }

    if (showPhoneDialog) {
        AlertDialog(
            onDismissRequest = { showPhoneDialog = false },
            title = { Text(if (!otpSent) "Change Phone Number" else "Verify New Number") },
            text = {
                Column {
                    if (!otpSent) {
                        OutlinedTextField(value = newPhone, onValueChange = { newPhone = it }, label = { Text("New Phone Number") })
                    } else {
                        OutlinedTextField(value = phoneOtp, onValueChange = { phoneOtp = it }, label = { Text("OTP sent to $newPhone") })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (!otpSent) {
                        viewModel.requestPhoneChange(newPhone)
                    } else {
                        viewModel.verifyPhoneChange(newPhone, phoneOtp)
                        showPhoneDialog = false
                        newPhone = ""
                        phoneOtp = ""
                    }
                }) { Text(if (!otpSent) "Send OTP" else "Verify") }
            }
        )
    }

    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text(if (!emailCodeSent) "Change Email Address" else "Verify New Email") },
            text = {
                Column {
                    if (!emailCodeSent) {
                        OutlinedTextField(value = newEmail, onValueChange = { newEmail = it }, label = { Text("New Email") })
                    } else {
                        OutlinedTextField(value = emailCode, onValueChange = { emailCode = it }, label = { Text("Code sent to $newEmail") })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (!emailCodeSent) {
                        viewModel.requestEmailChange(newEmail)
                    } else {
                        viewModel.verifyEmailChange(newEmail, emailCode)
                        showEmailDialog = false
                        newEmail = ""
                        emailCode = ""
                    }
                }) { Text(if (!emailCodeSent) "Send Code" else "Verify") }
            }
        )
    }

    if (showPassDialog) {
        AlertDialog(
            onDismissRequest = { showPassDialog = false },
            title = { Text("Change Password") },
            text = {
                Column {
                    OutlinedTextField(
                        value = currentPass,
                        onValueChange = { currentPass = it },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.changePassword(currentPass, newPass)
                    showPassDialog = false
                    currentPass = ""
                    newPass = ""
                }) { Text("Change") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            item {
                SecurityActionRow("Change Password", "Update your account password regularly") {
                    showPassDialog = true
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SecurityActionRow("Change Phone Number", "Update your primary contact number") {
                    showPhoneDialog = true
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SecurityActionRow("Change Email Address", "Update your account email") {
                    showEmailDialog = true
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                SecurityActionRow("Logout All Devices", "End all active sessions on other devices") {
                    viewModel.logoutAllDevices()
                }
                
                if (message != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = message!!, color = if (message!!.contains("successfully")) Color(0xFF2E7D32) else Color.Red, fontWeight = FontWeight.Bold)
                    LaunchedEffect(message) {
                        kotlinx.coroutines.delay(3000)
                        viewModel.clearMessage()
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityActionRow(title: String, sub: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = sub, fontSize = 12.sp, color = Color.Gray)
        }
        TextButton(onClick = onClick) {
            Text("Action")
        }
    }
}
