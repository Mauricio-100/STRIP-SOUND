package com.example.player

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioDeviceDetector(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    data class ActiveDevice(
        val name: String,
        val type: DeviceType,
        val isBluetooth: Boolean
    )

    enum class DeviceType {
        BLUETOOTH_A2DP,
        BLUETOOTH_SCO,
        BLE_HEADSET,
        BLE_SPEAKER,
        WIRED_HEADPHONE,
        WIRED_HEADSET,
        BUILTIN_SPEAKER,
        BUILTIN_EARPIECE,
        UNKNOWN
    }

    private val _activeDevice = MutableStateFlow<ActiveDevice?>(null)
    val activeDevice: StateFlow<ActiveDevice?> = _activeDevice.asStateFlow()

    private val _connectedBluetoothDevices = MutableStateFlow<List<String>>(emptyList())
    val connectedBluetoothDevices: StateFlow<List<String>> = _connectedBluetoothDevices.asStateFlow()

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            updateActiveDevice()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            updateActiveDevice()
        }
    }

    init {
        try {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
            updateActiveDevice()
        } catch (e: Exception) {
            Log.e("AudioDeviceDetector", "Error registering device callback", e)
        }
    }

    fun updateActiveDevice() {
        try {
            val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            var active: ActiveDevice? = null
            val bluetoothNames = mutableListOf<String>()

            for (device in outputs) {
                val deviceTypeInt = device.type
                var deviceName = device.productName?.toString() ?: "Device"
                if (deviceName.isBlank() || deviceName == "Device") {
                    deviceName = when {
                        deviceTypeInt == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Écouteurs Bluetooth"
                        deviceTypeInt == AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Casque Bluetooth"
                        Build.VERSION.SDK_INT >= 31 && deviceTypeInt == 26 -> "Audio BLE" // TYPE_BLE_HEADSET
                        Build.VERSION.SDK_INT >= 31 && deviceTypeInt == 27 -> "Enceinte BLE" // TYPE_BLE_SPEAKER
                        deviceTypeInt == AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Casque filaire"
                        deviceTypeInt == AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Écouteurs filaires"
                        deviceTypeInt == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Haut-parleur externe"
                        deviceTypeInt == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Haut-parleur interne"
                        else -> "Sortie Audio"
                    }
                }

                val type = when {
                    deviceTypeInt == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> DeviceType.BLUETOOTH_A2DP
                    deviceTypeInt == AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> DeviceType.BLUETOOTH_SCO
                    Build.VERSION.SDK_INT >= 31 && deviceTypeInt == 26 -> DeviceType.BLE_HEADSET // TYPE_BLE_HEADSET
                    Build.VERSION.SDK_INT >= 31 && deviceTypeInt == 27 -> DeviceType.BLE_SPEAKER // TYPE_BLE_SPEAKER
                    deviceTypeInt == AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> DeviceType.WIRED_HEADPHONE
                    deviceTypeInt == AudioDeviceInfo.TYPE_WIRED_HEADSET -> DeviceType.WIRED_HEADSET
                    deviceTypeInt == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> DeviceType.BUILTIN_SPEAKER
                    deviceTypeInt == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> DeviceType.BUILTIN_EARPIECE
                    else -> DeviceType.UNKNOWN
                }

                val isBt = type == DeviceType.BLUETOOTH_A2DP ||
                        type == DeviceType.BLUETOOTH_SCO ||
                        type == DeviceType.BLE_HEADSET ||
                        type == DeviceType.BLE_SPEAKER

                if (isBt) {
                    bluetoothNames.add(deviceName)
                }

                // Bluetooth takes precedence, then wired, then built-in
                if (active == null) {
                    active = ActiveDevice(deviceName, type, isBt)
                } else {
                    if (isBt && !active.isBluetooth) {
                        active = ActiveDevice(deviceName, type, isBt)
                    } else if (type == DeviceType.WIRED_HEADPHONE && active.type == DeviceType.BUILTIN_SPEAKER) {
                        active = ActiveDevice(deviceName, type, isBt)
                    }
                }
            }

            _connectedBluetoothDevices.value = bluetoothNames
            _activeDevice.value = active ?: ActiveDevice("Haut-parleurs", DeviceType.BUILTIN_SPEAKER, false)
        } catch (e: Exception) {
            Log.e("AudioDeviceDetector", "Error updating active devices", e)
        }
    }

    fun release() {
        try {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        } catch (e: Exception) {
            Log.e("AudioDeviceDetector", "Error unregistering callback", e)
        }
    }
}
