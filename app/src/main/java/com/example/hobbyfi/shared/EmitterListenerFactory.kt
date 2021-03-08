package com.example.hobbyfi.shared

import android.app.Activity
import android.util.Log
import com.example.hobbyfi.models.data.Message
import com.example.hobbyfi.models.data.Model
import io.socket.emitter.Emitter
import org.json.JSONObject

class EmitterListenerFactory(private val activity: Activity) {

    fun <T: Model> createEmitterListenerForCreate(
        modelFromMap: (map: Map<String, String>) -> T,
        onModelDeserialised: (T) -> Unit, errorFallback: (Exception) -> Unit = { throw it }): Emitter.Listener {
        return Emitter.Listener {
            activity.runOnUiThread {
                try {
                    Log.i("EmitterListenerFactory", "data received for createEmitterListenerForCreate: ${it}")
                    onModelDeserialised(modelFromMap((it[0] as JSONObject).toPlainStringMap()))
                } catch(ex: Exception) {
                    errorFallback(ex)
                }
            }
        }
    }

    fun createEmitterListenerForEdit() {
        TODO("Need to implement")
    }

    // TODO: Handle event_delete_batch with this too
    fun createEmitterListenerForDelete() {
        TODO("Need to implement")
    }
}