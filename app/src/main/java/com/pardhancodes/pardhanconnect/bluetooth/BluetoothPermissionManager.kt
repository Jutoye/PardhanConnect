package com.pardhancodes.pardhanconnect.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages runtime permission checks and declarations across Android API 26-35.
 */
class BluetoothPermissionManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    // Pure readers (no StateFlow access) so they are safe to call during property initialisation.
    private fun readBluetoothOn(): Boolean = bluetoothAdapter?.isEnabled == true

    private fun readPermissionsGranted(): Boolean =
        getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

    private val _isBluetoothEnabled = MutableStateFlow(readBluetoothOn())
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _hasAllPermissions = MutableStateFlow(readPermissionsGranted())
    val hasAllPermissions: StateFlow<Boolean> = _hasAllPermissions.asStateFlow()

    /**
     * Returns the exact set of permissions required based on the running Android API level.
     */
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+) fine-grained runtime permissions
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            // Android 8.0 - 11 (API 26 - 30) permissions
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    /**
     * Evaluates whether all required permissions have been granted.
     */
    fun checkPermissions(): Boolean {
        val perms = getRequiredPermissions()
        val allGranted = perms.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        _hasAllPermissions.value = allGranted
        return allGranted
    }

    /**
     * Checks if the physical Bluetooth adapter is currently powered ON.
     */
    fun isBluetoothOn(): Boolean {
        val enabled = bluetoothAdapter?.isEnabled == true
        _isBluetoothEnabled.value = enabled
        return enabled
    }

    /**
     * Refreshes permission and adapter state.
     */
    fun refreshState() {
        _isBluetoothEnabled.value = isBluetoothOn()
        _hasAllPermissions.value = checkPermissions()
    }
}
