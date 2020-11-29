package com.example.hobbyfi.shared

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.util.Predicate
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Tag
import com.example.spendidly.utils.PredicateTextWatcher
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken

inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)

inline fun <reified T> Gson.fromJson(json: JsonElement?) = fromJson<T>(json, object: TypeToken<T>() {}.type)

fun TextInputEditText.addTextChangedListener(errorText: String, predicate: Predicate<String>) =
    this.addTextChangedListener(
        PredicateTextWatcher(
            this,
            errorText,
            predicate
        )
    )

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

fun MutableList<Tag>.appendNewSelectedTagsToTags(selectedTags: List<Tag>) {
    selectedTags.forEach {
        if(!this.contains(it)) {
            this.add(it)
        }
    }
}

fun List<Tag>.getNewSelectedTagsWithTags(selectedTags: List<Tag>): List<Tag> {
    val newTags = this.toMutableList()
    selectedTags.forEach {
        if(!newTags.contains(it)) {
            newTags.add(it)
        }
    }
    return newTags
}