package com.example.hobbyfi.persistence

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.example.hobbyfi.models.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao : BaseDao<User> {
    @Query("SELECT * FROM users")
    fun getUsers(): PagingSource<Int, User>

    @Query("UPDATE users SET chatroomId = :chatroomId WHERE id = :userId")
    suspend fun updateUserChatroomId(userId: Long, chatroomId: Int?)

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: Long): Flow<User?>

    @Query("DELETE FROM users WHERE id != :userId")
    fun deleteUsersExceptId(userId: Long): Int
}