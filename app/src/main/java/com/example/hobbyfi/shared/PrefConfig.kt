package com.example.hobbyfi.shared

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.example.hobbyfi.R


class PrefConfig(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context
        .getSharedPreferences(
            context.getString(
                R.string.pref_file
            ), Context.MODE_PRIVATE
        )

    fun writeToken(token: String) {
        val editor = sharedPreferences.edit()
        editor.putString(context.getString(R.string.pref_token), token).apply()
    }

    fun readToken(): String? {
        return sharedPreferences.getString(context.getString(R.string.pref_token), "invalid")
    }

    fun resetToken() {
        val editor = sharedPreferences.edit()
        editor.remove(context.getString(R.string.pref_token)).apply()
    }

    fun writeRefreshToken(refreshToken: String) {
        val editor = sharedPreferences.edit()
        editor.putString(context.getString(R.string.pref_refresh_token), refreshToken).apply()
    }

    fun readRefreshToken(): String? {
        return sharedPreferences.getString(
            context.getString(R.string.pref_refresh_token),
            "invalid"
        )
    }

    fun resetRefreshToken() {
        val editor = sharedPreferences.edit()
        editor.remove(context.getString(R.string.pref_refresh_token)).apply()
    }


    fun writeLastPrefFetchTimeNow(prefId: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt(
            context.getString(prefId),
            System.currentTimeMillis()
                .toInt() / 1000
        ).apply()
    }

    fun resetLastPrefFetchTime(prefId: Int) {
        val editor = sharedPreferences.edit()
        editor.remove(context.getString(prefId)).apply()
    }

    fun readLastPrefFetchTime(prefId: Int) : Long {
        return sharedPreferences.getInt(
            context.getString(prefId),
            (System.currentTimeMillis() / 1000).toInt()
        ).toLong() // return different default value for Glide ObjectKey cache (always fetch)
    }

    fun displayToast(message: String?) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}