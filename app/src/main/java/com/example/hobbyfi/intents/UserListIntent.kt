package com.example.hobbyfi.intents

import com.example.hobbyfi.models.User

sealed class UserListIntent : Intent {
    object FetchUsers : UserListIntent()

    data class AddAUserCache(val user: User) : UserListIntent()
    data class UpdateAUserCache(val userUpdateFields: Map<String?, String?>) : UserListIntent()
    data class DeleteAUserCache(val userId: Long) : UserListIntent()
}
