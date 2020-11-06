package com.example.hobbyfi.models

import androidx.room.Entity
import androidx.room.PrimaryKey

// TODO: This is not a very good abstraction...
@Entity
abstract class Model(
    // TODO: Add id here without overriding it in child and making Room confused
    // TODO: If impossible, just turn this into an interface so that it can act as a generic constraint
)