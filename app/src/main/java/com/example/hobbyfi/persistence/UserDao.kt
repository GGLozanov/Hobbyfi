package com.example.hobbyfi.persistence

import androidx.room.Dao
import com.example.hobbyfi.models.User

@Dao
interface UserDao : BaseDao<User> {
}