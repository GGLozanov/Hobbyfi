package com.example.hobbyfi.models

import androidx.room.Entity

// in-memory db for predefined tags
// users can create their own tags (name + colour)
// once list of tag names is sent to backend for user/chatroom creation
// backend performs SELECT query to see if tag of same name already exists
// if not, insert and make connections with chatroom/user
// if yes, don't insert and make connections
// once user goes to see other chatrooms/users, they'll see their custom tags from the API fetch from the backend
@Entity
data class Tag(
  override val id: Int
) : Model(id)