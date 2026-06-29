package com.piecejob.core.ui.communication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    jobId: String,
    receiverId: String,
    receiverName: String,
    receiverPhone: String,
    receiverPhoto: String?,
    viewModel: CallViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isCallActive by viewModel.isCallActive.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    var secondsElapsed by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        if (!isCallActive) {
            viewModel.initiateCall(jobId, receiverId)
        }
    }

    LaunchedEffect(connectionStatus) {
        if (connectionStatus == "Connected") {
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
                if (receiverPhoto != null) {
                    AsyncImage(
                        model = receiverPhoto,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = receiverName,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = when (connectionStatus) {
                    "Connected" -> "On Call... ${formatTime(secondsElapsed)}"
                    "Calling..." -> "Calling..."
                    "Connecting..." -> "Connecting..."
                    "Disconnected" -> "Call Ended"
                    else -> connectionStatus
                },
                color = if (connectionStatus == "Connected") Color(0xFF4CAF50) else Color.Gray,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(64.dp))

            // VoIP Controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = { viewModel.toggleMute() },
                    modifier = Modifier.size(64.dp).background(if (isMuted) Color.White else Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Mute", tint = if (isMuted) Color.Black else Color.White)
                }

                IconButton(
                    onClick = { viewModel.toggleSpeaker() },
                    modifier = Modifier.size(64.dp).background(if (isSpeakerOn) Color.White else Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff, contentDescription = "Speaker", tint = if (isSpeakerOn) Color.Black else Color.White)
                }
            }

            Spacer(modifier = Modifier.height(80.dp))

            FloatingActionButton(
                onClick = {
                    viewModel.endCall("ANSWERED", secondsElapsed)
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

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
