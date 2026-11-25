package com.amit.amittvremote.ui.screen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amit.amittvremote.protocol.NsdDiscoveryManager
import com.amit.amittvremote.protocol.TvCommandClient
import com.amit.amittvremote.protocol.TvDevice
import com.amit.amittvremote.protocol.TvPairingClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ConnectionState {
    data object Idle : ConnectionState()
    data object Discovering : ConnectionState()
    data class DeviceFound(val devices: List<TvDevice>) : ConnectionState()
    data object Pairing : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

class RemoteViewModel(app: Application) : AndroidViewModel(app) {
    private val nsdManager = NsdDiscoveryManager(app)
    private var pairingClient: TvPairingClient? = null
    private var commandClient: TvCommandClient? = null
    private var discoveryJob: Job? = null

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    fun discoverDevices() {
        discoveryJob?.cancel()
        _state.value = ConnectionState.Discovering
        discoveryJob = viewModelScope.launch {
            nsdManager.discoverTvs().collect { device ->
                val current = (_state.value as? ConnectionState.DeviceFound)?.devices ?: emptyList()
                if (current.none { it.hostAddress == device.hostAddress }) {
                    _state.value = ConnectionState.DeviceFound(current + device)
                }
            }
        }
    }

    fun startPairing(ip: String) {
        viewModelScope.launch {
            _state.value = ConnectionState.Pairing
            pairingClient = TvPairingClient(ip)
            val connected = pairingClient?.connectAndRequestPairing() ?: false
            if (!connected) {
                _state.value = ConnectionState.Error("Failed to send pairing request")
            }
            // Now UI waits for user to enter code
        }
    }

    fun submitPairingCode(code: String) {
        viewModelScope.launch {
            val success = pairingClient?.submitPairingCode(code) ?: false
            if (success) {
                // Connect command client
                val ip = pairingClient?.let { /* extract ip */ "" } ?: ""
                commandClient = TvCommandClient(ip)
                if (commandClient?.connect() == true) {
                    _state.value = ConnectionState.Connected
                } else {
                    _state.value = ConnectionState.Error("Paired but command connection failed")
                }
            } else {
                _state.value = ConnectionState.Error("Invalid pairing code")
            }
        }
    }

    fun sendKey(keyCode: Int) {
        viewModelScope.launch {
            commandClient?.sendKey(keyCode)
        }
    }

    override fun onCleared() {
        discoveryJob?.cancel()
        pairingClient?.disconnect()
        commandClient?.disconnect()
        super.onCleared()
    }
}
