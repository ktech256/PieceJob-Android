package com.piecejob.core.ui.communication

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    jobId: String,
    receiverId: String,
    receiverName: String,
    receiverPhone: String,
    viewModel: CallViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var callStarted by remember { mutableStateOf(false) }
    var secondsElapsed by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        android.util.Log.d("FORENSIC", "CALL_STARTED | Job: $jobId | To: $receiverId")
        viewModel.initiateCall(jobId, receiverId)
    }

    LaunchedEffect(callStarted) {
        if (callStarted) {
            while (true) {
                delay(1000)
                secondsElapsed++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = receiverName,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (callStarted) "On Call... ${formatTime(secondsElapsed)}" else "Connecting securely...",
                color = Color.Gray,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(100.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (!callStarted) {
                    FloatingActionButton(
                        onClick = {
                            callStarted = true
                            // Open system dialer as the actual calling mechanism
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$receiverPhone")
                            }
                            context.startActivity(intent)
                        },
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Start Call", modifier = Modifier.size(32.dp))
                    }
                }

                FloatingActionButton(
                    onClick = {
                        if (callStarted) {
                            viewModel.endCall("ANSWERED", secondsElapsed)
                        } else {
                            viewModel.endCall("CANCELLED", 0)
                        }
                        onBack()
                    },
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "End Call", modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
