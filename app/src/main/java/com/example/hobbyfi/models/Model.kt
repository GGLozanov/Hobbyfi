package com.example.hobbyfi.models

import androidx.room.Entity
import androidx.room.PrimaryKey

// TODO: This is not a very good abstraction...
@Entity
abstract class Model(
    @PrimaryKey
    open val id: Int
)