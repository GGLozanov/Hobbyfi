package com.example.hobbyfi.shared

import android.util.Log
import android.util.Patterns
import androidx.core.util.Predicate
import androidx.paging.PagingConfig
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.repositories.Repository
import com.example.hobbyfi.ui.auth.RegisterFragmentDirections
import com.facebook.AccessToken
import com.facebook.Profile

object Constants {
    const val descriptionInputError: String = "Enter a shorter description!"
    const val usernameInputError: String = "Enter a non-empty or shorter username!"
    const val nameInputError: String = "Enter a non-empty or shorter name!"
    const val passwordInputError: String = "Enter a non-empty or shorter/longer password!"
    const val confirmPasswordInputError: String = "Enter the same password!"
    const val emailInputError: String = "Enter a non-empty valid e-mail address!"
    const val tagNameInputError: String = "Enter a non-empty or shorter tag name!"

    const val reauthError: String = "Logging out! Your session may have expired!"
    const val resourceExistsError: String = "This user/thing already exists!"
    const val noConnectionError: String = "Couldn't perform operation! Please check your connection!"
    const val invalidCredentialsError: String = "Invalid credentials!"
    const val invalidTokenError: String = "Invalid access! Please login again!"
    const val unauthorisedAccessError: String = "Unauthorised access!"
    const val expiredTokenError: String = "Your session may have expired and you need to (re)authenticate!"
    const val missingDataError: String = "Missing/invalid data entered!"
    const val cacheDeletionError: String = "Couldn't clear old (cached) chatrooms!"
    const val serverConnectionError: String = "Failed to connect to server! Something might have gone wrong on our end!"
    const val internalServerError: String = "Couldn't perform operation! Something might have gone wrong on our end!"
    const val chatroomJoined: String = "chatroomJoined"
    const val chatroomLeft: String = "chatroomLeft"
    fun unknownError(message: String?) = "Unknown error! Please check your connection or contact a developer! ${message}"

    const val imagePermissionsRequestCode = 200
    const val imageRequestCode = 777

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

    val passwordPredicate = Predicate<String> {
        return@Predicate it.isEmpty() || it.length <= 4 || it.length >= 15
    }

    fun confirmPasswordPredicate(originalPassword: String?) = Predicate<String> {
        return@Predicate it.isEmpty() || it != originalPassword
    }

    val namePredicate = Predicate<String> {
        return@Predicate it.isEmpty() || it.length >= 25
    }

    val descriptionPredicate = Predicate<String> {
        return@Predicate it.length >= 30
    }

    fun getDefaultPageConfig(): PagingConfig { // used in pager init
        return PagingConfig(pageSize = 5, enablePlaceholders = false)
    }

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

    const val PHOTO_URL = "photo_url"

    const val AUTH_HEADER = "Authorization"

    const val tagsKey = "tag"
    const val selectedTagsKey = "selectedTags"

    const val name = "name"
    const val colour = "colour"
    const val isFromFacebook: String = "is_from_facebook"

    const val profileImageWidth = 175
    const val profileImageHeight = 135

    const val userProfileImageDir = "user_pfps"
}