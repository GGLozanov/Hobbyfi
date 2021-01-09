package com.example.hobbyfi.shared

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.util.Predicate
import androidx.databinding.BindingAdapter
import androidx.paging.PagingConfig
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.tag.TagTypeAdapter
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.repositories.Repository
import com.facebook.AccessToken
import com.facebook.Profile
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.GsonBuilder


object Constants {
    const val descriptionInputError: String = "Enter a shorter description!"
    const val usernameInputError: String = "Enter a non-empty/shorter username!"
    const val nameInputError: String = "Enter a non-empty/shorter name!"
    const val passwordInputError: String = "Invalid password or doesn't match!"
    const val confirmPasswordInputError: String = "Enter the same password!"
    const val emailInputError: String = "Enter a non-empty valid e-mail address!"
    const val tagNameInputError: String = "Enter a non-empty or shorter tag name!"
    const val messageInputError: String = "Enter a valid, non-empty message!"

    const val invalidEventInfoError: String = "Please enter information date and set its location!"
    const val reauthError: String = "Logging out! Your session may have expired!"
    const val resourceExistsError: String = "This user/thing already exists! Try a different name!"
    const val noConnectionError: String = "Couldn't perform operation! Please check your connection!"
    const val invalidCredentialsError: String = "Invalid credentials!"
    const val invalidBroadcastAction: String = "Invalid action given for registered BroadcastReceiver types!"
    const val invalidTokenError: String = "Invalid access! Please login again!"
    const val unauthorisedAccessError: String = "Unauthorised access!"
    const val expiredTokenError: String = "Your session may have expired and you need to (re)authenticate!"
    const val missingDataError: String = "Missing/invalid data entered!"
    const val cacheDeletionError: String = "Couldn't clear old (cached) data!"
    const val serverConnectionError: String = "Failed to connect to server! Something might have gone wrong on our end!"
    const val internalServerError: String = "Couldn't perform operation! Something might have gone wrong on our end!"
    const val resourceNotFoundError: String = "Requested resource not found!"
    const val fcmTopicError: String = "Couldn't perform operation! Please check your connection or consult with Google, as this error is not ours!"
    const val invalidViewType: String = "Invalid view type for ViewHolder!"
    const val firestoreDeletionError: String = "Couldn't delete Firestore records needed to have been deleted!"
    fun unknownError(message: String?) = "Unknown error! Please check your connection or contact a developer! ${message}"

    const val imagePermissionsRequestCode = 200
    const val imageRequestCode = 777

    const val locationRequestCode: Int = 220
    const val locationPermissionsRequestCode: Int = 888

    const val eventLocationRequestCode: Int = 999

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

    // TODO: Put in in-memory db annotated by room with @Database
    val emailPredicate = Predicate<String> {
        return@Predicate it.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(it).matches()
    }

    fun newEmailPredicate(originalEmail: String?) = Predicate<String> {
        return@Predicate it.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(it).matches() ||
                originalEmail == it
    }

    fun passwordPredicate(confirmPasswordField: EditText? = null) = Predicate<String> {
        return@Predicate it.isEmpty() || it.length <= 4 || it.length >= 15 ||
                if(confirmPasswordField == null ||
                    confirmPasswordField.text.toString().isEmpty()) false else it != confirmPasswordField.text.toString()
    }

    fun confirmPasswordPredicate(passwordField: EditText) = Predicate<String> {
        return@Predicate it.isEmpty() || it != passwordField.text.toString()
    }

    val namePredicate = Predicate<String> {
        return@Predicate it.isEmpty() || it.length >= 25
    }

    val descriptionPredicate = Predicate<String> {
        return@Predicate it.length >= 30
    }
    
    val messagePredicate = Predicate<String> {
        return@Predicate it.isEmpty() || it.length >= 200
    }

    const val chatroomPageSize: Int = 5
    const val messagesPageSize: Int = 20

    fun getDefaultPageConfig(pageSize: Int): PagingConfig { // used in pager init
        return PagingConfig(pageSize = pageSize, enablePlaceholders = false)
    }

    // should be in prefconfig but... eh
    fun isFacebookUserAuthd(): Boolean {
        if(AccessToken.getCurrentAccessToken() != null) {
            if(!AccessToken.getCurrentAccessToken().isExpired && Profile.getCurrentProfile().id != null) {
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
    const val FAILED_RESPONSE = "failed"
    const val IMAGE_UPLOAD_SUCCESS_RESPONSE = "Image Uploaded"
    const val IMAGE_UPLOAD_FAILED_RESPONSE = "Image Upload Failed"
    const val FACEBOOK_EMAIL_FAILED_EXCEPTION = "Error with fetching your Facebook email! Stopping login!"
    const val FACEBOOK_TAGS_FAILED_EXCEPTION = "Error with fetching your Facebook tags! Continuing without them!"
    const val EXISTS_RESPONSE = "exists"
    const val INVALID_TOKEN = "invalid"
    const val REAUTH_FLAG = "Reauth"
    const val FAILED_FLAG = "Failed: "

    // enum would've been more concise here but annotations can't have enums call toString()
    const val ID = "id"
    const val RESPONSE = "response"
    const val EMAIL = "email"
    const val PASSWORD = "password"
    const val NAME = "name"
    const val USERNAME = "username"
    const val DESCRIPTION = "description"
    const val CHATROOM_ID = "chatroom_id"
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
    const val LATITUDE = "latitude"
    const val LONGITUDE = "longitude"

    const val PHOTO_URL = "photo_url"

    const val AUTH_HEADER = "Authorization"

    const val tagsKey = "tag"
    const val selectedTagsKey = "selectedTags"

    const val name = "name"
    const val colour = "colour"
    const val isFromFacebook: String = "is_from_facebook"

    const val profileImageWidth = 175
    const val profileImageHeight = 135

    const val confirmAccountDeletionMessage: String = "Are you certain you want to delete your account?"
    const val confirmChatroomDeletionMessage: String = "Are you ABSOLUTELY sure you want to delete this chatroom? All messages will be LOST."

    const val noUpdateFields: String = "No fields to update!"

    const val userProfileImageDir = "user_pfps"
    fun chatroomProfileImageDir(chatroomId: Long): String {
        return "chatroom_imgs_$chatroomId"
    }
    fun chatroomMessagesProfileImageDir(chatroomId: Long) = chatroomProfileImageDir(chatroomId) + "/messages"
    
    const val chatroomTopicPrefix = "chatroom_"
    fun chatroomTopic(chatroomId: Long): String {
        return chatroomTopicPrefix + chatroomId
    }

    fun buildDeleteAlertDialog(
        context: Context,
        dialogMessage: String,
        onConfirm: DialogInterface.OnClickListener, onCancel: DialogInterface.OnClickListener
    ) {
        val dialog = AlertDialog.Builder(context)
            .setMessage(dialogMessage)
            .setPositiveButton(context.getString(R.string.yes), onConfirm)
            .setNegativeButton(context.getString(R.string.no), onCancel)
            .create()

        dialog.window!!.setBackgroundDrawableResource(R.color.colorBackground)
        dialog.show()
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

    const val CHATROOM_DELETED: String = "CHATROOM_DELETED" // action for broadcast whenever owner deletes chatroom
    const val LOGOUT: String = "LOGOUT_ACTION" // action for user logout

    const val MAIN_ACTIVITY_FRAGMENT_SELECTED: String = "MAIN_ACTIVITY_FRAGMENT_SELECTED"

    // TODO: Move to DI and use it somehow...?!??
    // Process death go brrr :(((
    val tagJsonConverter: Gson = GsonBuilder()
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

    const val USER = "USER"

    class ImageFetchException(message: String? = null) : Exception(message)

    val imageRegex = Regex(
        Regex.escape(BuildConfig.BASE_URL) +
                "uploads\\/[^.]+\\.jpg"
    )

    const val LOCATIONS_COLLECTION: String = "locations"
    const val LOCATION: String = "location"
    const val KEY_LOCATION: String = "KEY_LOCATION"
    const val KEY_CAMERA_POSITION = "KEY_CAMERA_POSITION"

    const val EVENT_LOCATION = "EVENT_LOCATION"
    const val EVENT_TITLE: String = "EVENT_TITLE"
    const val EVENT_DESCRIPTION = "EVENT_DESCRIPTION"

    const val EVENT_IDS: String = "event_ids"
    const val CHATROOM_IDS: String = "chatroom_ids"
    const val LEAVE_CHATROOM_ID: String = "leave_chatroom_id"
}