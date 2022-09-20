package com.ogeorges.mobileproximitycommunication.models

import com.google.gson.Gson
import com.ogeorges.mobileproximitycommunication.localdatabase.LocalDB
import com.ogeorges.mobileproximitycommunication.localdatabase.entities.DBMessage

data class ChatMessage(val body: String?, var sender: String, var sender_type:String, val sentTime: Long){
    companion object{
        const val SENDER_TYPE_YOU = "you"
        const val SENDER_TYPE_ME = "me"

        fun fromJson(json: String): ChatMessage {
            return Gson().fromJson(json, ChatMessage::class.java)
        }
    }
    constructor(chatMessage: ChatMessage): this(chatMessage.body, chatMessage.sender, chatMessage.sender_type, chatMessage.sentTime)
    constructor(dbMessage: DBMessage): this(dbMessage.body, dbMessage.senderId, dbMessage.senderType, dbMessage.timestamp){
        val fr = LocalDB.readFriendList()!!.find{it.publicKey == dbMessage.senderId}
        if (fr == null){
            this.sender = "???"
        }
        else {
            this.sender = fr.realName.toString()
        }
    }
    fun toJson():String{
        return Gson().toJson(this)
    }
}