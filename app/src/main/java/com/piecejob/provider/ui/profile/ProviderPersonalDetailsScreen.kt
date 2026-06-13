package com.piecejob.provider.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.piecejob.core.data.remote.dto.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderPersonalDetailsScreen(
    viewModel: ProviderPersonalDetailsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSuccess by viewModel.isUpdateSuccess.collectAsState()
    
    val isVerified = profile?.verificationStatus == "APPROVED"

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var proofOfResidenceUrl by remember { mutableStateOf<String?>(null) }

    val hasAddressChanged = (province != (profile?.userId?.province ?: "")) ||
                           (city != (profile?.userId?.city ?: "")) ||
                           (address != (profile?.userId?.address ?: ""))

    LaunchedEffect(profile) {
        profile?.let {
            firstName = it.userId.firstName
            lastName = it.userId.lastName
            email = it.userId.email
            phone = it.userId.phoneNumber
            idNumber = it.idOrPassportNumber
            gender = it.gender
            dob = it.dob
            country = it.countryCode
            province = it.userId.province ?: ""
            city = it.userId.city ?: ""
            address = it.userId.address ?: ""
        }
    }

    if (isSuccess) {
        LaunchedEffect(Unit) {
            viewModel.resetSuccessState()
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
        if (isLoading && profile == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Photo
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = Color.LightGray
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.padding(24.dp), tint = Color.White)
                    }
                    IconButton(
                        onClick = { /* Pick Image */ },
                        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                ProfileField("First Name", firstName, enabled = !isVerified) { firstName = it }
                ProfileField("Last Name", lastName, enabled = !isVerified) { lastName = it }
                ProfileField("ID / Passport Number", idNumber, enabled = false) { idNumber = it }
                ProfileField("Phone Number", phone, enabled = false) { phone = it }
                ProfileField("Email Address", email) { email = it }
                ProfileField("Gender", gender, enabled = !isVerified) { gender = it }
                ProfileField("Date of Birth", dob, enabled = false) { dob = it }
                ProfileField("Country", country, enabled = false) { country = it }
                ProfileField("Province / State", province) { province = it }
                ProfileField("City", city) { city = it }
                ProfileField("Address", address) { address = it }

                if (isVerified && hasAddressChanged) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "ADDRESS REVERIFICATION REQUIRED",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "You are changing your verified address. A new Proof of Residence is required for approval.",
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { /* File Picker */ },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(if (proofOfResidenceUrl != null) "Document Uploaded" else "Upload Proof of Residence")
                            }
                        }
                    }
                }

                if (profile?.userId?.pendingAddress != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "PENDING ADDRESS CHANGE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFE65100))
                            Text(text = "${profile?.userId?.pendingAddress?.address}, ${profile?.userId?.pendingAddress?.city}", fontSize = 11.sp)
                            Text(text = "Status: ${profile?.userId?.pendingAddress?.status}", fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = {
                        viewModel.updateProfile(
                            UpdateProfileRequest(
                                firstName = firstName,
                                lastName = lastName,
                                gender = gender,
                                dob = dob,
                                profilePhoto = null,
                                city = city,
                                province = province,
                                address = address,
                                emergencyContact = null,
                                proofOfResidenceUrl = proofOfResidenceUrl
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("SAVE CHANGES", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileField(label: String, value: String, enabled: Boolean = true, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = Color.Gray,
            disabledBorderColor = Color.LightGray,
            disabledLabelColor = Color.Gray
        )
    )
}
