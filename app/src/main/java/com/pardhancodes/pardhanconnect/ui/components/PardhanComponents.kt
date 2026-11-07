package com.pardhancodes.pardhanconnect.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pardhancodes.pardhanconnect.data.model.BluetoothDeviceDomain
import com.pardhancodes.pardhanconnect.data.model.ConnectionState
import com.pardhancodes.pardhanconnect.data.model.PingPongResult
import com.pardhancodes.pardhanconnect.ui.theme.DarkBorder
import com.pardhancodes.pardhanconnect.ui.theme.DarkCard
import com.pardhancodes.pardhanconnect.ui.theme.DarkSurfaceVariant
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldDark
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldGlow
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldMuted
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldPrimary
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldSubtle
import com.pardhancodes.pardhanconnect.ui.theme.StatusConnected
import com.pardhancodes.pardhanconnect.ui.theme.StatusConnecting
import com.pardhancodes.pardhanconnect.ui.theme.StatusDisconnected
import com.pardhancodes.pardhanconnect.ui.theme.StatusError
import com.pardhancodes.pardhanconnect.ui.theme.TextOnEmerald
import com.pardhancodes.pardhanconnect.ui.theme.TextPrimary
import com.pardhancodes.pardhanconnect.ui.theme.TextSecondary
import com.pardhancodes.pardhanconnect.ui.theme.TextTertiary

/**
 * Pardhan Connect Emerald Badge Logo.
 */
@Composable
fun PardhanLogo(
    modifier: Modifier = Modifier,
    size: Int = 64
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(size.dp / 4))
            .background(
                Brush.linearGradient(
                    colors = listOf(DarkSurfaceVariant, DarkCard)
                )
            )
            .border(1.5.dp, EmeraldPrimary.copy(alpha = 0.6f), RoundedCornerShape(size.dp / 4)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Hub,
                contentDescription = "Pardhan Logo",
                tint = EmeraldPrimary,
                modifier = Modifier.size((size * 0.55).dp)
            )
        }
    }
}

/**
 * Connection Status Pill.
 */
@Composable
fun ConnectionStatusBadge(
    state: ConnectionState,
    modifier: Modifier = Modifier
) {
    val (label, bgColor, textColor, icon) = when (state) {
        is ConnectionState.Connected -> Quadruple(
            "CONNECTED: ${state.device.name}",
            StatusConnected.copy(alpha = 0.15f),
            StatusConnected,
            Icons.Default.BluetoothConnected
        )
        is ConnectionState.Connecting -> Quadruple(
            "CONNECTING...",
            StatusConnecting.copy(alpha = 0.15f),
            StatusConnecting,
            Icons.Default.BluetoothSearching
        )
        is ConnectionState.Handshaking -> Quadruple(
            "HANDSHAKING: ${state.step}",
            StatusConnecting.copy(alpha = 0.15f),
            StatusConnecting,
            Icons.Default.NetworkCheck
        )
        is ConnectionState.Scanning -> Quadruple(
            "SCANNING FOR PEERS",
            EmeraldPrimary.copy(alpha = 0.15f),
            EmeraldPrimary,
            Icons.Default.BluetoothSearching
        )
        is ConnectionState.Error -> Quadruple(
            "ERROR: ${state.message}",
            StatusError.copy(alpha = 0.15f),
            StatusError,
            Icons.Default.ErrorOutline
        )
        ConnectionState.Disconnected -> Quadruple(
            "OFF-GRID READY (DISCONNECTED)",
            StatusDisconnected.copy(alpha = 0.15f),
            StatusDisconnected,
            Icons.Default.Bluetooth
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/**
 * Discovered / Paired Device Item Card.
 */
@Composable
fun DeviceItemCard(
    device: BluetoothDeviceDomain,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (device.isPardhanDevice) EmeraldPrimary.copy(alpha = 0.5f) else DarkBorder,
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (device.isPardhanDevice) DarkCard else DarkSurfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (device.isPardhanDevice) EmeraldMuted else DarkBorder.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (device.isPardhanDevice) Icons.Default.Hub else Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = if (device.isPardhanDevice) EmeraldGlow else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (device.isPardhanDevice) {
                            Text(
                                text = "PARDHAN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(EmeraldSubtle)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (device.isPaired) {
                            Text(
                                text = "Paired",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        device.rssi?.let { rssi ->
                            Text(
                                text = "RSSI: $rssi dBm",
                                fontSize = 11.sp,
                                color = if (rssi > -70) EmeraldPrimary else TextTertiary
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onConnectClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = TextOnEmerald
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 14.dp,
                    vertical = 8.dp
                )
            ) {
                Text(
                    text = "Connect",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * PING/PONG Latency Probe Result Card.
 */
@Composable
fun PingPongResultCard(
    result: PingPongResult?,
    onPingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Bi-Directional PING / PONG",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                Button(
                    onClick = onPingClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldDark,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Send PING", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (result != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sequence #${result.sequenceNumber}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = if (result.success) "Latency: ${result.latencyMs} ms" else "Failed",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (result.success) EmeraldGlow else StatusError
                        )
                    }

                    Icon(
                        imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = if (result.success) EmeraldPrimary else StatusError,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Text(
                    text = "No ping test executed yet. Tap 'Send PING' to probe real RFCOMM round-trip latency.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary
                )
            }
        }
    }
}

/**
 * Permission or Bluetooth Disabled Alert Banner.
 */
@Composable
fun AlertBanner(
    title: String,
    message: String,
    actionText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, StatusConnecting.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = StatusConnecting,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusConnecting,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = actionText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
