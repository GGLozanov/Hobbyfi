package com.example.hobbyfi.persistence

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.example.hobbyfi.models.User

@Dao
interface UserDao : BaseDao<User> {
    @Query("SELECT * from users")
    fun getUsers() : PagingSource<Int, User>
}