package com.pardhancodes.pardhanconnect.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pardhancodes.pardhanconnect.data.model.BluetoothDeviceDomain
import com.pardhancodes.pardhanconnect.data.model.ConnectionState
import com.pardhancodes.pardhanconnect.ui.components.AlertBanner
import com.pardhancodes.pardhanconnect.ui.components.ConnectionStatusBadge
import com.pardhancodes.pardhanconnect.ui.components.PardhanLogo
import com.pardhancodes.pardhanconnect.ui.theme.DarkBackground
import com.pardhancodes.pardhanconnect.ui.theme.DarkBorder
import com.pardhancodes.pardhanconnect.ui.theme.DarkCard
import com.pardhancodes.pardhanconnect.ui.theme.DarkSurface
import com.pardhancodes.pardhanconnect.ui.theme.DarkSurfaceVariant
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldDark
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldGlow
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldMuted
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldPrimary
import com.pardhancodes.pardhanconnect.ui.theme.EmeraldSubtle
import com.pardhancodes.pardhanconnect.ui.theme.TextOnEmerald
import com.pardhancodes.pardhanconnect.ui.theme.TextPrimary
import com.pardhancodes.pardhanconnect.ui.theme.TextSecondary
import com.pardhancodes.pardhanconnect.ui.theme.TextTertiary

enum class HomeTab(val title: String, val icon: ImageVector) {
    CHATS("Chats", Icons.Default.Chat),
    GROUPS("Groups", Icons.Default.Groups),
    CALLS("Calls", Icons.Default.Call),
    STATUS("Status", Icons.Default.WifiTethering),
    TRANSFERS("Transfers", Icons.Default.FolderShared)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    connectionState: ConnectionState,
    isBluetoothEnabled: Boolean,
    hasPermissions: Boolean,
    onNavigateNearby: () -> Unit,
    onNavigateConnection: () -> Unit,
    onNavigateSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = HomeTab.values()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PardhanLogo(size = 36)
                        Column {
                            Text(
                                text = "Pardhan Connect",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Connect Without Internet.",
                                fontSize = 11.sp,
                                color = EmeraldGlow
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateNearby) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Nearby Devices",
                            tint = EmeraldPrimary
                        )
                    }
                    IconButton(onClick = onNavigateConnection) {
                        Icon(
                            imageVector = if (connectionState is ConnectionState.Connected)
                                Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                            contentDescription = "Connection Status",
                            tint = if (connectionState is ConnectionState.Connected) EmeraldGlow else TextSecondary
                        )
                    }
                    IconButton(onClick = onNavigateSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateNearby,
                containerColor = EmeraldPrimary,
                contentColor = TextOnEmerald,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = "Scan Nearby"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Status Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .clickable { onNavigateConnection() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                ConnectionStatusBadge(
                    state = connectionState,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Permission / BT State Warnings
            if (!hasPermissions) {
                Box(modifier = Modifier.padding(16.dp)) {
                    AlertBanner(
                        title = "Permissions Needed",
                        message = "Nearby Bluetooth discovery and connection permissions are required to operate off-grid.",
                        actionText = "Grant",
                        onActionClick = onRequestPermissions
                    )
                }
            } else if (!isBluetoothEnabled) {
                Box(modifier = Modifier.padding(16.dp)) {
                    AlertBanner(
                        title = "Bluetooth Disabled",
                        message = "Turn on Bluetooth to discover and pair with nearby devices.",
                        actionText = "Enable",
                        onActionClick = onEnableBluetooth
                    )
                }
            }

            // Scrollable Tab Row for Home Navigation
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkSurface,
                contentColor = EmeraldPrimary,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = EmeraldPrimary,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTabIndex == index) EmeraldPrimary else TextTertiary
                                )
                                Text(
                                    text = tab.title,
                                    color = if (selectedTabIndex == index) EmeraldPrimary else TextSecondary,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                }
            }

            // Tab Content Placeholders (Explicit Phase 1 Foundation Placeholders)
            val currentTab = tabs[selectedTabIndex]
            Phase1PlaceholderView(
                tab = currentTab,
                connectionState = connectionState,
                onNavigateNearby = onNavigateNearby,
                onNavigateConnection = onNavigateConnection
            )
        }
    }
}

/**
 * Clean, explicit Phase 1 Placeholder explaining that this screen will be implemented
 * on top of the validated Bluetooth RFCOMM foundation.
 */
@Composable
fun Phase1PlaceholderView(
    tab: HomeTab,
    connectionState: ConnectionState,
    onNavigateNearby: () -> Unit,
    onNavigateConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(DarkCard)
                    .border(1.5.dp, EmeraldPrimary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${tab.title} — Phase 1 Foundation",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (tab) {
                    HomeTab.CHATS -> "Off-grid direct 1-on-1 text messaging will become available in Phase 2 once the RFCOMM mesh connection engine is established and verified."
                    HomeTab.GROUPS -> "Multi-hop decentralized mesh group channels will be unlocked when multi-peer routing and duplicate packet detection are integrated."
                    HomeTab.CALLS -> "Low-latency offline peer-to-peer audio calls over RFCOMM stream sockets will be enabled in Phase 3."
                    HomeTab.STATUS -> "Dispersed ephemeral status broadcast beacons across nearby Bluetooth nodes without internet."
                    HomeTab.TRANSFERS -> "Chunked binary file transfers with cryptographic hash validation off-grid."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
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
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Phase 1 Bluetooth Deliverables Active",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "• Dual Bluetooth Discovery (Classic Inquiry + BLE Scanner)\n" +
                                "• RFCOMM Server & Client connection engines\n" +
                                "• Protocol Handshake validation (v1.0)\n" +
                                "• Bidirectional PING/PONG latency probes\n" +
                                "• Mesh routing architectural interfaces prepared",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateNearby,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = TextOnEmerald
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Scan Nearby", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onNavigateConnection,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceVariant,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Connection Hub")
                        }
                    }
                }
            }
        }
    }
}
