package com.example.hobbyfi.models

import androidx.room.Entity

// TODO: Room embed fields & typeconverters for saving tag lists in user entity
@Entity
data class Chatroom(
    override val id: Int
) : Model(id)