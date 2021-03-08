package com.example.hobbyfi.ui.base

import android.util.Log
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.shared.EmitterListenerFactory
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

interface ServerSocketAccessor {
    val serverSocket: Socket?
        get() = try {
            IO.socket(BuildConfig.SOCKET_BASE_URL)
        } catch(e: URISyntaxException) {
            onConnectedServerSocketFail()
            null
        }

    val emitterListenerFactory: EmitterListenerFactory

    fun connectServerSocket() {
        serverSocket?.connect()
        connectServerSocketListeners()
    }

    fun disconnectServerSocket() {
        Log.i("ServerSocketAccessor", "Disconnecting server socket")
        serverSocket?.disconnect()
        disconnectServerSocketListeners()
    }

    fun onConnectedServerSocketFail()

    fun connectServerSocketListeners()

    fun disconnectServerSocketListeners()
}