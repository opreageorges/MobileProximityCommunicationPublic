package com.ogeorges.mobileproximitycommunication.localdatabase

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ogeorges.mobileproximitycommunication.localdatabase.daos.FriendDAO
import com.ogeorges.mobileproximitycommunication.localdatabase.daos.MessageDAO
import com.ogeorges.mobileproximitycommunication.localdatabase.entities.DBFriend
import com.ogeorges.mobileproximitycommunication.localdatabase.entities.DBMessage

@Database(entities = [DBFriend::class, DBMessage::class], version = 5)
abstract class LocalDBImplementation: RoomDatabase() {
    abstract fun friendDAO(): FriendDAO
    abstract fun messageDAO(): MessageDAO
}