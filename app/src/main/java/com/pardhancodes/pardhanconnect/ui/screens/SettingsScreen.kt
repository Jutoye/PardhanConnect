package com.pardhancodes.pardhanconnect.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PermDeviceInformation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pardhancodes.pardhanconnect.data.model.PardhanProtocol
import com.pardhancodes.pardhanconnect.ui.theme.DarkBackground
import com.pardhancodes.pardhanconnect.ui.theme.DarkBorder
import com.pardhancodes.pardhanconnect.ui.theme.DarkCard
import com.pardhancodes.pardhanconnect.ui.theme.DarkSurface
import com.pardhancodes.pardhanconnect.ui.theme.DarkSurfaceVariant
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldGlow
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldMuted
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldPrimary
import com.pardhancodes.pardhanconnect.ui.theme.StatusConnected
import com.pardhancodes.pardhanconnect.ui.theme.StatusError
import com.pardhancodes.pardhanconnect.ui.theme.TextPrimary
import com.pardhancodes.pardhanconnect.ui.theme.TextSecondary
import com.pardhancodes.pardhanconnect.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    localNodeId: String,
    isBluetoothEnabled: Boolean,
    hasPermissions: Boolean,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var allowRelayMesh by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Diagnostics",
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
                Text(
                    text = "NODE IDENTITY",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PermDeviceInformation,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Local Mesh Node",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        SettingRow(title = "Node Identifier", value = localNodeId)
                        SettingRow(title = "Application ID", value = "com.pardhancodes.pardhanconnect")
                        SettingRow(title = "Protocol Version", value = PardhanProtocol.PROTOCOL_VERSION)
                        SettingRow(title = "Service UUID", value = PardhanProtocol.SERVICE_UUID.toString())
                    }
                }
            }

            item {
                Text(
                    text = "BLUETOOTH SUBSYSTEM",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    imageVector = Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    tint = if (isBluetoothEnabled) EmeraldPrimary else StatusError,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Bluetooth Adapter",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = if (isBluetoothEnabled) "POWERED ON" else "OFF",
                                color = if (isBluetoothEnabled) EmeraldGlow else StatusError,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

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
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = if (hasPermissions) EmeraldPrimary else StatusError,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Runtime Permissions",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = if (hasPermissions) "GRANTED" else "MISSING",
                                color = if (hasPermissions) EmeraldGlow else StatusError,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "MESH ROUTING PREPARATION",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Relay Eligibility Flag",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Allows Phase 2 mesh engine to use this node as a hop relay for distant peers.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = allowRelayMesh,
                                onCheckedChange = { allowRelayMesh = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldPrimary,
                                    checkedTrackColor = EmeraldMuted
                                )
                            )
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
private fun SettingRow(title: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, fontSize = 11.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 12.sp,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
