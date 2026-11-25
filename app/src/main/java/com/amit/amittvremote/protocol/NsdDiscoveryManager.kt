package com.amit.amittvremote.protocol

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class TvDevice(val name: String, val hostAddress: String, val port: Int)

class NsdDiscoveryManager(private val context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    fun discoverTvs(): Flow<TvDevice> = callbackFlow {
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType.contains("_androidtvremote2._tcp")) {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                            trySend(
                                TvDevice(
                                    name = resolvedInfo.serviceName,
                                    hostAddress = resolvedInfo.host.hostAddress ?: "",
                                    port = resolvedInfo.port
                                )
                            )
                        }
                    })
                }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { close() }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) { close() }
        }

        nsdManager.discoverServices("_androidtvremote2._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)

        awaitClose {
            nsdManager.stopServiceDiscovery(discoveryListener)
        }
    }
}
