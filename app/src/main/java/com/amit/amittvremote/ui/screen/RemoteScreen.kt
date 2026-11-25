package com.amit.amittvremote.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amit.amittvremote.ui.components.GlassBackground
import com.amit.amittvremote.ui.components.GlassCard
import com.amit.amittvremote.ui.theme.BlobOrange
import com.amit.amittvremote.ui.theme.BlobTeal
import com.amit.amittvremote.ui.theme.TextPrimary
import com.amit.amittvremote.ui.theme.TextSecondary

@Composable
fun RemoteScreen(vm: RemoteViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var pairingCode by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize()) {
        GlassBackground()
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(18.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tv, null, tint = TextPrimary, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("AmitTVRemote", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("SPPL 2K Android TV", color = TextSecondary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(20.dp))

            when (val s = state) {
                is ConnectionState.Idle, is ConnectionState.Discovering, is ConnectionState.DeviceFound -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Select TV", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = { vm.discoverDevices() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Scan Wi-Fi for TVs", color = Color.Black)
                            }
                            
                            if (s is ConnectionState.DeviceFound) {
                                Spacer(Modifier.height(12.dp))
                                s.devices.forEach { tv ->
                                    GlassCard(modifier = Modifier.fillMaxWidth().clickable { vm.startPairing(tv.hostAddress) }) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Tv, null, tint = TextPrimary)
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text(tv.name, color = TextPrimary)
                                                Text(tv.hostAddress, color = TextSecondary, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
                is ConnectionState.Pairing -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Enter Pairing Code", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text("Check your TV screen", color = TextSecondary, fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = pairingCode,
                                onValueChange = { pairingCode = it },
                                label = { Text("Code", color = TextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { vm.submitPairingCode(pairingCode) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Submit")
                            }
                        }
                    }
                }
                is ConnectionState.Connected -> {
                    // ... (Same remote UI as previous code, no changes needed here)
                    Text("Connected to TV", color = BlobTeal, fontSize = 14.sp)
                    // Add your remote buttons here...
                }
                is ConnectionState.Error -> {
                    Text(s.message, color = BlobOrange, fontSize = 14.sp)
                }
            }
        }
    }
}
