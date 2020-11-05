package com.example.hobbyfi.models

import androidx.room.Entity

@Entity
data class Message(
    override val id: Int,
    val message: String,
    val createTime: String // iso string?
) : Model(id) {
}