package com.piecejob.core.ui.auth

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piecejob.core.ui.theme.PieceJobTheme
import com.piecejob.core.ui.components.PieceJobButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToOtp: (String) -> Unit,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        android.util.Log.e("PIECEJOB_LOGIN", "Login screen created")
    }

    val prefilledPhone by viewModel.loginIdentifier.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val state by viewModel.authState.collectAsState()

    LaunchedEffect(prefilledPhone) {
        if (prefilledPhone.isNotBlank()) {
            // Remove country code for display if it matches
            val code = selectedCountry?.phoneCode ?: "+27"
            identifier = if (prefilledPhone.startsWith(code)) {
                "0" + prefilledPhone.removePrefix(code)
            } else {
                prefilledPhone
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log In", fontWeight = FontWeight.Bold) },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome back! Please enter your phone number and password to continue.",
                fontSize = 16.sp,
                color = Color.Gray,
                lineHeight = 24.sp,
                modifier = Modifier.align(Alignment.Start)
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
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    placeholder = { Text("071 234 5678") }
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            PieceJobButton(
                text = "LOG IN",
                onClick = { 
                    val fullIdentifier = if (identifier.all { it.isDigit() || it == ' ' }) {
                        "${selectedCountry?.phoneCode ?: "+27"}${identifier.trim().removePrefix("0")}"
                    } else {
                        identifier.trim()
                    }
                    viewModel.login(fullIdentifier, password) 
                },
                isLoading = state is AuthState.Loading,
                enabled = identifier.isNotBlank() && password.isNotBlank()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TextButton(onClick = { onNavigateToOtp(identifier) }) {
                Text("Login with OTP", fontWeight = FontWeight.Bold)
            }

            if (state is AuthState.Error) {
                Text(
                    text = (state as AuthState.Error).message,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            LaunchedEffect(state) {
                if (state is AuthState.Authenticated) {
                    onLoginSuccess()
                }
            }
        }
    }
}
