package com.example.hobbyfi.shared

import android.app.Activity
import android.util.Log
import com.example.hobbyfi.models.data.Model
import io.socket.emitter.Emitter
import org.json.JSONObject

class EmitterListenerFactory(private val activity: Activity) {

    fun <T: Model> createEmitterListenerForCreate(
        modelFromMap: (map: Map<String, String>) -> T,
        onModelDeserialised: (T) -> Unit, errorFallback: (Exception) -> Unit = { throw it }
    ): Emitter.Listener = Emitter.Listener {
            activity.runOnUiThread {
                try {
                    Log.i("EmitterListenerFactory", "data received for createEmitterListenerForCreate: ${it}")
                    onModelDeserialised(modelFromMap((it[0] as JSONObject).toPlainStringMap()))
                } catch(ex: Exception) {
                    errorFallback(ex)
                }
            }
        }

    fun createEmitterListenerForEdit(
        onEditFieldMapReceived: (Map<String, String?>) -> Unit, errorFallback: (Exception) -> Unit = { throw it }
    ): Emitter.Listener = Emitter.Listener {
        activity.runOnUiThread {
            try {
                Log.i("EmitterListenerFactory", "data received for createEmitterListenerForCreate: ${it}")
                onEditFieldMapReceived((it[0] as JSONObject).toPlainStringMap())
            } catch(ex: Exception) {
                errorFallback(ex)
            }
        }
    }

    fun createEmitterListenerForDelete(
        onIdFieldReceived: (id: Long) -> Unit, errorFallback: (Exception) -> Unit = { throw it },
        idField: String = Constants.ID
    ): Emitter.Listener = Emitter.Listener {
        activity.runOnUiThread {
            try {
                Log.i("EmitterListenerFactory", "data received for createEmitterListenerForCreate: ${it}")
                onIdFieldReceived(
                    (it[0] as JSONObject).toPlainStringMap()[idField]?.toLong()
                        ?: throw IllegalArgumentException(
                            "Id field in createEmitterListenerForDelete not found! Sent data is incorrect!")
                )
            } catch(ex: Exception) {
                errorFallback(ex)
            }
        }
    }

    fun createEmitterListenerForDeleteArray(
        onIdFieldArrayReceived: (id: List<Long>) -> Unit, errorFallback: (Exception) -> Unit = { throw it },
        idField: String
    ): Emitter.Listener = Emitter.Listener {
        activity.runOnUiThread {
            try {
                Log.i("EmitterListenerFactory", "data received for createEmitterListenerForCreate: ${it}")
                onIdFieldArrayReceived(
                    Constants.jsonConverter.fromJson(
                        (it[0] as JSONObject).toPlainStringMap()[idField]
                    ) ?: throw IllegalArgumentException(
                            "Id field in createEmitterListenerForDelete not found! Sent data is incorrect!")
                )
            } catch(ex: Exception) {
                errorFallback(ex)
            }
        }
    }
}