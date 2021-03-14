    package com.example.hobbyfi.ui.base

import android.util.Log
import androidx.annotation.MainThread
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.shared.EmitterListenerFactory
import com.example.hobbyfi.shared.isConnected
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.client.SocketOptionBuilder
import io.socket.emitter.Emitter
import io.socket.engineio.client.Transport
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

        serverSocket?.io()?.on(Manager.EVENT_TRANSPORT) {
            val transport: Transport = it[0] as Transport
            transport.on(Transport.EVENT_ERROR) { args ->
                val e = args[0] as Exception
                Log.e("ServerSocketAccessor", "Transport error $e")
                e.printStackTrace()
                e.cause!!.printStackTrace()
            }
        }

        Log.i("ServerSocketAccessor", "Connecting server socket")

        serverSocket?.connect()
    }

    fun disconnectServerSocket() {
        Log.i("ServerSocketAccessor", "Disconnecting server socket")
        serverSocket?.off()
        serverSocket?.disconnect()
        disconnectServerSocketListeners()
    }

    fun initSocket(): Socket? =
        try {
            val socket = IO.socket(
                BuildConfig.SOCKET_BASE_URL,
                SocketOptionBuilder.builder().setForceNew(true).build())
            Log.i("ServerSocketAccessor", "Accessed socket with successful connection")
            socket
        } catch(e: URISyntaxException) {
            onConnectedServerSocketFail()
            null
        }

    @MainThread
    fun onConnectedServerSocketFail()

    @MainThread
    fun connectServerSocketListeners()

    @MainThread
    fun disconnectServerSocketListeners() {

    }
}