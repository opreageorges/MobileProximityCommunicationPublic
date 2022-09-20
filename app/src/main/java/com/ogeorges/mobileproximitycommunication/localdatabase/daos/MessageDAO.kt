package com.ogeorges.mobileproximitycommunication.localdatabase.daos

import androidx.room.*
import com.ogeorges.mobileproximitycommunication.localdatabase.entities.DBMessage


@Dao
interface MessageDAO {
    @Query("SELECT * FROM messages")
    suspend fun getAll(): List<DBMessage>

    @Query("SELECT * FROM messages WHERE sender_pk == :userPK")
    suspend fun loadAllByUserPK(userPK: String): List<DBMessage>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(vararg users: DBMessage)

    @Delete
    suspend fun delete(message: DBMessage)
}