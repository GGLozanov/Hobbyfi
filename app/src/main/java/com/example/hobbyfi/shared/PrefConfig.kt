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
    // defined separately in terms of semantics and clarity

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
            null
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

    fun writeRequestingLocationUpdates(requesting: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(
            context.getString(R.string.pref_requesting_location_updates),
            requesting
        ).apply()
    }

    fun resetRequestingLocationUpdates() {
        val editor = sharedPreferences.edit()
        editor.remove(context.getString(R.string.pref_requesting_location_updates)).apply()
    }

    fun readRequestingLocationUpdates(): Boolean {
        return sharedPreferences.getBoolean(
            context.getString(R.string.pref_requesting_location_updates),
            false
        )
    }

    fun writeRequestLocationServiceRunning(running: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(
            context.getString(R.string.pref_request_location_service_running),
            running
        ).apply()
    }

    fun readRequestLocationServiceRunning(): Boolean {
        return sharedPreferences.getBoolean(
            context.getString(R.string.pref_request_location_service_running),
            true
        )
    }

    fun readRestartedFromChatroomTaskRoot(): Boolean {
        return sharedPreferences.getBoolean(
            context.getString(R.string.pref_restarted_from_chatroom_task_root),
            false
        )
    }

    fun writeRestartedFromChatroomTaskRoot(res: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(
            context.getString(R.string.pref_restarted_from_chatroom_task_root),
            res
        ).apply()
    }

    fun registerPrefsListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterPrefsListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun readOnboardingValid(): Boolean {
        return sharedPreferences.getBoolean(
            context.getString(R.string.pref_first_app_boot),
            true
        )
    }

    fun writeOnboardingValid(valid: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(
            context.getString(R.string.pref_first_app_boot),
            valid
        ).apply()
    }

    fun readReachedBottomMessagesAfterSearch(): Boolean {
        return sharedPreferences.getBoolean(
            context.getString(R.string.pref_reached_bottom_messages_after_search),
            true
        )
    }

    fun writeReachedBottomMessagesAfterSearch(reached: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(
            context.getString(R.string.pref_reached_bottom_messages_after_search),
            reached
        ).apply()
    }

    fun readCurrentDeviceTokenUploaded(): Boolean {
        return sharedPreferences.getBoolean(
            context.getString(R.string.pref_current_token_uploaded),
            false
        )
    }

    fun writeCurrentDeviceTokenUploaded(uploaded: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(
            context.getString(R.string.pref_current_token_uploaded),
            uploaded
        ).apply()
    }

    fun getAuthUserIdFromToken(): Long =
        if(Constants.isFacebookUserAuthd()) Profile.getCurrentProfile().id.toLong() else
            TokenUtils.getTokenUserIdFromPayload(readToken())

    fun isUserAuthenticated(): Boolean =
        if(Constants.isFacebookUserAuthd()) {
            true
        } else {
            try {
                TokenUtils.getTokenUserIdFromStoredTokens(this).compareTo(0) != 0
            } catch(ex: Exception) {
                Log.w("PrefConfig", "isUserAuthenticated() -> normal token check exception")
                false
            }
        }

    fun getAuthUserToken(): String? =
        if(Constants.isFacebookUserAuthd()) AccessToken.getCurrentAccessToken().token else readToken()
}