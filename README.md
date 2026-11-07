# PARDHAN CONNECT — PHASE 1
### Native Android Bluetooth Foundation
**Application ID:** `com.pardhancodes.pardhanconnect`  
**Application Name:** Pardhan Connect  
**Tagline:** Connect Without Internet.  

---

## 📌 Phase 1 Architecture Overview

Pardhan Connect Phase 1 delivers a zero-internet Bluetooth communication engine built natively for Android using **Kotlin**, **Jetpack Compose**, and **Material 3**. It establishes the fundamental RFCOMM socket layer, dual-mode discovery (Classic + BLE), bi-directional handshake protocol negotiation, and latency probe testing required before multi-hop mesh routing is unlocked in Phase 2.

```
┌────────────────────────────────────────────────────────────┐
│                  Pardhan Connect App                       │
├─────────────────────────────┬──────────────────────────────┤
│ Jetpack Compose UI (M3)     │ MainViewModel (StateFlow)    │
│  - SplashScreen (Animated)  │  - ConnectionState           │
│  - HomeScreen (Placeholders)│  - DiscoveredDevices         │
│  - NearbyDevicesScreen      │  - PingPongResults           │
│  - BluetoothConnectionScreen│  - PacketLogs                │
│  - SettingsScreen           │                              │
├─────────────────────────────┴──────────────────────────────┤
│               BluetoothManager (Facade)                    │
├─────────────────────┬──────────────────┬───────────────────┤
│ DiscoveryManager    │ ServerManager    │ ClientManager     │
│  - Classic Inquiry  │  - RFCOMM Server │  - RFCOMM Client  │
│  - BLE Scanner      │  - Service UUID  │  - Target Connect │
│  - BLE Advertiser   │  - Listen Loop   │  - Discovery Stop │
├─────────────────────┴──────────────────┴───────────────────┤
│ HandshakeManager & Protocol Framing                        │
│  - Protocol: PARDHAN-v1.0 (UUID: 7c844111-...)             │
│  - Packet Header: [0x50, 0x43] [Type] [Length] [Payload]   │
│  - Handshake: INIT -> ACK (Validated)                      │
│  - Bidirectional PING -> PONG Latency Probing              │
├────────────────────────────────────────────────────────────┤
│ Mesh Preparation Layer (Interfaces for Phase 2)            │
│  - PeerIdentity, PeerConnection, MessageRoutingEngine      │
│  - RelayEligibilityDecider, DuplicatePacketCache (LRU)     │
│  - HopCountEnforcer (Max 7 Hops)                           │
└────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Build & APK Generation Instructions

### Prerequisites
1. **Android Studio**: Ladybug, Koala, Jellyfish, or Hedgehog (2023.3.1+)
2. **JDK**: Version 17 or higher
3. **Android SDK**: Compile SDK 35, Minimum SDK 26 (Android 8.0 Oreo+)

### 1. Build via Android Studio
1. Launch Android Studio.
2. Select **Open** and choose the `android/` directory.
3. Allow Gradle Sync to finish.
4. Select `Build` > `Build Bundle(s) / APK(s)` > `Build APK(s)`.
5. The generated debug APK will be located at:
   `android/app/build/outputs/apk/debug/app-debug.apk`

### 2. Build via Command Line
```bash
cd android
./gradlew clean assembleDebug
```
To install directly to a connected USB or Wi-Fi adb device:
```bash
./gradlew installDebug
```

---

## 📱 Two Physical Android Phones: 10-Step Testing Guide

To verify real-world off-grid Bluetooth RFCOMM connectivity:

### Step 1: Install APK on Both Phones
Install the debug APK on **Phone A** (e.g., Pixel 8 Pro) and **Phone B** (e.g., Samsung Galaxy S24).

### Step 2: Enable Bluetooth
Ensure the Bluetooth toggle is ON in Android Settings on both devices.

### Step 3: Grant Runtime Permissions
Open **Pardhan Connect** on both phones. When prompted, grant:
- Android 12+ (API 31+): **Nearby devices** (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`).
- Android 8-11: **Location** (`ACCESS_FINE_LOCATION`) and Bluetooth.

### Step 4: Background Server & BLE Advertising
Observe on both phones:
- The RFCOMM Server socket starts listening on UUID `7c844111-53b9-4a0b-9ef1-1f9e20a7b5e1`.
- Privacy-safe BLE advertising begins broadcasting without personal user data.

### Step 5: Discover Each Other
On **Phone A**, tap **"Scan Nearby"** or the radar floating action button.
- The phone initiates Classic Discovery + BLE Scanning.
- **Phone B** appears in the list marked with the **"PARDHAN"** badge.

### Step 6: Pair Devices (If Prompted)
If the phones have not paired before, Android OS will display the standard Bluetooth pairing PIN dialog. Confirm pairing on both phones.

### Step 7: Establish RFCOMM Connection
On **Phone A**, tap **"Connect"** next to **Phone B**.
- Active discovery automatically halts (prevents radio degradation).
- RFCOMM socket connects to Service UUID `7c844111-53b9-4a0b-9ef1-1f9e20a7b5e1`.

### Step 8: Automatic Handshake Exchange
Within milliseconds, the two phones exchange protocol packets:
1. `HANDSHAKE_INIT`: `{"protocol_version":"PARDHAN-v1.0","app_name":"Pardhan Connect","session_id":"...","node_id":"..."}`
2. `HANDSHAKE_ACK`: `{"accepted":true,...}`
Both phones transition to the **CONNECTED** state, displaying the peer device name and session tokens.

### Step 9: Verify Bidirectional Data (PING / PONG)
On either phone, tap **"Send PING"**:
- Phone sends `PING` packet with sequence number and microsecond timestamp.
- Remote peer receives and instantly echoes a `PONG` packet.
- Live round-trip latency (typically 12ms - 45ms over RFCOMM) displays on screen.

### Step 10: Disconnect, Reconnect & Failure Scenarios
1. Tap **"Disconnect"**: verify socket closes cleanly, resources are freed, and the RFCOMM server resumes listening.
2. Turn Bluetooth OFF in Android Quick Settings: observe the app promptly switches to the **"Bluetooth Disabled"** state banner.
3. Turn Bluetooth back ON: verify the app recovers and resumes ready state.

---

## 🔒 Permission & Privacy Specifications

| Permission | Android API Level | Rationale |
|------------|-------------------|-----------|
| `BLUETOOTH_SCAN` | API 31+ (Android 12+) | Discovers nearby Classic and BLE Pardhan devices (`neverForLocation`). |
| `BLUETOOTH_CONNECT` | API 31+ (Android 12+) | Opens RFCOMM sockets and reads device names. |
| `BLUETOOTH_ADVERTISE` | API 31+ (Android 12+) | Broadcasts BLE presence beacon off-grid. |
| `BLUETOOTH` & `BLUETOOTH_ADMIN` | API 26-30 | Legacy adapter control on older Android releases. |
| `ACCESS_FINE_LOCATION` | API 26-30 | Required by Android OS for BLE scanning on API <= 30. |

*Privacy Guarantee:* Advertisements and beacons broadcast ONLY the application service UUID. User identities, hardware identifiers, and private keys are never transmitted in unencrypted beacons.

---

## 📦 Deliverables Summary

- Full Gradle Kotlin DSL configuration (`build.gradle.kts`, `settings.gradle.kts`, `libs.versions.toml`).
- AndroidManifest.xml with version-aware permission boundaries.
- Android UI built with Jetpack Compose & Material 3.
- Kotlin RFCOMM Bluetooth engine with Dispatchers.IO background coroutines.
- Clean Architecture MVVM + StateFlow.
- Mesh Interfaces preparing Phase 2 multi-hop decentralization.
