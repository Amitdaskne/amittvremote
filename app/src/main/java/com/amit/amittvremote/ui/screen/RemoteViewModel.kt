package com.amit.amittvremote.ui.screen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amit.amittvremote.protocol.TvRemoteProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Pairing : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

class RemoteViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private var commandConnection: TvRemoteProtocol.CommandConnection? = null
    private var pairingConnection: TvRemoteProtocol.PairingConnection? = null

    fun startPairing(ip: String) {
        viewModelScope.launch {
            _state.value = ConnectionState.Pairing
            TvRemoteProtocol.connectForPairing(ip).onSuccess {
                pairingConnection = it
                _state.value = ConnectionState.Pairing // Still pairing, waiting for code
            }.onFailure {
                _state.value = ConnectionState.Error("Pairing connection failed: ${it.message}")
            }
        }
    }

    fun submitPairingCode(code: String) {
        val conn = pairingConnection ?: return
        viewModelScope.launch {
            val success = TvRemoteProtocol.sendPairingCode(conn, code)
            if (success) {
                // After pairing, connect for commands
                connectForCommands(conn.socket.inetAddress.hostAddress ?: "")
            } else {
                _state.value = ConnectionState.Error("Invalid pairing code")
            }
        }
    }

    private fun connectForCommands(ip: String) {
        viewModelScope.launch {
            TvRemoteProtocol.connectForCommands(ip).onSuccess {
                commandConnection = it
                _state.value = ConnectionState.Connected
            }.onFailure {
                _state.value = ConnectionState.Error("Command connection failed: ${it.message}")
            }
        }
    }

    fun sendKey(keyCode: Int) {
        val conn = commandConnection ?: return
        viewModelScope.launch {
            TvRemoteProtocol.sendKey(conn, keyCode)
        }
    }
}
