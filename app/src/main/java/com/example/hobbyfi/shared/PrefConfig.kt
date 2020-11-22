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

    fun writeLoginStatus(status: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(context.getString(R.string.pref_login_status), status).apply()
    }

    fun readLoginStatus(): Boolean {
        return sharedPreferences.getBoolean(context.getString(R.string.pref_login_status), false)
    }

    fun writeToken(token: String) {
        val editor = sharedPreferences.edit()
        editor.putString(context.getString(R.string.pref_token), token).apply()
    }

    fun readToken(): String? {
        return sharedPreferences.getString(context.getString(R.string.pref_token), "invalid")
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

    fun writeLastChatroomFetchTimeNow() {
        val editor = sharedPreferences.edit()
        editor.putInt(
            context.getString(R.string.pref_last_chatroom_fetch_time), System.currentTimeMillis()
                .toInt() / 1000
        ).apply()
    }

    fun resetLastChatroomFetchTime() {
        val editor = sharedPreferences.edit()
        editor.remove(context.getString(R.string.pref_last_chatroom_fetch_time)).apply()
    }

    fun readLastChatroomFetchTime() : Long {
        return sharedPreferences.getInt(
            context.getString(R.string.pref_last_chatroom_fetch_time),
            System.currentTimeMillis().toInt()
        ).toLong() // return different default value for Glide ObjectKey cache (always fetch)
    }


//    // last chatroom users fetch time is used for checking and responding to possible notifications of
//    fun writeLastChatroomUsersFetchTimeNow() {
//        val editor = sharedPreferences.edit()
//        editor.putInt(
//            context.getString(R.string.pref_last_chatroom_users_fetch_time), System.currentTimeMillis()
//                .toInt() / 1000
//        ).apply()
//    }
//
//    fun resetLastChatroomUsersFetchTime() {
//        val editor = sharedPreferences.edit()
//        editor.remove(context.getString(R.string.pref_last_chatroom_users_fetch_time)).apply()
//    }
//
//    fun readLastChatroomUsersFetchTime(): Long {
//        return sharedPreferences.getInt(
//            context.getString(R.string.pref_last_chatroom_users_fetch_time),
//            System.currentTimeMillis().toInt()
//        ).toLong() // return different default value for ObjectKey cache (always fetch)
//    }


    fun writeLastUserFetchTimeNow() {
        val editor = sharedPreferences.edit()
        editor.putInt(
            context.getString(R.string.pref_last_user_fetch_time), System.currentTimeMillis()
                .toInt() / 1000
        ).apply()
    }

    fun resetLastUserFetchTime() {
        val editor = sharedPreferences.edit()
        editor.remove(context.getString(R.string.pref_last_user_fetch_time)).apply()
    }

    fun readLastUserFetchTime() : Long {
        return sharedPreferences.getInt(
            context.getString(R.string.pref_last_user_fetch_time),
            System.currentTimeMillis().toInt()
        ).toLong() // return different default value for Glide ObjectKey cache (always fetch)
    }

    fun displayToast(message: String?) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}