package com.example.player

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioDeviceDetector(context: Context) {
    enum class DeviceType {
        WIRED_HEADPHONE,
        WIRED_HEADSET,
        BLUETOOTH,
        SPEAKER,
        OTHER
    }

    data class ActiveDevice(
        val type: DeviceType,
        val name: String,
        val isBluetooth: Boolean
    )

    private val _activeDevice = MutableStateFlow(ActiveDevice(DeviceType.SPEAKER, "Haut-parleur intégré", false))
    val activeDevice: StateFlow<ActiveDevice> = _activeDevice.asStateFlow()
}
