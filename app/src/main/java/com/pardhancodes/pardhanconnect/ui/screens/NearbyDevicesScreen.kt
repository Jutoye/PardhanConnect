package com.pardhancodes.pardhanconnect.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pardhancodes.pardhanconnect.data.model.BluetoothDeviceDomain
import com.pardhancodes.pardhanconnect.data.model.ConnectionState
import com.pardhancodes.pardhanconnect.ui.components.AlertBanner
import com.pardhancodes.pardhanconnect.ui.components.DeviceItemCard
import com.pardhancodes.pardhanconnect.ui.theme.DarkBackground
import com.pardhancodes.pardhanconnect.ui.theme.DarkBorder
import com.pardhancodes.pardhanconnect.ui.theme.DarkCard
import com.pardhancodes.pardhanconnect.ui.theme.DarkSurface
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldDark
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldGlow
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldPrimary
import com.pardhancodes.pardhanconnect.ui.theme.StatusConnecting
import com.pardhancodes.pardhanconnect.ui.theme.StatusError
import com.pardhancodes.pardhanconnect.ui.theme.TextOnEmerald
import com.pardhancodes.pardhanconnect.ui.theme.TextPrimary
import com.pardhancodes.pardhanconnect.ui.theme.TextSecondary
import com.pardhancodes.pardhanconnect.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyDevicesScreen(
    discoveredDevices: List<BluetoothDeviceDomain>,
    pairedDevices: List<BluetoothDeviceDomain>,
    isDiscovering: Boolean,
    isAdvertising: Boolean,
    errorMessage: String?,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onConnectToDevice: (BluetoothDeviceDomain) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPardhanOnly by remember { mutableStateOf(false) }

    val filteredDiscovered = if (showPardhanOnly) {
        discoveredDevices.filter { it.isPardhanDevice }
    } else {
        discoveredDevices
    }

    val filteredPaired = if (showPardhanOnly) {
        pairedDevices.filter { it.isPardhanDevice }
    } else {
        pairedDevices
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Nearby Devices",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isDiscovering) "Scanning Classic & BLE..." else "Off-grid discovery idle",
                            fontSize = 11.sp,
                            color = if (isDiscovering) EmeraldGlow else TextSecondary
                        )
                    }
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
                actions = {
                    if (isDiscovering) {
                        IconButton(onClick = onStopDiscovery) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Scanning",
                                tint = StatusConnecting
                            )
                        }
                    } else {
                        IconButton(onClick = onStartDiscovery) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Scan",
                                tint = EmeraldPrimary
                            )
                        }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Discovery status & controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = showPardhanOnly,
                        onClick = { showPardhanOnly = !showPardhanOnly },
                        label = { Text("Pardhan Devices Only") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldDark,
                            selectedLabelColor = TextPrimary,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        )
                    )

                    Button(
                        onClick = {
                            if (isDiscovering) onStopDiscovery() else onStartDiscovery()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDiscovering) StatusConnecting else EmeraldPrimary,
                            contentColor = TextOnEmerald
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isDiscovering) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = TextOnEmerald,
                                    strokeWidth = 2.dp
                                )
                                Text("Scanning")
                            }
                        } else {
                            Text("Scan Nearby")
                        }
                    }
                }
            }

            // Error message display
            if (errorMessage != null) {
                item {
                    Text(
                        text = "Discovery notice: $errorMessage",
                        color = StatusError,
                        fontSize = 12.sp
                    )
                }
            }

            // Paired Devices Section
            if (filteredPaired.isNotEmpty()) {
                item {
                    Text(
                        text = "PAIRED DEVICES (${filteredPaired.size})",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(filteredPaired, key = { "paired_${it.address}" }) { device ->
                    DeviceItemCard(
                        device = device,
                        onConnectClick = { onConnectToDevice(device) }
                    )
                }
            }

            // Discovered Nearby Devices Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DISCOVERED NEARBY (${filteredDiscovered.size})",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    if (isDiscovering) {
                        Text(
                            text = "Inquiry active",
                            fontSize = 11.sp,
                            color = EmeraldGlow
                        )
                    }
                }
            }

            if (filteredDiscovered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCard)
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BluetoothSearching,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = if (isDiscovering) "Listening for nearby Pardhan beacons..." else "No devices discovered yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            if (!isDiscovering) {
                                Text(
                                    text = "Ensure both physical phones have Bluetooth ON and are within 10 meters.",
                                    fontSize = 12.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                }
            } else {
                items(filteredDiscovered, key = { "discovered_${it.address}" }) { device ->
                    DeviceItemCard(
                        device = device,
                        onConnectClick = { onConnectToDevice(device) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
