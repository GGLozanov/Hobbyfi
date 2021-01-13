package com.example.hobbyfi.shared

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.example.hobbyfi.R
import com.example.hobbyfi.repositories.Repository
import com.example.hobbyfi.utils.TokenUtils
import com.facebook.AccessToken
import com.facebook.Profile


class PrefConfig(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context
        .getSharedPreferences(
            context.getString(
                R.string.pref_file
            ), Context.MODE_PRIVATE
        )

    // methods below can be treated as code dup but I feel it better to have these methods
    // defined separately in terms of semantics
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

    fun writeDeviceToken(token: String) {
        val editor = sharedPreferences.edit()
        editor.putString(context.getString(R.string.pref_device_token), token).apply()
    }

    fun readDeviceToken(): String? {
        return sharedPreferences.getString(
            context.getString(R.string.pref_device_token),
            "invalid"
        )
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

    fun writeLastEnteredChatroomId(chatroomId: Long) {
        val editor = sharedPreferences.edit()
        editor.putInt(
            context.getString(R.string.pref_last_entered_chatroom_id),
            chatroomId.toInt()
        ).apply()
    }

    fun resetLastEnteredChatroomId() {
        val editor = sharedPreferences.edit()
        editor.remove(context.getString(R.string.pref_last_entered_chatroom_id)).apply()
    }

    fun readLastEnteredChatroomId(): Long {
        return sharedPreferences.getInt(
            context.getString(R.string.pref_last_entered_chatroom_id),
            0
        ).toLong()
    }

    fun writeChatroomJoinRememberNavigate(remember: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt(
            context.getString(R.string.pref_chatroom_join_remember_navigate),
            remember
        ).apply()
    }

    fun resetChatroomJoinRememberNavigate() {
        val editor = sharedPreferences.edit()
        editor.remove(context.getString(R.string.pref_chatroom_join_remember_navigate)).apply()
    }

    fun readChatroomJoinRememberNavigate(): Int {
        return sharedPreferences.getInt(
            context.getString(R.string.pref_chatroom_join_remember_navigate),
            Constants.NoRememberDualChoice.NO_REMEMBER.ordinal
        )
    }

    fun getAuthUserIdFromToken(): Long =
        if(Constants.isFacebookUserAuthd()) Profile.getCurrentProfile().id.toLong() else
            TokenUtils.getTokenUserIdFromPayload(readToken())

    fun getAuthUserToken(): String? =
        if(Constants.isFacebookUserAuthd()) AccessToken.getCurrentAccessToken().token else readToken()
}