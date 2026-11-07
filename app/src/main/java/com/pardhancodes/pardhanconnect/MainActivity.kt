package com.pardhancodes.pardhanconnect

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pardhancodes.pardhanconnect.ui.MainViewModel
import com.pardhancodes.pardhanconnect.ui.screens.BluetoothConnectionScreen
import com.pardhancodes.pardhanconnect.ui.screens.HomeScreen
import com.pardhancodes.pardhanconnect.ui.screens.NearbyDevicesScreen
import com.pardhancodes.pardhanconnect.ui.screens.SettingsScreen
import com.pardhancodes.pardhanconnect.ui.screens.SplashScreen
import com.pardhancodes.pardhanconnect.ui.theme.DarkBackground
import com.pardhancodes.pardhanconnect.ui.theme.PardhanConnectTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PardhanConnectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val navController = rememberNavController()

                    // Collect StateFlows
                    val connectionState by viewModel.connectionState.collectAsState()
                    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
                    val pairedDevices by viewModel.pairedDevices.collectAsState()
                    val isDiscovering by viewModel.isDiscovering.collectAsState()
                    val isAdvertising by viewModel.isAdvertising.collectAsState()
                    val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsState()
                    val hasPermissions by viewModel.hasAllPermissions.collectAsState()
                    val lastPingResult by viewModel.lastPingPongResult.collectAsState()
                    val rawPacketLogs by viewModel.rawPacketLogs.collectAsState()

                    // Runtime Permission Launcher
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { _ ->
                        viewModel.refreshStateAndStart()
                    }

                    // Enable Bluetooth Activity Result Launcher
                    val enableBluetoothLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) {
                        viewModel.refreshStateAndStart()
                    }

                    fun requestBluetoothPermissions() {
                        val perms = viewModel.bluetoothManager.permissionManager.getRequiredPermissions()
                        permissionLauncher.launch(perms)
                    }

                    fun promptEnableBluetooth() {
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        try {
                            enableBluetoothLauncher.launch(enableBtIntent)
                        } catch (e: Exception) {
                            // Fallback if intent cannot be launched
                        }
                    }

                    // Initial permission and adapter verification
                    LaunchedEffect(Unit) {
                        if (!hasPermissions) {
                            requestBluetoothPermissions()
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {
                        composable("splash") {
                            SplashScreen(
                                onSplashFinished = {
                                    navController.navigate("home") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                connectionState = connectionState,
                                isBluetoothEnabled = isBluetoothEnabled,
                                hasPermissions = hasPermissions,
                                onNavigateNearby = { navController.navigate("nearby") },
                                onNavigateConnection = { navController.navigate("connection") },
                                onNavigateSettings = { navController.navigate("settings") },
                                onRequestPermissions = { requestBluetoothPermissions() },
                                onEnableBluetooth = { promptEnableBluetooth() }
                            )
                        }

                        composable("nearby") {
                            NearbyDevicesScreen(
                                discoveredDevices = discoveredDevices,
                                pairedDevices = pairedDevices,
                                isDiscovering = isDiscovering,
                                isAdvertising = isAdvertising,
                                errorMessage = null,
                                onStartDiscovery = { viewModel.startDiscovery() },
                                onStopDiscovery = { viewModel.stopDiscovery() },
                                onConnectToDevice = { device ->
                                    viewModel.connectToDevice(device)
                                    navController.navigate("connection")
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("connection") {
                            BluetoothConnectionScreen(
                                connectionState = connectionState,
                                lastPingResult = lastPingResult,
                                rawPacketLogs = rawPacketLogs,
                                onSendPing = { viewModel.sendPing() },
                                onDisconnect = { viewModel.disconnect() },
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateNearby = { navController.navigate("nearby") }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                localNodeId = viewModel.localNodeId,
                                isBluetoothEnabled = isBluetoothEnabled,
                                hasPermissions = hasPermissions,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshStateAndStart()
    }
}
