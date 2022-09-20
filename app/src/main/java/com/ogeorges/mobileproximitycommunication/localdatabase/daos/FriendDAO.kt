package com.ogeorges.mobileproximitycommunication.localdatabase.daos

import androidx.room.*
import com.ogeorges.mobileproximitycommunication.localdatabase.entities.DBFriend


@Dao
interface FriendDAO {
    @Query("SELECT * FROM friends")
    suspend fun getAll(): List<DBFriend>

//    @Query("SELECT * FROM user WHERE uid IN (:userIds)")
//    fun loadAllByIds(userIds: IntArray): List<DBUser>

//    @Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
//            "last_name LIKE :last LIMIT 1")
//    fun findByName(first: String, last: String): User

    @Update
    suspend fun update(vararg friends: DBFriend)

    @Insert
    suspend fun insertAll(vararg friends: DBFriend)

    @Query("DELETE FROM friends WHERE id > 0")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(vararg user: DBFriend)

}