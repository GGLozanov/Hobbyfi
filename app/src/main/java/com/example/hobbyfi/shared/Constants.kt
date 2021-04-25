package com.example.hobbyfi.shared

import android.annotation.SuppressLint
import android.util.Log
import androidx.paging.PagingConfig
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.adapters.tag.TagTypeAdapter
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.repositories.Repository
import com.facebook.AccessToken
import com.facebook.Profile
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder


object Constants {
    const val invalidViewTypeError: String = "Invalid view type for ViewHolder!"
    const val invalidStateError: String = "Invalid state to call this method in!"
    const val incorrectCallToBuildLocationTrackingDialog: String = "Cannot call method buildLocationTrackingDialog in EventMapsActivity without having garnered user location permissions first!"

    const val imagePermissionsRequestCode = 200
    const val imageRequestCode = 777

    const val locationPermissionsRequestCode: Int = 888

    const val eventLocationRequestCode: Int = 999
    const val eventMapsRequestCode: Int = 666
    const val externalStorageWriteCode = 323
    const val RESULT_CHATROOM_DELETE: Int = 423
    const val RESULT_KICKED: Int = 246
    const val RESULT_REAUTH: Int = 111

    // TODO: Put in-memory tags here
    val predefinedTags: List<Tag> = listOf(
        Tag(
            "Skating",
            "#dd9612"
        ),
        Tag(
            "Skiing",
            "#57f429"
        ),
        Tag(
            "Writing",
            "#ed3ae5"
        ),
        Tag(
            "Art",
            "#8f7edf"
        )
    )

    const val chatroomPageSize: Int = 5
    const val messagesPageSize: Int = 20

    fun getDefaultPageConfig(pageSize: Int): PagingConfig { // used in pager init
        return PagingConfig(pageSize = pageSize, enablePlaceholders = false)
    }

    // should be in prefconfig but... eh
    fun isFacebookUserAuthd(): Boolean {
        if(AccessToken.getCurrentAccessToken() != null) {
            if(!AccessToken.getCurrentAccessToken().isExpired && Profile.getCurrentProfile() != null
                    && Profile.getCurrentProfile().id != null) {
                return true
            } // facebook user access true

            Log.i("Constants", "Facebook user sharedprefs token is expired. Reauthenticating.")
            throw Repository.ReauthenticationException() // facebook user auth'd but expired token - throw exception to reauth
        }
        return false // facebook user not auth'd - just return false
    }

    fun cacheTimedOut(prefConfig: PrefConfig, prefId: Int): Boolean {
        return ((System.currentTimeMillis() / 1000) - prefConfig.readLastPrefFetchTime(prefId)) <= Constants.CACHE_TIMEOUT
    }

    const val CACHE_TIMEOUT = 60 * 60 * 2 // 2 hours
        .toLong()

    const val SUCCESS_RESPONSE = "Ok"
    const val INVALID_TOKEN = "invalid"

    // enum would've been more concise here but annotations can't have enums call toString()
    const val ID = "id"
    const val RESPONSE = "response"
    const val EMAIL = "email"
    const val PASSWORD = "password"
    const val NAME = "name"
    const val USERNAME = "username"
    const val DESCRIPTION = "description"
    const val CHATROOM_ID = "chatroom_id"
    const val TOGGLE = "toggle"
    const val MESSAGE_ID = "message_id"
    const val IMAGE = "image"
    const val TAGS = "tags"
    const val OWNER_ID = "owner_id"
    const val LAST_EVENT_ID = "last_event_id"
    const val PAGE = "page"
    const val DATA = "data"
    const val DATA_LIST = "data_list"
    const val MESSAGE = "message"
    const val TYPE = "type"
    const val CREATE_TIME = "create_time"
    const val USER_SENT_ID = "user_sent_id"
    const val CHATROOM_SENT_ID = "chatroom_sent_id"
    const val START_DATE = "start_date"
    const val DATE = "date"
    const val USER_ID = "user_id"
    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"

    const val PHOTO_URL = "photo_url"

    const val AUTH_HEADER = "Authorization"
    const val PREF_ID = "PREF_ID"

    const val tagsKey = "tag"
    const val selectedTagsKey = "selectedTags"

    const val name = "name"
    const val colour = "colour"
    const val isFromFacebook: String = "is_from_facebook"

    const val profileImageWidth = 175
    const val profileImageHeight = 135

    fun userImageBucket(title: String): String {
        return "users/$title.jpg"
    }

    fun eventImageBucket(title: String): String {
        return "events/$title.jpg"
    }

    fun chatroomImageBucket(chatroomId: Long, title: String): String {
        return "chatroom_$chatroomId/$title.jpg"
    }

    fun messageImageBucket(chatroomId: Long, title: String): String {
        return "chatroom_$chatroomId/messages/$title.jpg"
    }

    fun getFirebaseStorageUrlForLocation(location: String, onSuccess: (link: String) -> Unit, onFailure: (() -> Unit)? = null) {
        FirebaseStorage.getInstance().reference.child(location).downloadUrl.addOnSuccessListener {
            onSuccess(it.toString())
        }.addOnFailureListener {
            if(onFailure != null) {
                onFailure()
            } else {
                Log.w("Constants", "Couldn't fetch image URL")
                // TODO: better error handling (? ? ?)
            }
        }
    }

    // dupped from API and whenever that changes, this needs to as well, but...
    // how else? Getting it from the server each time?
    const val CREATE_MESSAGE_TYPE: String = "CREATE_MESSAGE"
    const val EDIT_MESSAGE_TYPE: String = "EDIT_MESSAGE"
    const val DELETE_MESSAGE_TYPE: String = "DELETE_MESSAGE"
    const val JOIN_USER_TYPE: String = "JOIN_USER"
    const val LEAVE_USER_TYPE: String = "LEAVE_USER"
    const val EDIT_USER_TYPE: String = "EDIT_USER"
    const val DELETE_CHATROOM_TYPE: String = "DELETE_CHATROOM"
    const val EDIT_CHATROOM_TYPE: String = "EDIT_CHATROOM"
    const val CREATE_EVENT_TYPE: String = "CREATE_EVENT"
    const val EDIT_EVENT_TYPE: String = "EDIT_EVENT"
    const val DELETE_EVENT_TYPE: String = "DELETE_EVENT"
    const val DELETE_EVENT_BATCH_TYPE: String = "DELETE_EVENT_BATCH"

    const val DELETED_MODEL_USER_SENT_ID: String = "DELETED_MODEL_USER_SENT_ID"

    const val CHATROOM_DELETED: String = "CHATROOM_DELETED" // action for broadcast whenever owner deletes chatroom

    const val MAIN_ACTIVITY_FRAGMENT_SELECTED: String = "MAIN_ACTIVITY_FRAGMENT_SELECTED" // TODO: Use

    const val QUERY: String = "query"
    const val ROOM_IDS: String = "roomIds"

    const val USERS: String = "users"
    const val CHATROOMS: String = "chatrooms"
    const val EVENTS: String = "events"

    // TODO: Move to DI and use it somehow...?!??
    // Process death go brrr
    val jsonConverter: Gson = GsonBuilder()
        .registerTypeAdapter(
            Tag::class.java,
            TagTypeAdapter()
        )
    .create()

    // intent extra keys
    // data = FCM message data payload
    const val DATA_KEYS: String = "data_keys"
    const val DATA_VALUES: String = "data_values"
    const val DELETED_MODEL_ID: String = "deleted_model_id"
    const val PARCELABLE_MODEL: String = "parcelable_model"

    const val USER_TYPING = "user_typing"
    const val USER_CEASE_TYPING = "user_cease_typing"

    const val TOKEN: String = "token"
    const val API: String = "API"

    const val USER = "USER"

    class ImageFetchException(message: String? = null) : Exception(message)

    val imageRegex = Regex(
        "(${Regex.escape("https://storage.googleapis.com/${FirebaseStorage.getInstance().reference.bucket}")}|${Regex.escape(
            "https://firebasestorage.googleapis.com/v0/b/${FirebaseStorage.getInstance().reference.bucket}/o/")})" +
                "[^.]+" + Regex.escape(".jpg") + ".*"
    )

    const val searchMessage: String = "searchMessage"
    const val currentMessages: String = "currentMessages"
    const val messagesPagingData: String = "MESSAGES_PAGING_DATA"

    const val LOCATIONS_COLLECTION: String = "locations"
    const val LOCATION: String = "location"
    const val KEY_LOCATION: String = "KEY_LOCATION"
    const val KEY_CAMERA_POSITION = "KEY_CAMERA_POSITION"

    const val EVENT_LOCATION = "EVENT_LOCATION"
    const val EVENT_TITLE: String = "EVENT_TITLE"
    const val EVENT_DESCRIPTION = "EVENT_DESCRIPTION"

    const val EVENT_ID = "event_id"
    const val EVENT_IDS: String = "event_ids"
    const val CHATROOM_IDS: String = "chatroom_ids"
    const val ALLOWED_PUSH_CHATROOM_IDS: String = "allowed_push_chatroom_ids"
    const val LEAVE_CHATROOM_ID: String = "leave_chatroom_id"

    const val LAST_CONNECTIVITY: String = "LAST_CONNECTIVITY"

    const val EVENT_SELECTION: String = "EVENT_SELECTION"
    const val EVENT: String = "EVENT"
    const val CALENDAR_DAY: String = "CALENDAR_DAY"

    const val UPDATED_LOCATION: String = "UPDATED_LOCATION"
    const val STARTED_UPDATE_LOCATION_FROM_NOTIFICATION: String = "STARTED_UPDATE_LOCATION_FROM_NOTIFICATION"
    const val UPDATED_LOCATION_ACTION: String = "com.example.hobbyfi.UPDATED_LOCATION_ACTION"
    const val FOREGROUND_REACTIVATION_ACTION: String = "com.example.hobbyfi.FOREGROUND_REACTIVATION_ACTION";
    const val USER_GEO_POINT: String = "USER_GEO_POINT"
    const val deepLinkCall: String = "CALLED_FROM_DEEPLINK"
    const val DEEP_LINK_YEET: String = "DEEP_LINK_YEET"
    const val DEEP_LINK_EXTRAS: String = "DEEP_LINK_EXTRAS"

    const val JOIN_CHATROOM: String = "join_chatroom"
    const val ENTER_MAIN: String = "enter_main"

    @SuppressLint("SimpleDateFormat")
    val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    enum class NoRememberDualChoice {
        REMEMBER_YES, REMEMBER_NO, NO_REMEMBER
    }
}