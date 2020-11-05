package com.example.hobbyfi.models

import androidx.room.Entity

@Entity
data class Event(
    override val id: Int
) : Model(id)