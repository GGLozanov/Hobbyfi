    package com.example.hobbyfi.ui.base

import android.net.ConnectivityManager
import android.util.Log
import androidx.annotation.MainThread
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.shared.EmitterListenerFactory
import com.example.hobbyfi.shared.isConnected
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.client.SocketOptionBuilder
import java.net.URISyntaxException

interface ServerSocketAccessor: ConnectivityAccessor {
    val serverSocket: Socket?
    val emitterListenerFactory: EmitterListenerFactory

    fun connectServerSocket() {
        connectServerSocketListeners()

        serverSocket?.on(Socket.EVENT_CONNECT_ERROR) {
            if(!connectivityManager.isConnected()) {
                onConnectedServerSocketFail()
            }
        }

        serverSocket?.connect()
    }

    fun disconnectServerSocket() {
        Log.i("ServerSocketAccessor", "Disconnecting server socket")
        serverSocket?.disconnect()
        disconnectServerSocketListeners()
    }

    @MainThread
    fun onConnectedServerSocketFail()

    fun connectServerSocketListeners()

    fun disconnectServerSocketListeners()
}