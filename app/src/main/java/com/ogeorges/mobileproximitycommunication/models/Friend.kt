package com.ogeorges.mobileproximitycommunication.models

import android.content.Context
import com.ogeorges.mobileproximitycommunication.localdatabase.entities.DBFriend
import java.io.File

class Friend(user: User, var status: String = "") : User(user.username, user.realname, user.avatarimg, user.publicKeyString){
    companion object{
        const val PENDING: String = "Pending.."
        const val ACCEPTED: String = "Accepted"
        const val DECLINED: String = "Declined"
        const val REQUEST: String = "Request"

        fun fromDBFriend(dbFriend: DBFriend, context: Context): Friend{
            val f = Friend(
                User(
                    dbFriend.username.toString(),
                    dbFriend.realName.toString(),
                    dbFriend.avatarImage,
                    dbFriend.publicKey
                ),
                dbFriend.status.toString()
            )
            f.stringid = dbFriend.lastEndpointID.toString()
            f.id = f.hashCode()

            val tempLocationPath = context.filesDir.path + "/TEMPAvatarOf${f.stringid}"
            val persistentLocationPath = context.filesDir.path + "/AvatarOf${f.id}.jpeg"
            if(File(persistentLocationPath).exists())
                f.avatarimg = persistentLocationPath
            else if (File(tempLocationPath).exists())
                f.avatarimg = tempLocationPath
            else
                f.avatarimg = ""

            return f
        }

        fun fromDBFriend(dbFriend: DBFriend): Friend{
            val f = Friend(
                User(
                    dbFriend.username.toString(),
                    dbFriend.realName.toString(),
                    dbFriend.avatarImage,
                    dbFriend.publicKey
                ),
                dbFriend.status.toString()
            )
            f.stringid = dbFriend.lastEndpointID.toString()
            f.id = f.hashCode()
            f.avatarimg = ""
            return f
        }
    }

    init {
        id = this.hashCode()
    }

    fun toDBFriend():DBFriend{
        return DBFriend(null, this.username, this.realname, this.avatarimg, this.publicKeyString, this.status, this.stringid)
    }

    fun update(friend: Friend){
        this.username = friend.username
        this.realname = friend.realname
        this.avatarimg = friend.avatarimg
        this.publicKeyString = friend.publicKeyString
        this.stringid = friend.stringid
        this.id = friend.id
        this.status = friend.status
    }

    fun update(user: User){
        this.username = user.username
        this.realname = user.realname
        this.avatarimg = user.avatarimg
        this.publicKeyString = user.publicKeyString
        this.stringid = user.stringid
        this.id = user.id
    }

    fun getLastMessage():String?{
        return null
    }
}