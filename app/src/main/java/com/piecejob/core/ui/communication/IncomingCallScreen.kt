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

@Composable
fun IncomingCallScreen(
    jobId: String,
    callerId: String,
    callId: String,
    callerName: String,
    callerPhone: String,
    viewModel: CallViewModel = hiltViewModel(),
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val context = LocalContext.current
    
    LaunchedEffect(callId) {
        viewModel.setCallId(callId)
        viewModel.startRinging(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopRinging()
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
                text = callerName,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Incoming PieceJob Call...",
                color = Color.Gray,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(100.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FloatingActionButton(
                    onClick = {
                        android.util.Log.d("FORENSIC", "CALL_ACCEPTED | Job: $jobId")
                        viewModel.stopRinging()
                        viewModel.endCall("ANSWERED", 0) 
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$callerPhone")
                        }
                        context.startActivity(intent)
                        onAccept()
                    },
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Accept Call", modifier = Modifier.size(32.dp))
                }

                FloatingActionButton(
                    onClick = {
                        android.util.Log.d("FORENSIC", "CALL_REJECTED | Job: $jobId")
                        viewModel.stopRinging()
                        viewModel.endCall("REJECTED", 0)
                        onReject()
                    },
                    containerColor = Color(0xFFD32F2F),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "Reject Call", modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}
