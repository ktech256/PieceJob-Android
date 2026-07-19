package com.piecejob.core.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piecejob.core.ui.auth.AuthViewModel
import com.piecejob.core.ui.auth.AuthState
import com.piecejob.core.ui.components.PieceJobButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPhoneScreen(
    viewModel: AuthViewModel,
    onNavigateToOtp: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit
) {
    var phoneInput by remember { mutableStateOf("") }
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.OtpSent) {
            onNavigateToOtp()
        }
    }

    if (authState is AuthState.PhoneAlreadyRegistered) {
        AlertDialog(
            onDismissRequest = { viewModel.resetState() },
            title = { Text("Already Registered", fontWeight = FontWeight.Bold) },
            text = { Text("The phone number is already registered on PieceJob.") },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.resetState()
                        onNavigateToLogin() 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("SIGN IN")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resetState() }) {
                    Text("USE ANOTHER NUMBER")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phone Number", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Verify your phone number to secure your account.",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(48.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF5F5F5)
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = selectedCountry?.phoneCode ?: "+27", 
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { if (it.length <= 10) phoneInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text("082 123 4567") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                }

                if (authState is AuthState.Error) {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 12.dp, start = 4.dp)
                    )
                }
            }

            PieceJobButton(
                text = "SEND VERIFICATION CODE",
                onClick = { 
                    val fullPhone = "${selectedCountry?.phoneCode ?: "+27"}${phoneInput.removePrefix("0")}"
                    viewModel.requestOtp(fullPhone) 
                },
                isLoading = authState is AuthState.Loading,
                enabled = phoneInput.length >= 9
            )
        }
    }
}
