package com.ogeorges.mobileproximitycommunication.localdatabase.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "friends", indices = [Index(value = ["public_key"], unique = true)])
data class DBFriend(@PrimaryKey(autoGenerate = true) val id: Int?,
                    @ColumnInfo(name = "username") val username: String?,
                    @ColumnInfo(name = "real_name") val realName: String?,
                    @ColumnInfo(name = "avatar_image") val avatarImage: String?,
                    @ColumnInfo(name = "public_key") val publicKey: String,
                    @ColumnInfo(name = "status") var status: String?,
                    @ColumnInfo(name = "last_endpoint_id") var lastEndpointID: String?
                  ){

    override fun hashCode(): Int {
        var result = realName.hashCode()
        result = 31 * result + publicKey.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DBFriend

        if (id != other.id) return false
        if (realName != other.realName) return false
        if (publicKey != other.publicKey) return false

        return true
    }
}