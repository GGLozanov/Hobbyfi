package com.example.hobbyfi.responses

import com.example.hobbyfi.models.User;

// list<users> response for chatroom will be map<string, user> atomic response from get_chatroom_users endpoint
class UserResponse(
    response: String?,
    model: User
) : CacheResponse<User>(response, model)