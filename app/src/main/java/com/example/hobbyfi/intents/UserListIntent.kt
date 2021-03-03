package com.example.hobbyfi.intents

import com.example.hobbyfi.models.data.User

sealed class UserListIntent : Intent {
    object FetchUsers : UserListIntent()

    data class KickUser(val userId: Long) : UserListIntent()

    data class AddAUserCache(val user: User) : UserListIntent()
    data class UpdateAUserCache(val userUpdateFields: Map<String, String?>) : UserListIntent()
    data class DeleteAUserCache(val userId: Long) : UserListIntent()
}
