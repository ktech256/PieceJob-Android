package com.piecejob.provider.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderBankDetailsScreen(
    viewModel: ProviderBankDetailsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val bankDetails by viewModel.bankDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var bankName by remember { mutableStateOf("") }
    var accountHolder by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var branchCode by remember { mutableStateOf("") }

    LaunchedEffect(bankDetails) {
        bankDetails?.let {
            bankName = it.bankName
            accountHolder = it.accountHolder
            accountNumber = "********" // Masked for safety
            branchCode = it.branchCode
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bank Details", fontWeight = FontWeight.Bold) },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Enter the account where you want to receive your earnings.",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("Bank Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = accountHolder,
                onValueChange = { accountHolder = it },
                label = { Text("Account Holder Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = accountNumber,
                onValueChange = { if(it.all { c -> c.isDigit() }) accountNumber = it },
                label = { Text("Account Number") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = branchCode,
                onValueChange = { branchCode = it },
                label = { Text("Branch Code") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    viewModel.updateBankDetails(bankName, accountHolder, accountNumber, branchCode)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("SAVE BANK DETAILS", fontWeight = FontWeight.Black)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Security Note: Any changes to bank details will trigger an OTP verification for your protection.",
                fontSize = 11.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )
        }
    }
}
