package com.pardhancodes.pardhanconnect.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pardhancodes.pardhanconnect.data.model.ConnectionState
import com.pardhancodes.pardhanconnect.data.model.PingPongResult
import com.pardhancodes.pardhanconnect.ui.components.ConnectionStatusBadge
import com.pardhancodes.pardhanconnect.ui.components.PingPongResultCard
import com.pardhancodes.pardhanconnect.ui.theme.DarkBackground
import com.pardhancodes.pardhanconnect.ui.theme.DarkBorder
import com.pardhancodes.pardhanconnect.ui.theme.DarkCard
import com.pardhancodes.pardhanconnect.ui.theme.DarkSurface
import com.pardhancodes.pardhanconnect.ui.theme.DarkSurfaceVariant
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldDark
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldGlow
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldMuted
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldPrimary
import com.pardhancodes.pardhanconnect.ui.theme.StatusConnected
import com.pardhancodes.pardhanconnect.ui.theme.StatusConnecting
import com.pardhancodes.pardhanconnect.ui.theme.StatusError
import com.pardhancodes.pardhanconnect.ui.theme.TextOnEmerald
import com.pardhancodes.pardhanconnect.ui.theme.TextPrimary
import com.pardhancodes.pardhanconnect.ui.theme.TextSecondary
import com.pardhancodes.pardhanconnect.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothConnectionScreen(
    connectionState: ConnectionState,
    lastPingResult: PingPongResult?,
    rawPacketLogs: List<String>,
    onSendPing: () -> Unit,
    onDisconnect: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateNearby: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bluetooth Hub",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                ConnectionStatusBadge(
                    state = connectionState,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Connected Device Details & Handshake
            when (connectionState) {
                is ConnectionState.Connected -> {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, EmeraldPrimary, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkCard)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldMuted),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.BluetoothConnected,
                                                contentDescription = null,
                                                tint = EmeraldGlow,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = connectionState.device.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = connectionState.device.address,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextTertiary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = onDisconnect,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = StatusError.copy(alpha = 0.2f),
                                            contentColor = StatusError
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LinkOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.size(4.dp))
                                        Text("Disconnect", fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Validated Handshake Info
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurfaceVariant)
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Validated Protocol Handshake",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = EmeraldPrimary
                                        )
                                    }

                                    DetailRow(label = "Protocol Version", value = connectionState.handshake.protocolVersion)
                                    DetailRow(label = "Application", value = connectionState.handshake.appName)
                                    DetailRow(label = "Peer Node ID", value = connectionState.handshake.peerNodeId)
                                    DetailRow(label = "Peer Session ID", value = connectionState.handshake.peerSessionId)
                                    DetailRow(label = "Local Session ID", value = connectionState.handshake.localSessionId)
                                }
                            }
                        }
                    }

                    // PING / PONG Probe Card
                    item {
                        PingPongResultCard(
                            result = lastPingResult,
                            onPingClick = onSendPing
                        )
                    }
                }

                is ConnectionState.Connecting, is ConnectionState.Handshaking -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkCard)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = EmeraldPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = if (connectionState is ConnectionState.Handshaking)
                                        "Negotiating Handshake..." else "Opening RFCOMM Socket...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Exchanging protocol versions and cryptographic session tokens off-grid.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                is ConnectionState.Error -> {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, StatusError.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkCard)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Connection Failed",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = StatusError,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = connectionState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Button(
                                    onClick = onNavigateNearby,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldPrimary,
                                        contentColor = TextOnEmerald
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Retry Discovery")
                                }
                            }
                        }
                    }
                }

                ConnectionState.Scanning -> { }

                ConnectionState.Disconnected -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkCard)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "No Active RFCOMM Connection",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Scan for nearby Pardhan Connect nodes or wait for another phone to connect to your RFCOMM server.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Button(
                                    onClick = onNavigateNearby,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldPrimary,
                                        contentColor = TextOnEmerald
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Discover Nearby Devices", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Live Protocol Packet Logs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE PACKET LOGS",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "${rawPacketLogs.size} events",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF060911))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (rawPacketLogs.isEmpty()) {
                            item {
                                Text(
                                    text = "No packet activity yet. Connect or send PING to view framed raw IO frames.",
                                    color = TextTertiary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            items(rawPacketLogs) { logEntry ->
                                Text(
                                    text = logEntry,
                                    color = if (logEntry.contains("Rx") || logEntry.contains("Success"))
                                        EmeraldGlow else TextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}
