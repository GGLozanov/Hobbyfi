package com.example.hobbyfi.shared

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import android.widget.GridView
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

// credit to Utsav Branwal from SO https://stackoverflow.com/questions/6005245/how-to-have-a-gridview-that-adapts-its-height-when-items-are-added
fun GridView.setHeightBasedOnChildren(noOfColumns: Int) {
    val gridViewAdapter = adapter ?: return // adapter is not set yet
    var totalHeight: Int //total height to set on grid view
    val items = gridViewAdapter.count //no. of items in the grid
    val rows: Int //no. of rows in grid
    val listItem: View = gridViewAdapter.getView(0, null, this)
    listItem.measure(0, 0)
    totalHeight = listItem.getMeasuredHeight()
    val x: Float
    if (items > noOfColumns) {
        x = (items / noOfColumns).toFloat()

        //Check if exact no. of rows of rows are available, if not adding 1 extra row
        rows = if (items % noOfColumns != 0) {
            (x + 1).toInt()
        } else {
            x.toInt()
        }
        totalHeight *= rows

        //Adding any vertical space set on grid view
        totalHeight += verticalSpacing * rows
    }

    //Setting height on grid view
    val params = layoutParams
    params.height = totalHeight
    layoutParams = params
}