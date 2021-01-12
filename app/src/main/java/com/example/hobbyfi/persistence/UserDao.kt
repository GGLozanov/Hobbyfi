package com.example.hobbyfi.persistence

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.example.hobbyfi.models.User
import kotlinx.coroutines.flow.Flow

@Dao
abstract class UserDao : BaseDao<User>() {
    @Query("SELECT * FROM users")
    abstract fun getUsers(): Flow<List<User>?>

    @Query("SELECT * FROM users where id != :userId")
    abstract fun getUsersImmediateExceptId(userId: Long): List<User>?

    @Query("SELECT * FROM users WHERE id = :userId")
    abstract fun getUserById(userId: Long): Flow<User?>

    @Query("DELETE FROM users WHERE id != :userId")
    abstract fun deleteUsersExceptId(userId: Long): Int

    @Query("DELETE FROM users WHERE id = :userId")
    abstract fun deleteUserById(userId: Long): Int

}