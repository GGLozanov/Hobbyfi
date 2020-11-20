package com.example.hobbyfi.responses

import com.example.hobbyfi.models.User;

// list<users> response for chatroom will be map<string, user> atomic response from get_chatroom_users endpoint
data class UserResponse(override val response: String?) : CacheResponse<User>(response) {

}