package com.example.hobbyfi.persistence

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.example.hobbyfi.models.User

@Dao
interface UserDao : BaseDao<User> {
    @Query("SELECT * from users")
    fun getUsers() : PagingSource<Int, User>

    @Query("UPDATE users SET chatroomId = :chatroomId WHERE id = :userId")
    fun updateUserChatroomId(userId: Int, chatroomId: Int)
}