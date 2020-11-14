package com.example.hobbyfi.shared

import androidx.paging.PagingConfig
import com.example.hobbyfi.models.Tag

object Constants {
    val descriptionInputError: String = "Enter a shorter description!"
    val usernameInputError: String = "Enter a non-empty valid username!"
    val passwordInputError: String = "Enter a non-empty or shorter password!"
    val emailInputError: String = "Enter a non-empty valid e-mail address!"
    val tagNameInputError: String = "Enter a non-empty or shorter tag name!"

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

    fun getDefaultPageConfig(): PagingConfig { // used in pager init
        return PagingConfig(pageSize = 5, enablePlaceholders = false)
    }

    const val CACHE_TIMEOUT = 60 * 30 // 30 minutes
        .toLong()

    const val SUCCESS_RESPONSE = "ok"
    const val FAILED_RESPONSE = "failed"
    const val IMAGE_UPLOAD_SUCCESS_RESPONSE = "Image Uploaded"
    const val IMAGE_UPLOAD_FAILED_RESPONSE = "Image Upload Failed"
    const val FACEBOOK_EMAIL_FAILED_EXCEPTION = "Error with fetching your Facebook email! Stopping login!"
    const val FACEBOOK_TAGS_FAILED_EXCEPTION = "Error with fetching your Facebook tags! Continuing without them!"
    const val EXISTS_RESPONSE = "exists"
    const val REAUTH_FLAG = "Reauth"
    const val FAILED_FLAG = "Failed: "

    // enum would've been more concise here but annotations can't have enums call toString()
    const val ID = "id"
    const val RESPONSE = "response"
    const val EMAIL = "email"
    const val PASSWORD = "password"
    const val USERNAME = "username"
    const val DESCRIPTION = "description"
    const val IMAGE = "image"
    const val TAGS = "tags"

    const val PHOTO_URL = "photo_url"

    const val AUTH_HEADER = "Authorization"
}