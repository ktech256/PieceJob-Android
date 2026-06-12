package com.piecejob.core.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piecejob.BuildConfig
import com.piecejob.core.ui.auth.AuthViewModel
import com.piecejob.core.ui.auth.AuthState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationDetailsScreen(
    viewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val firstName by viewModel.firstName.collectAsState()
    val lastName by viewModel.lastName.collectAsState()
    val email by viewModel.email.collectAsState()
    val dob by viewModel.dob.collectAsState()
    val idNumber by viewModel.idNumber.collectAsState()
    val password by viewModel.password.collectAsState()
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    
    val authState by viewModel.authState.collectAsState()
    val isProvider = BuildConfig.FLAVOR == "provider"

    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )

    // Form Validation Logic (CHAIN AUDIT FIX)
    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordMatch = password == confirmPassword && password.isNotEmpty()
    val isPasswordStrong = password.length >= 6
    
    val isFormValid = firstName.trim().isNotBlank() && 
                     lastName.trim().isNotBlank() && 
                     isEmailValid && 
                     dob.isNotBlank() && 
                     idNumber.trim().isNotBlank() && 
                     isPasswordStrong && 
                     isPasswordMatch &&
                     authState !is AuthState.Loading

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onSuccess()
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        viewModel.dob.value = sdf.format(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personal Details", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Provide your details to complete your account registration.",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Start),
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // READ-ONLY VERIFIED FIELDS
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    ReadOnlyField(label = "Country", value = selectedCountry?.name ?: "")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    ReadOnlyField(label = "Phone", value = phoneNumber)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(24.dp))

            // FIRST NAME + LAST NAME (ROW LAYOUT IMPROVEMENT)
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    DetailField(label = "First Name", value = firstName, onValueChange = { viewModel.firstName.value = it })
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    DetailField(label = "Last Name", value = lastName, onValueChange = { viewModel.lastName.value = it })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailField(label = "Email Address", value = email, onValueChange = { viewModel.email.value = it }, keyboardType = KeyboardType.Email)
            
            Spacer(modifier = Modifier.height(16.dp))

            // DATE OF BIRTH (MODERN DATE PICKER IMPROVEMENT)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dob,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date of Birth", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                // Overlay for click detection
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            DetailField(label = "ID / Passport Number", value = idNumber, onValueChange = { viewModel.idNumber.value = it })
            
            Spacer(modifier = Modifier.height(16.dp))

            // Password
            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.password.value = it },
                label = { Text("Password", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = null)
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            if (authState is AuthState.Error) {
                Text(
                    text = (authState as AuthState.Error).message,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { 
                    if (isProvider) {
                        onSuccess() // Navigates to service selection
                    } else {
                        viewModel.register() // Finalizes customer registration
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = isFormValid
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text(
                        text = if (isProvider) "CONTINUE TO TRADES" else "CREATE ACCOUNT",
                        fontSize = 15.sp, 
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ReadOnlyField(label: String, value: String) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        enabled = false,
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = Color.Gray,
            disabledBorderColor = Color.LightGray,
            disabledLabelColor = Color.Gray,
            disabledContainerColor = Color(0xFFF9F9F9)
        )
    )
}

@Composable
fun DetailField(label: String, value: String, onValueChange: (String) -> Unit, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true
    )
}
