package com.example.hobbyfi.shared

import androidx.paging.PagingConfig
import com.example.hobbyfi.models.Tag

object Constants {
    val descriptionInputError: String = "Enter a shorter description!"
    val usernameInputError: String = "Enter a non-empty valid username!"
    val passwordInputError: String = "Enter a non-empty or shorter password!"
    val emailInputError: String = "Enter a non-empty valid e-mail address!"

    // TODO: Put in-memory tags here
    val predefinedTags: List<Tag> = listOf()

    fun getDefaultPageConfig(): PagingConfig { // used in pager init
        return PagingConfig(pageSize = 5, enablePlaceholders = false)
    }
}