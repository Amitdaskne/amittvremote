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
import com.amit.amittvremote.protocol.TvRemoteProtocol.Keys
import com.amit.amittvremote.ui.components.GlassBackground
import com.amit.amittvremote.ui.components.GlassCard
import com.amit.amittvremote.ui.theme.BlobOrange
import com.amit.amittvremote.ui.theme.BlobTeal
import com.amit.amittvremote.ui.theme.TextPrimary
import com.amit.amittvremote.ui.theme.TextSecondary

@Composable
fun RemoteScreen(vm: RemoteViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var ip by remember { mutableStateOf("192.168.1.19") } // Pre-filled from your screenshot
    var pairingCode by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize()) {
        GlassBackground()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Header
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tv, null, tint = TextPrimary, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("AmitTVRemote", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("SPPL 2K Android TV", color = TextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Connection Status / Pairing UI
            when (state) {
                is ConnectionState.Disconnected, is ConnectionState.Error -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Connect to TV", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = ip,
                                onValueChange = { ip = it },
                                label = { Text("TV IP Address", color = TextSecondary) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = TextPrimary,
                                    unfocusedBorderColor = TextSecondary,
                                    cursorColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { vm.startPairing(ip) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                            ) {
                                Text("Start Pairing", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            if (state is ConnectionState.Error) {
                                Spacer(Modifier.height(8.dp))
                                Text((state as ConnectionState.Error).message, color = BlobOrange, fontSize = 12.sp)
                            }
                        }
                    }
                }
                is ConnectionState.Pairing -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Enter Pairing Code", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Text("Check your TV screen for the 6-digit code", color = TextSecondary, fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = pairingCode,
                                onValueChange = { pairingCode = it },
                                label = { Text("Code", color = TextSecondary) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = TextPrimary,
                                    unfocusedBorderColor = TextSecondary,
                                    cursorColor = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { vm.submitPairingCode(pairingCode) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BlobTeal)
                            ) {
                                Text("Submit Code", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                is ConnectionState.Connected -> {
                    // Remote UI
                    Text("Connected to TV", color = BlobTeal, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))

                    // Power / Mute
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RemoteButton(Icons.Default.PowerSettingsNew, "Power", Modifier.weight(1f)) { vm.sendKey(Keys.POWER) }
                        RemoteButton(Icons.Default.VolumeOff, "Mute", Modifier.weight(1f)) { vm.sendKey(Keys.MUTE) }
                    }

                    Spacer(Modifier.height(12.dp))

                    // D-Pad
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DPadButton(Icons.Default.KeyboardArrowUp) { vm.sendKey(Keys.DPAD_UP) }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DPadButton(Icons.Default.KeyboardArrowLeft) { vm.sendKey(Keys.DPAD_LEFT) }
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    Modifier.size(74.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f))
                                        .clickable { vm.sendKey(Keys.DPAD_CENTER) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("OK", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(8.dp))
                                DPadButton(Icons.Default.KeyboardArrowRight) { vm.sendKey(Keys.DPAD_RIGHT) }
                            }
                            DPadButton(Icons.Default.KeyboardArrowDown) { vm.sendKey(Keys.DPAD_DOWN) }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Volume & Channel
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("VOLUME", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                            RemoteButton(Icons.Default.VolumeUp, "Vol +", Modifier.fillMaxWidth()) { vm.sendKey(Keys.VOLUME_UP) }
                            Spacer(Modifier.height(8.dp))
                            RemoteButton(Icons.Default.VolumeDown, "Vol -", Modifier.fillMaxWidth()) { vm.sendKey(Keys.VOLUME_DOWN) }
                        }
                        Column(Modifier.weight(1f)) {
                            Text("CHANNEL", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                            RemoteButton(Icons.Default.KeyboardArrowUp, "CH +", Modifier.fillMaxWidth()) { /* CH+ not directly supported, use DPAD */ vm.sendKey(Keys.DPAD_UP) }
                            Spacer(Modifier.height(8.dp))
                            RemoteButton(Icons.Default.KeyboardArrowDown, "CH -", Modifier.fillMaxWidth()) { vm.sendKey(Keys.DPAD_DOWN) }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Back / Home / Menu / Input
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RemoteButton(Icons.Default.ArrowBack, "Back", Modifier.weight(1f)) { vm.sendKey(Keys.BACK) }
                        RemoteButton(Icons.Default.Home, "Home", Modifier.weight(1f)) { vm.sendKey(Keys.HOME) }
                        RemoteButton(Icons.Default.Menu, "Menu", Modifier.weight(1f)) { vm.sendKey(Keys.MENU) }
                        RemoteButton(Icons.Default.Input, "Input", Modifier.weight(1f)) { vm.sendKey(Keys.TV_INPUT) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RemoteButton(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlassCard(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).clickable { onClick() },
        cornerRadius = 18,
        contentPadding = 14
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = TextPrimary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun DPadButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(58.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(30.dp))
    }
}
