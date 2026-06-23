package com.example.player

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WebBluetoothManager(context: Context) {
    data class WebBluetoothStatus(
        val isGattConnected: Boolean = true,
        val deviceName: String = "Sony WH-1000XM4 (LE Audio)",
        val serviceUuid: String = "0000180d-0000-1000-8000-00805f9b34fb",
        val audioCodec: String = "LDAC / 990kbps",
        val estimatedLatencyMs: Int = 45,
        val streamQuality: String = "Ultra HD Audio (Direct-In)"
    )

    private val _webBluetoothStatus = MutableStateFlow(WebBluetoothStatus())
    val webBluetoothStatus: StateFlow<WebBluetoothStatus> = _webBluetoothStatus.asStateFlow()
}
