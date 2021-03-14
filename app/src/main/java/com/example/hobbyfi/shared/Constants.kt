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
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder


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
    const val invalidDataError: String = "Invalid data format!"
    const val noConnectionError: String = "Couldn't perform operation! Please check your connection!"
    const val noConnectionOrAuthTaskRootError: String = "Couldn't enter the chatroom! Please check your connection or login again!"
    const val invalidCredentialsError: String = "Invalid credentials!"
    const val invalidBroadcastAction: String = "Invalid action given for registered BroadcastReceiver types!"
    const val invalidTokenError: String = "Invalid access! Please login again!"
    const val unauthorisedAccessError: String = "Unauthorised access! Please login again!"
    const val expiredTokenError: String = "Your session may have expired and you need to (re)authenticate!"
    const val missingDataError: String = "Missing/invalid data entered!"
    const val cacheDeletionError: String = "Couldn't clear old (cached) data!"
    const val serverConnectionError: String = "Failed to connect to server! Something might have gone wrong on our end!"
    const val internalServerError: String = "Couldn't perform operation! Something might have gone wrong on our end!"
    const val resourceNotFoundError: String = "Requested resource not found!"
    const val invalidViewTypeError: String = "Invalid view type for ViewHolder!"
    const val invalidStateError: String = "Invalid state to call this method in!"
    const val firestoreDeletionError: String = "Couldn't delete remote source records needed to have been deleted!"
    const val firestoreUpdateError: String = "Couldn't update remote source records that needed to have been updated!"
    const val limitReachedError: String = "Not created! Maximum limit may be reached! (250)"
    const val requiredPermissionsDeniedError: String = "Cannot partake in this without required permissions!"
    const val incorrectCallToBuildLocationTrackingDialog: String = "Cannot call method buildLocationTrackingDialog in EventMapsActivity without having garnered user location permissions first!"
    const val showShareDialogFail: String = "Couldn't reroute to Facebook share screen! Please try again!"
    const val shareDeepLinkCancel: String = "Event share attempt cancelled!"
    const val shareDeepLinkSuccess: String = "Successfuly shared event to Facebook!"
    const val shareDeepLinkFail: String = "Couldn't share event to Facebook! Something must have gone wrong on their end!"
    const val deepLinkGenFail: String = "Couldn't generate an appropriate share format for this event! Please contact a developer!"
    const val eventDeleted: String = "Event you were tracking was suddenly deleted!"
    const val invalidAccessError: String = "In order to access this content, you'll need to log in, or sign up and join the chatroom first."
    const val eventAlreadyDeleted: String = "Linked event seems to already have been deleted by the chatroom owner!"
    const val notJoinedChatroomError: String = "Can't participate in an event whose chatroom you haven't joined!"
    const val tapToViewImage: String = "Tap to view sent image"
    const val emailNotFound: String = "User with this given e-mail wasn't found!"
    const val emailSendFail: String= "Failed to send e-mail! Please, try again!"
    const val facebookUserSendAttempt: String = "User with this e-mail is a Facebook user and doesn't need an e-mail sent!"
    const val searchMessageNotFound: String = "Message you were searching for couldn't be found!"
    const val userKickFail: String = "Couldn't manage to kick user!"
    const val userKickSuccess: String = "User successfully kicked!"
    const val chatroomDeletedMessage: String = "Oh no, it looks like the chatroom was deleted by the owner! We apologise for the inconvenience this may have caused!"
    const val chatroomKickedMessage: String = "Oh no, it looks like you've been kicked from the chatroom by the owner!"
    const val eventParsingError: String = "Failed to parse event date!"
    const val socketConnectionError: String = "Couldn't connect to server for realtime capabilities!"
    const val socketEmissionError: String = "Couldn't receive new messages to display!"
    fun unknownError(message: String?) = "Unknown error! Please check your connection or contact a developer! $message"

    const val canOnlyResetOnNoUpdate: String = "Can only reset location if you aren't currently partaking in the event with your location!"
    const val eventAlreadyConcluded: String = "Event has already concluded!"

    const val imagePermissionsRequestCode = 200
    const val imageRequestCode = 777

    const val locationRequestCode: Int = 220
    const val locationPermissionsRequestCode: Int = 888

    const val eventLocationRequestCode: Int = 999
    const val eventMapsRequestCode: Int = 666
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
    const val FACEBOOK_EMAIL_FAILED_EXCEPTION = "Couldn't fetch Facebook email!"
    const val FACEBOOK_TAGS_FAILED_EXCEPTION = "Error with fetching your Facebook tags! Continuing without them!"
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

    const val takeMeThere: String = "Get me in the room!"
    const val noPlease: String = "Nah, let me browse."

    const val locationReset: String = "Location successfuly reset!"

    const val userProfileImageDir = "user_pfps"
    fun chatroomProfileImageDir(chatroomId: Long): String {
        return "chatroom_imgs_$chatroomId"
    }
    fun chatroomMessagesProfileImageDir(chatroomId: Long) = chatroomProfileImageDir(chatroomId) + "/messages"
    fun eventProfileImageDir(eventId: Long): String {
        return "events_imgs_$eventId"
    }

    const val chatroomTopicPrefix = "chatroom_"
    fun chatroomTopic(chatroomId: Long): String {
        return chatroomTopicPrefix + chatroomId
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

    // TODO: Move to DI and use it somehow...?!??
    // Process death go brrr
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

    const val TOKEN: String = "token"
    const val API: String = "API"

    const val USER = "USER"

    class ImageFetchException(message: String? = null) : Exception(message)

    val imageRegex = Regex(
        Regex.escape(BuildConfig.BASE_URL) +
                "uploads\\/[^.]+\\.jpg"
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
    const val LEAVE_CHATROOM_ID: String = "leave_chatroom_id"

    const val LAST_CONNECTIVITY: String = "LAST_CONNECTIVITY"

    const val EVENT_SELECTION: String = "EVENT_SELECTION"
    const val EVENT: String = "EVENT"
    const val CALENDAR_DAY: String = "CALENDAR_DAY"

    const val UPDATED_LOCATION: String = "UPDATED_LOCATION"
    const val STARTED_UPDATE_LOCATION_FROM_NOTIFICATION: String = "STARTED_UPDATE_LOCATION_FROM_NOTIFICATION"
    const val UPDATED_LOCATION_ACTION: String = "com.example.hobbyfi.UPDATED_LOCATION_ACTION"
    const val FOREGROUND_REACTIVIATION_ACTION: String = "com.example.hobbyfi.FOREGROUND_REACTIVIATION_ACTION";
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