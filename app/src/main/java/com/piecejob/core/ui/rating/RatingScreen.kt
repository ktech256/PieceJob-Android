package com.piecejob.core.ui.rating

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.piecejob.BuildConfig

import androidx.activity.compose.BackHandler

@Composable
fun RatingScreen(
    jobId: String,
    viewModel: RatingViewModel = hiltViewModel(),
    onSuccess: () -> Unit
) {
    val job by viewModel.job.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()
    val error by viewModel.error.collectAsState()

    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }

    // Prevent going back to tracking screen, mark as dismissed if user tries to back out
    BackHandler {
        viewModel.dismissRating(jobId)
    }

    LaunchedEffect(jobId) {
        viewModel.loadJob(jobId)
    }

    LaunchedEffect(isSuccess) {
        if (isSuccess) onSuccess()
    }

    LaunchedEffect(job) {
        val isProviderApp = BuildConfig.FLAVOR == "provider"
        val alreadyRated = if (isProviderApp) job?.providerRated == true else job?.customerRated == true
        val alreadyDismissed = if (isProviderApp) job?.providerRatingDismissed == true else job?.customerRatingDismissed == true
        if (alreadyRated || alreadyDismissed) {
            android.util.Log.d("FORENSIC", "RATING_SCREEN | Job already rated or dismissed. Closing.")
            onSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Job Completed!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Profile Picture
        val imageUrl = if (BuildConfig.FLAVOR == "provider") {
            job?.customerInfo?.profilePicture
        } else {
            job?.providerInfo?.profilePicture
        }

        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = Color(0xFFF5F5F5)
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val isProviderApp = BuildConfig.FLAVOR == "provider"
        val displayName = if (isProviderApp) {
            job?.customerInfo?.let { "${it.firstName} ${it.lastName}" } ?: job?.recipientName ?: "Customer"
        } else {
            job?.providerInfo?.let { "${it.firstName} ${it.lastName}" } ?: "Provider"
        }
        
        val displayRole = if (isProviderApp) "Customer" else job?.serviceName ?: job?.serviceCode ?: "Professional"

        Text(
            text = "Rate $displayName",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = displayRole,
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Star Rating
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            for (i in 1..5) {
                Icon(
                    imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if (i <= rating) Color(0xFFFFA000) else Color.LightGray,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { rating = i }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Add a comment (Optional)") },
            placeholder = { Text("Share your experience...") },
            shape = RoundedCornerShape(12.dp),
            minLines = 3
        )

        Spacer(modifier = Modifier.weight(1f))

        if (error != null) {
            Text(text = error!!, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
        }

        Button(
            onClick = { viewModel.submitRating(jobId, rating, comment) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isSubmitting && rating > 0,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("SUBMIT RATING", fontWeight = FontWeight.Black)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = { viewModel.dismissRating(jobId) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting
        ) {
            Text("NOT NOW, MAYBE LATER", color = Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}
