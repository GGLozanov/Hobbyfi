package com.example.hobbyfi.models

import androidx.room.Entity
import androidx.room.PrimaryKey

// TODO: Room embed fields & typeconverters for saving tag lists in user entity
@Entity
data class Chatroom(
    @PrimaryKey
    val id: Int
) : Model()