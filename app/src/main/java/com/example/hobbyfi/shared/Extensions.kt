package com.example.hobbyfi.shared

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.hobbyfi.api.HobbyfiAPI
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken

inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)

inline fun <reified T> Gson.fromJson(json: JsonElement) = fromJson<T>(json, object: TypeToken<T>() {}.type)

fun ConnectivityManager.isConnected(): Boolean {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        with(getNetworkCapabilities(
            activeNetwork
        )) {
            return this != null &&
                    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    } else {
        return activeNetworkInfo?.isConnected == true // null safety requires explicit check
    }
}